(ns leiningen.vimclojure.options.tarsier
  "Handles the :vimclojure-opts configuration."
  {:author "Daniel Solano Gómez"}
  (:import java.net.InetAddress)
  (:use [trammel.core :only [defconstrainedfn]]))

(def ^{:private true
       :doc "Default values for the plug-in options."}
  default-opts
  {:host "127.0.0.1"
   :port 2113
   :repl false})

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

(defmulti validate-host
  "Validates the address as being valid and converts it to a string."
  {:post [#(string? %)
          #(InetAddress/getByName %)]}
  type)

;; If the host is a valid InetAddress, just return the its name as a string
(defmethod validate-host InetAddress
  [^InetAddress host]
  (.getHostName host))

;; If the host is a string, try to parse it into an InetAddress.  If
;; successful, this will eventually be recursively transformed back into a
;; string.
(defmethod validate-host String
  [host]
  (validate-host (InetAddress/getByName host)))

;; For any other value, coerce it into a string and try again.
(defmethod validate-host :host
  [host]
  (validate-host (str host)))

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

(defn merge-options
  "Merges the options from the defaults, the project, and the command line
  arguments."
  [project args]
  (merge default-opts
         (:vimclojure-opts project)
         (args->options args)))

(defn- validate-options
  "Validates the options to make sure they contain valid values."
  [options]
  (-> options
      (update-in [:host] validate-host)
      (update-in [:port] to-int)
      (update-in [:repl] to-boolean)))

(defn update-options
  "Updates the project with validated options from the profile, project, and
  command line."
  [project args]
  (update-in project
             [:vimclojure-opts]
             merge (-> (merge-options project args)
                       validate-options)))

(defconstrainedfn ^{:private true} get-opt
  "Gets the value of the given plug-in option from the project."
  [project opt]
  [(:vimclojure-opts project)]
  (get-in project [:vimclojure-opts opt]))

(defmacro defoptfn
  "Creates a convenience function for getting the given option."
  [opt]
  `(defn ~(-> opt name symbol)
     ~(str "Get the value of the " opt " option from the project.")
     {:arglists '([~'project])}
     [project#]
     (get-opt project# ~opt)))

;; Create the convenience functions host, port, and repl
(defoptfn :host)
(defoptfn :port)
(defoptfn :repl)
