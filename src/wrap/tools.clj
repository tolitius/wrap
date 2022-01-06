(ns wrap.tools
  (:require [taoensso.nippy :as nippy]
            [cljhash.core :as hasher])
  (:import [com.google.common.hash Hashing
                                   Funnel
                                   PrimitiveSink]
           (clojure.lang Symbol)))

(def nippy-funnel
  (reify Funnel
    (^void funnel [_ obj ^PrimitiveSink sink]
      (.putBytes sink (nippy/freeze obj)))))

(defn hash-it [obj]
  (let [murmur (Hashing/murmur3_128)
        h (try
            (hasher/clj-hash murmur obj)
            (catch Exception e
              (hasher/hash-obj murmur nippy-funnel obj)))]
    (str h)))

(defn fmv
  "apply f to each value v of map m"
  [m f]
  (into {}
        (for [[k v] m]
          [k (f v)])))

(defn safe-read [s]
  (try
    (condp = (-> s read-string type)
      Symbol s
      (read-string s))
    (catch Exception e
      s)))

(defn page-details [page-number page-size total-count]
  {:totalCount total-count
   :pageNumber page-number
   :pageSize   page-size
   :totalPages (cond
                  (= page-size 0) 0
                  (zero? (mod total-count page-size)) (quot total-count page-size)
                  :else (-> (quot total-count page-size)
                            (+ 1)))})