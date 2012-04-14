(ns leiningen.vimclojure
  "Adds VimClojure server support to a Leiningen project."
  (:use [trammel.core :only [defconstrainedfn]])
  (:import java.net.InetAddress))

(def ^{:private true
       :doc "The name  of the VimClojure dependency."}
  vimclojure-package
  'vimclojure/server)

(def ^{:private true
       :doc "The version of VimClojure to include."}
  vimclojure-version
  "2.3.1")

(def ^{:private true
       :doc "The VimClojure dependency."}
  vimclojure-dep
  [vimclojure-package vimclojure-version :exclusions ['org.clojure/core]])

(def ^{:private true
       :doc "Default values for the plug-in options."}
  default-opts
  {:host (InetAddress/getByName "127.0.0.1")
   :port 2113
   :repl false})

(defconstrainedfn try-resolve
  "Attempts to resolve the given namespace-qualified symbol.  If successful,
  returns the resolved symbol.  Otherwise, returns nil."
  [sym]
  [symbol? namespace]
  (let [ns-sym (symbol (namespace sym))]
    (try (require ns-sym)
      (resolve sym)
      (catch java.io.FileNotFoundException _))))

(defn- eval-in-project
  "Support eval-in-project for both Leiningen 1.x and 2.x.  This code is
  inspired from the code in the lein-swank plug-in."
  [project form]
  (let [eip (or (try-resolve 'leiningen.core.eval/eval-in-project)
                (try-resolve 'leiningen.compile/eval-in-project))]
    (eip project form)))

(defconstrainedfn read-keys
  "Take a key-value pair and reads the key ensuring the result is a keyword."
  [[k v]]
  [(string? k) (string? v) => (fn [k2 v2] (and (keyword? k2) (= v v2)))]
  [(read-string k) v])

(defconstrainedfn args->options
  "Process the command line arguments for the vimclojure plug-in, converting
  them to a map of keywords to string values."
  [args]
  [(even? (count args))]
  (->> args
       (partition 2)
       (mapcat read-keys)
       (apply hash-map)))

(defmulti to-inetaddress
  "Converts the argument into an InetAddress instance."
  {:post [#(instance? InetAddress %)]}
  type)

(defmethod to-inetaddress InetAddress [addr] addr)
(defmethod to-inetaddress String [s] (InetAddress/getByName s))
(defmethod to-inetaddress :default [x] (to-inetaddress (str x)))

(defconstrainedfn to-boolean
  "Converts the argument into a boolean value."
  [x]
  [=> #(instance? Boolean %)]
  (boolean (Boolean/valueOf (str x))))

(defconstrainedfn to-int
  "Converts the argument to an integer."
  [n]
  [=> integer? pos?]
  (if (number? n)
    (int n)
    (Integer/parseInt (str n))))

(defn get-options
  "Gets the options from the defaults, the project, and the command line
  arguments."
  [project args]
  (merge default-opts
         (:vimclojure-opts project)
         (args->options args)))

(defn validate-options
  "Validates the options to make sure they contain valid values."
  [options]
  (-> options
      (update-in [:host] to-inetaddress)
      (update-in [:port] to-int)
      (update-in [:repl] to-boolean)))

(defconstrainedfn has-vimclojure-dep?
  "Returns a value that evaluates to true if the project has a
  vimclojure/server dependency."
  [project]
  [(vector? (:dependencies project))]
  (some #(= vimclojure-package (first %)) (:dependencies project)))

(defconstrainedfn add-vimclojure-dep
  "Adds the VimClojure server as a dependency of the project."
  [project]
  [=> has-vimclojure-dep?]
  (if (has-vimclojure-dep? project)
    project
    (update-in project [:dependencies] conj vimclojure-dep)))

(defconstrainedfn get-vimclojure-opt
  "Gets the value of the given plug-in option from the project."
  [project opt]
  [(:vimclojure-opts project)]
  (get-in project [:vimclojure-opts opt]))

(defconstrainedfn run-server-form
  "Generates a form that will run the server and cleanly it shut down when the
  user interrupts the process."
  [host port]
  `(let [server#      (~'vimclojure.nailgun.NGServer. 
                          (java.net.InetAddress/getByName ~host)
                          (Integer/parseInt ~port))
         shutdowner#  (Thread. (reify Runnable
                                 (run [_]
                                   (.shutdown server# false))))]
     (.. (Runtime/getRuntime) (addShutdownHook shutdowner#))
     (println (str "Starting VimClojure server on " ~host ", port " ~port))
     (println "Happy hacking!")
     (.run server#)))

(defconstrainedfn run-repl-form
  "Generates a form that will run the server in the background and provide a
  REPL in the foreground.  The server will shut down when the REPL terminates."
  [host port]
  `(let [server#      (~'vimclojure.nailgun.NGServer. 
                          (java.net.InetAddress/getByName ~host)
                          (Integer/parseInt ~port))
         thread#      (Thread. server#)]
     (println (str "Starting VimClojure server on " ~host ", port " ~port))
     (.start thread#)
     (println "Clojure" (clojure-version))
     (clojure.main/repl)
     (.shutdown server# false)))


(defconstrainedfn vimclojure-form
  "Generates a form to be evaluated in the project's context."
  [project]
  [has-vimclojure-dep?
   (instance? InetAddress (get-vimclojure-opt project :host))
   (integer? (get-vimclojure-opt project :port))
   ]
   ;=>
   ;(instance? clojure.lang.Cons %)]
  (let [host (.getHostAddress (get-vimclojure-opt project :host))
        port (str (get-vimclojure-opt project :port))]
    (if (get-vimclojure-opt project :repl)
      (run-repl-form host port)
      (run-server-form host port))))

(defn vimclojure
  "Launches a VimClojure server.

  This plug-in understands the following options:

  :host  the host name to use for opening the server (default: 127.0.0.1)

  :port  the port on which the server will listen (default: 2113)

  :repl  if true, in addition to starting the server, starts a REPL (default: false)
   
  These options may be supplied in your project.clj as the value of
  ':vimclojure-opts' or on the command lein, as in:

    lein vimclojure :repl true
  
  Command line options override project options."
  [project & args]
  (let [options (-> (get-options project args)
                    (validate-options))
        project (-> project
                    add-vimclojure-dep
                    (update-in [:vimclojure-opts] merge options))]
    (eval-in-project project (vimclojure-form project))))
