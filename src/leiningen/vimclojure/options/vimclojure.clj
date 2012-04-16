(ns leiningen.vimclojure.options.vimclojure
  "Handles the :vimclojure-opts configuration."
  {:author "Daniel Solano Gómez"}
  (:import java.net.InetAddress)
  (:use [trammel.core :only [defconstrainedfn]]))

(def ^{:private true
       :doc "Default values for the plug-in options."}
  default-opts
  {:host (InetAddress/getByName "127.0.0.1")
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

(defconstrainedfn get-opt
  "Gets the value of the given plug-in option from the project."
  [project opt]
  [(:vimclojure-opts project)]
  (get-in project [:vimclojure-opts opt]))
