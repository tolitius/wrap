(ns wrap.tools
  (:require [taoensso.nippy :as nippy]
            [cljhash.core :as hasher])
  (:import [com.google.common.hash Hashing
                                   Funnel
                                   PrimitiveSink]))

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
