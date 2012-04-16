(ns leiningen.vimclojure
  "Adds VimClojure server support to a Leiningen project."
  {:author "Daniel Solano Gómez"}
  (:require [leiningen.vimclojure
               [eval-in-project :as eip]
               [deps :as deps]]
            [leiningen.vimclojure.options.vimclojure :as vimopt])
  (:use [trammel.core :only [defconstrainedfn]])
  (:import java.net.InetAddress))

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
  [deps/has-vimclojure?
   (instance? InetAddress (vimopt/get-opt project :host))
   (integer? (vimopt/get-opt project :port))]
  (let [host (.getHostAddress (vimopt/get-opt project :host))
        port (str (vimopt/get-opt project :port))]
    (if (vimopt/get-opt project :repl)
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
  (let [project (-> project
                    (vimopt/update-options args)
                    deps/add-vimclojure)]
    (eip/eval-in-project project (vimclojure-form project))))
