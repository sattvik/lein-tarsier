(ns leiningen.vimclojure.repl
  "Provides the interface to the REPL for lein-tarsier."
  {:author "Daniel Solano Gómez"}
  (:require [leinjacker.utils :as utils])
  (:use [trammel.core :only [defconstrainedfn]]))

(defconstrainedfn run
  "Finds the appropriate REPL runner depending on the current Leiningen
  version, and then invokes the REPL appropriately."
  [project host port with-server]
  [(map? project) (string? host) (integer? port)]
  (let [run (or (when (utils/try-resolve 'leiningen.core.eval/eval-in-project)
                  (utils/try-resolve 'leiningen.vimclojure.repl.lein2/run))
                (utils/try-resolve 'leiningen.vimclojure.repl.lein1/run))]
    (@run project host port with-server)))
