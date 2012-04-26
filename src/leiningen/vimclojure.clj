(ns leiningen.vimclojure
  "Adds VimClojure server support to a Leiningen project."
  {:author "Daniel Solano Gómez"}
  (:require [leinjacker.eval-in-project :as eip]
            [leiningen.vimclojure.deps :as deps]
            [leiningen.vimclojure.options.tarsier :as tarsier-opts])
  (:use [trammel.core :only [defconstrainedfn]])
  (:import java.net.InetAddress))

(defn- form?
  "Checks to see if the argument is a form."
  [x]
  (instance? clojure.lang.Cons x))

(defconstrainedfn with-server
  "Creates a form that will create a VimClojure server on the given host and
  port.  It will then call the thunk with the server as an argument."
  [host port thunk]
  [(string? host) (integer? port) (form? thunk) => form?]
  `(let [server#      (~'vimclojure.nailgun.NGServer. 
                          (java.net.InetAddress/getByName ~host)
                          (Integer/parseInt ~(str port)))
         shutdowner#  (Thread. (reify Runnable
                                 (run [_]
                                   (.shutdown server# false))))]
     (.. (Runtime/getRuntime) (addShutdownHook shutdowner#))
     (~thunk server#)))

(defconstrainedfn launch-server
  "Launches the server and cleanly shuts it down when the user interrupts the
  process."
  [project host port]
  [(string? host) (integer? port)]
  (eip/eval-in-project
    project
    (with-server host port
      `(fn [server#]
         (println (str "Starting VimClojure server on " ~host ", port " ~port))
         (println "Happy hacking!")
         (.run server#)))))

(defconstrainedfn launch-repl
  "Generates a form that will run the server in the background and provide a
  REPL in the foreground.  The server will shut down when the REPL terminates."
  [project host port]
  [(string? host) (integer? port)]
  (eip/eval-in-project project
    (with-server host port
      `(fn [server#]
         (let [thread# (Thread. server#)]
           (println (str "Starting VimClojure server on " ~host ", port " ~port))
           (.start thread#)
           (println "Clojure" (clojure-version))
           (clojure.main/repl)
           (.shutdown server# false))))))

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
  (let [project (-> project
                    (tarsier-opts/update-options args)
                    deps/add-vimclojure)
        host    (tarsier-opts/host project)
        port    (tarsier-opts/port project)]
    (if (tarsier-opts/repl project)
      (launch-repl project host port)
      (launch-server project host port))))
