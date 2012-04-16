(ns leiningen.vimclojure.deps
  "Handles adding dependencies to projects."
  {:author "Daniel Solano Gómez"}
  (:use [trammel.core :only [defconstrainedfn]]))

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

(defconstrainedfn has-vimclojure?
  "Returns a value that evaluates to true if the project has a
  vimclojure/server dependency."
  [project]
  [(vector? (:dependencies project))]
  (some #(= vimclojure-package (first %)) (:dependencies project)))

(defconstrainedfn add-vimclojure
  "Adds the VimClojure server as a dependency of the project."
  [project]
  [=> has-vimclojure?]
  (if (has-vimclojure? project)
    project
    (update-in project [:dependencies] conj vimclojure-dep)))
