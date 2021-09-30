(ns wrap.core
  (:require [calip.core :as calip]
            [robert.hooke :as hooke]))

(defn wrap
  "takes a set of functions (namespace vars) and a function to wrap them with

   i.e. (wrap #{#'app/foo #'app/bar} (fn [f & args]
                                       (println \"before..\")
                                       (apply f args)
                                       (println \"after..\")))"
  [fs wrapper]
  (doseq [f (#'calip.core/unwrap-stars fs)]
    (let [fvar (#'calip.core/f-to-var f)]
      (println "wrapping" fvar)
      (hooke/add-hook fvar          ;; target var
                      (str fvar)    ;; hooke key
                      wrapper))))   ;; wrapper

(defn unwrap [fs]
  "takes a set of functions (namespace vars) and removes wrappers from them.
   i.e. (unwrap #{#'app/foo #'app/bar})"
  (doseq [f (#'calip.core/unwrap-stars fs)]
    (hooke/clear-hooks
      (#'calip.core/f-to-var f))
    (println "remove a wrapper from" f)))
