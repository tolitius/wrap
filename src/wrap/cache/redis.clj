(ns wrap.cache.redis
  (:require [wrap.core :as w]
            [wrap.cache.core :as c]
            [wrap.tools :as t]
            [taoensso.nippy :as nippy]
            [obiwan.core :as redis]))

(defn- make-default-key [k args]
  (case (count args)
        0     k                      ;; TODO might need a warning
        1     (first args)
        (t/hash-it (rest args))))

(defn- make-key [k cache-by args]
  ;; using the function user sends to extract the key
  ;; else hash the entire object as the key
  (str k ":" (if cache-by
               (cache-by args)
               (make-default-key k args))))

;; if more than redis place this ns behind a couple of protocols
(defn lookup [conn prefix cache-by args]
  (when-let [v (redis/get conn
                          (make-key prefix cache-by args))]
    (nippy/thaw-from-string v {:incl-metadata? false})))

(defn store [conn prefix cache-by args v]
  (when (and v args)
    (redis/set conn
               (make-key prefix cache-by args)
               (nippy/freeze-to-string v {:incl-metadata? false}))))

(defn delete [conn prefix cache-by args]
  (when args
    (redis/del conn
               [(make-key prefix cache-by args)])))

;; wrappers
(defn cache [conn fs {:keys [prefix cache-by]}]
  (w/wrap fs
          (c/cache (partial lookup conn prefix cache-by)
                   (partial store conn prefix cache-by))))

(defn evict [conn fs {:keys [prefix cache-by]}]
  (w/wrap fs
          (c/evict (partial delete conn prefix cache-by))))

(defn put [conn fs {:keys [prefix cache-by]}]
  (w/wrap fs
          (c/put (partial store conn prefix cache-by))))
