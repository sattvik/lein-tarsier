(ns leiningen.vimclojure.repl
  "Provides the interface to the REPL for lein-tarsier."
  {:author "Daniel Solano Gómez"}
  (:require [leinjacker.utils :as utils])
  (:use [leinjacker.defconstrainedfn :only [defconstrainedfn]]))

(def ^{:private true
       :doc "Map of Leiningen generation to REPL run functions."}
  repl-runners {1 #(utils/try-resolve 'leiningen.vimclojure.repl.lein1/run)
                2 #(utils/try-resolve 'leiningen.vimclojure.repl.lein2/run)})

(defconstrainedfn run
  "Finds the appropriate REPL runner depending on the current Leiningen
  version, and then invokes the REPL appropriately."
  [project host port with-server]
  [(map? project) (string? host) (integer? port)]
  (let [run (repl-runners (utils/lein-generation))]
    (@(run) project host port with-server)))
