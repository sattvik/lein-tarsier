(ns leiningen.vimclojure.deps
  "Handles adding dependencies to projects."
  {:author "Daniel Solano Gómez"}
  (:require [leinjacker.deps :as deps])
  (:use [trammel.core :only [defconstrainedfn]]))

(def ^{:private true
       :doc "The name  of the VimClojure dependency."}
  vimclojure-spec ['vimclojure/server "2.3.1"])

(defn has-vimclojure?
  "Returns a value that evaluates to true if the project has a VimClojure
  server dependency."
  [project]
  (deps/has-dep? project vimclojure-spec))

(defconstrainedfn add-vimclojure
  "Adds the VimClojure server as a dependency of the project."
  [project]
  [=> has-vimclojure?]
  (deps/add-if-missing project vimclojure-spec))
