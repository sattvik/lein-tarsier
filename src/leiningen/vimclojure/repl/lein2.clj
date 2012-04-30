(ns leiningen.vimclojure.repl.lein2
  "Runs the REPL in a Leiningen 2 project.  The code here is largely derivative
  of the code from `leiningen.repl`."
  {:author "Daniel Solano Gómez"}
  (:require [clojure.tools.nrepl.ack :as nrepl.ack]
            [leiningen.repl :as repl]
            [leiningen.trampoline :as trampoline]
            [leiningen.core.eval :as eval]
            [leiningen.core.project :as project]
            [leinjacker.utils :as utils]
            [reply.main :as reply])
  (:use [trammel.core :only [defconstrainedfn]]))

(defconstrainedfn ^{:private true} start-server
  "This a modified version of Lein 2’s `leiningen.repl/start-server` to
  accomodate the VimClojure server.  One particularly evil thing that I do here
  is redefine `reply.exit/exit` so that it shuts down the VimClojure server.
  Otherwise the process will just hang to my great annoyance."
  [project port ack-port vimclojure-host vimclojure-port with-server]
  [(or (map? project) (nil? project))
   (integer? port)
   (integer? ack-port)
   (string? vimclojure-host)
   (integer? vimclojure-port)]
  (let [server-starting-form
        (with-server
          `(fn [vimclojure#]
             (require 'reply.exit)
             ; This is evil.  But otherwise the process will just hang.
             (alter-var-root #'reply.exit/exit
                             (fn [f#]
                               (fn []
                                 (try
                                   (.shutdown vimclojure# false)
                                   (catch Throwable _#))
                                 (f#))))
             (let [server# (clojure.tools.nrepl.server/start-server
                             :port ~port :ack-port ~ack-port)]
               (println (str "Starting VimClojure server on" ~vimclojure-host
                             ", " ~vimclojure-port))
               (.start (Thread. vimclojure#))
               (println "nREPL server started on port"
                        (-> server# deref :ss .getLocalPort))
               (while true (Thread/sleep Long/MAX_VALUE)))))]
    (if project
      (eval/eval-in-project
        (project/merge-profile project repl/profile)
        server-starting-form
        '(do (require 'clojure.tools.nrepl.server)
             (require 'complete.core)))
      (eval server-starting-form))))

(defconstrainedfn run
  "Runs the REPL.  This is derived from Leiningen 2’s `leiningen.repl/repl`
  with some modifications to accomodate the VimClojure server."
  [project vimclojure-host vimclojure-port with-server]
  [(or (map? project) (nil? project))
   (string? vimclojure-host)
   (integer? vimclojure-port)]
  (if trampoline/*trampoline?*
    (println "lein-tarsier does not yet support trampoline with Lein 2 (wait for preview 4)"))
  (let [mode (if (utils/try-resolve 'leiningen.repl/trampoline-profile)
               :master
               :preview3)]
    (when (= mode :master)
      (println "You are running a bleeding edge version of Leiningen,"
               "all warranties are null and void."))
    (nrepl.ack/reset-ack-port!)
    (let [prepped (promise)
          repl-port @(resolve 'leiningen.repl/repl-port)]
      (.start
        (Thread.
          (bound-fn []
            (start-server (and project (vary-meta project assoc
                                                  :prepped prepped))
                          (repl-port project)
                          (-> @repl/lein-repl-server deref :ss .getLocalPort)
                          vimclojure-host
                          vimclojure-port
                          with-server))))
      (and project @prepped)
      (if-let [repl-port (nrepl.ack/wait-for-ack (or (-> project
                                                         :repl-options
                                                         :timeout)
                                                   30000))]
        (reply/launch-nrepl (if (= mode :preview3)
                              (repl/options-for-reply repl-port project)
                              (repl/options-for-reply project :attach repl-port)))
        (println "REPL server launch timed out.")))))
