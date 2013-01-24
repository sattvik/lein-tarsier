(ns leiningen.vimclojure.repl.lein2
  "Runs the REPL in a Leiningen 2 project.  The code here is largely derivative
  of the code from `leiningen.repl`."
  {:author "Daniel Solano Gómez"}
  (:require [clojure.java.io :as io]
            [clojure.tools.nrepl.ack :as nrepl.ack]
            [leiningen.repl :as repl]
            [leiningen.trampoline :as trampoline]
            [leiningen.core.eval :as eval]
            [leiningen.core.project :as project]
            [leiningen.core.user :as user]
            [leiningen.vimclojure.deps :as deps]
            [leinjacker.utils :as utils]
            [reply.main :as reply]
            reply.exit)
  (:use [trammel.core :only [defconstrainedfn]]))

(def ^{:private true
       :doc     "A profile that contains the VimClojure server dependency."}
  vimclojure-profile {:dependencies [deps/vimclojure-spec]})

(defconstrainedfn ^{:private true} start-server
  "This a modified version of Lein 2’s `leiningen.repl/start-server` to
  accomodate the VimClojure server.  One particularly evil thing that I do here
  is redefine `reply.exit/exit` so that it shuts down the VimClojure server.
  Otherwise the process will just hang to my great annoyance.  This seems to
  only happen to plug-in projects."
  [project host port ack-port vimclojure-host vimclojure-port with-server]
  [(or (map? project) (nil? project))
   (string? host)
   (integer? port)
   (integer? ack-port)
   (string? vimclojure-host)
   (integer? vimclojure-port)]
  (let [handler-for   @(resolve 'leiningen.repl/handler-for)
        init-requires @(resolve 'leiningen.repl/init-requires)
        server-starting-form
        (with-server
          `(fn [vimclojure#]
             (when-let [exit-var# (try (resolve 'reply.exit/exit)
                                    (catch Throwable _#))]
               (alter-var-root exit-var#
                               (fn [f#]
                                 (fn []
                                   (try
                                     (.shutdown vimclojure# false)
                                     (catch Throwable _#))
                                   (f#)))))
             (let [server# (clojure.tools.nrepl.server/start-server
                             :bind ~host :port ~port :ack-port ~ack-port
                             :handler ~(handler-for project))
                   port#   (-> server# deref :ss .getLocalPort)]
               (println (str "Starting VimClojure server on "
                             ~vimclojure-host ", " ~vimclojure-port))
               (.start (Thread. vimclojure#))
               (println "nREPL server started on port" port#)
               (spit ~(str (io/file (:target-path project) "repl-port")) port#)
               (.deleteOnExit (io/file ~(:target-path project) "repl-port"))
               @(promise))))]
    (if project
      (eval/eval-in-project
        (project/merge-profiles project (conj (repl/profiles-for project false true)
                                              vimclojure-profile))
        server-starting-form
        `(require ~@(init-requires project)))
      (eval server-starting-form))))

(defconstrainedfn run
  "Runs the REPL.  This is derived from Leiningen 2’s `leiningen.repl/repl`
  with some modifications to accomodate the VimClojure server."
  [project vimclojure-host vimclojure-port with-server]
  [(or (map? project) (nil? project))
   (string? vimclojure-host)
   (integer? vimclojure-port)]
  (let [repl-port     @(resolve 'leiningen.repl/repl-port)
        repl-host     @(resolve 'leiningen.repl/repl-host)
        init-requires @(resolve 'leiningen.repl/init-requires)]
    (if trampoline/*trampoline?*
      (let [options (repl/options-for-reply project :port (repl-port project))
            profiles (conj (repl/profiles-for project true true) vimclojure-profile)]
        (eval/eval-in-project
          (project/merge-profiles project profiles)
          (with-server
            `(fn [vimclojure#]
               (when-let [exit-var# (try (resolve 'reply.exit/exit)
                                      (catch Throwable _#))]
                 (alter-var-root exit-var#
                                 (fn [f#]
                                   (fn []
                                     (try
                                       (.shutdown vimclojure# false)
                                       (catch Throwable _#))
                                     (f#)))))
               (println (str "Starting VimClojure server on "
                             ~vimclojure-host ", "
                             ~vimclojure-port))
               (.. (Thread. vimclojure#) start)
               (reply.main/launch-nrepl ~options)))
          `(require ~@(init-requires project 'reply.main))))
      (let [prep-blocker @eval/prep-blocker]
        (nrepl.ack/reset-ack-port!)
        (.start
          (Thread.
            (bound-fn []
              (start-server project
                            (repl-host project)
                            (repl-port project)
                            (-> @repl/lein-repl-server deref :ss .getLocalPort)
                            vimclojure-host
                            vimclojure-port
                            with-server))))
        (when project @prep-blocker)
        (if-let [repl-port (nrepl.ack/wait-for-ack (-> project
                                                       :repl-options
                                                       (:timeout 30000)))]
          (reply/launch-nrepl (repl/options-for-reply project :attach repl-port))
          (println "REPL server launch timed out."))))))
