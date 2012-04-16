(ns leiningen.vimclojure.eval-in-project
  "Provides eval-in-project support for Leiningen 1.x and 2.x"
  {:author "Daniel Solano Gómez"}
  (:use [trammel.core :only [defconstrainedfn]]))

(defconstrainedfn try-resolve
  "Attempts to resolve the given namespace-qualified symbol.  If successful,
  returns the resolved symbol.  Otherwise, returns nil."
  [sym]
  [symbol? namespace]
  (let [ns-sym (symbol (namespace sym))]
    (try (require ns-sym)
      (resolve sym)
      (catch java.io.FileNotFoundException _))))

(defn eval-in-project
  "Support eval-in-project for both Leiningen 1.x and 2.x.  This code is
  inspired from the code in the lein-swank plug-in."
  [project form]
  (let [eip (or (try-resolve 'leiningen.core.eval/eval-in-project)
                (try-resolve 'leiningen.compile/eval-in-project))]
    (eip project form)))
