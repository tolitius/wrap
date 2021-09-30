(ns wrap.cache.redis
  (:require [wrap.core :as w]
            [wrap.cache.core :as c]
            [wrap.tools :as t]
            [taoensso.nippy :as nippy]
            [obiwan.core :as redis]))

(defn- make-key [k args]
  ;; (!) dropping the first arg by assuming it is a db/IO resources is
  ;; a hack: needs more thinking
  (let [without-db (rest args)] ;; <<< needs more thinking
    (str k ":" (t/hash-it without-db))))

;; if more than redis place this ns behind a couple of protocols

(defn lookup [conn prefix args]
  (when-let [v (redis/get conn
                          (make-key prefix args))]
    (nippy/thaw (.getBytes v))))

(defn store [conn prefix args v]
  (when (and v args)
    (redis/set conn
               (make-key prefix args)
               (String. (nippy/freeze v)))))

(defn delete [conn prefix args]
  (when args
    (redis/del conn
               [(make-key prefix args)])))

;; wrappers
(defn cache [conn prefix fs]
  (w/wrap fs
          (c/cache (partial lookup conn prefix)
                   (partial store conn prefix))))

(defn evict [conn prefix fs]
  (w/wrap fs
          (c/evict (partial delete conn prefix))))

(defn put [conn prefix fs]
  (w/wrap fs
          (c/put (partial store conn prefix))))

