(ns leiningen.vimclojure.repl.lein1
  "Runs the REPL in a Leiningen 1.x project."
  {:author "Daniel Solano Gómez"}
  (:require [leiningen [compile :as eval]
                       [core :as core]
                       [deps :as deps]
                       [trampoline :as trampoline]
                       [repl :as repl]])
  (:use [leinjacker.defconstrainedfn :only [defconstrainedfn]]))

(defconstrainedfn run
  "Runs the REPL.  This is derived from Leiningen 1.x’s `leiningen.repl/repl`
  with some modifications to accomodate the VimClojure server."
  [project vimclojure-host vimclojure-port with-server]
  [(or (map? project) (nil? project))
   (string? vimclojure-host)
   (integer? vimclojure-port)]
  (when (and project (or (empty? (deps/find-deps-files project))
                         (:checksum-deps project)))
    (deps/deps project))
  (let [[port host] (repl/repl-socket-on project)
        server-form (binding [core/*interactive?* true]
                      (apply repl/repl-server project host port
                             (concat (:repl-options project)
                                     (:repl-options (core/user-settings)))))
        server-form (with-server
                      `(fn [vimclojure#]
                         (println (str "Starting VimClojure server on "
                                       ~vimclojure-host ", "
                                       ~vimclojure-port))
                         (.. (Thread. vimclojure#) start)
                         ~server-form))
        retries (- repl/retry-limit
                   (or (:repl-retry-limit project)
                       ((core/user-settings) :repl-retry-limit)
                       repl/retry-limit))]
    (if trampoline/*trampoline?*
      (eval/eval-in-project project server-form)
      (do (future (if (empty? project)
                    (clojure.main/with-bindings (println (eval server-form)))
                    (eval/eval-in-project project server-form)))
        (repl/poll-repl-connection port retries repl/repl-client)
        (core/exit)))))
