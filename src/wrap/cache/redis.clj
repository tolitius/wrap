(ns wrap.cache.redis
  (:require [wrap.core :as w]
            [wrap.cache.core :as c]
            [wrap.tools :as t]
            [taoensso.encore :as encore]
            [obiwan.core :as redis]))

(defn make-key [k key-fn args]
  ;; use key-fn from user to extract the key value
  ;; else hash all arguments and use as key

  ;; dev=> (make-key "test" #(-> % last :foo) ["baz" {:foo "bar"}])
  ;; "test:bar"
  ;; dev=> (make-key "keys" first ["foo" "bar" "baz"])
  ;; "keys:foo"
    (let [key (if key-fn (key-fn args) (t/hash-it args))]
      (str k " : " key)))

;; if more than redis place this ns behind a couple of protocols

(defn lookup [conn prefix key-fn args]
  (when-let [v (redis/get conn
                          (make-key prefix key-fn args))]
    (encore/read-edn v)))

(defn store [conn prefix key-fn args v]
  (when (and v args)
    (redis/set conn
               (make-key prefix key-fn args)
               (encore/pr-edn v))))

(defn delete [conn prefix args key-fn]
  (when args
    (redis/del conn
               [(make-key prefix key-fn args)])))

;; wrappers
(defn cache [conn prefix fs & [key-fn]]
  (w/wrap fs
          (c/cache (partial lookup conn prefix key-fn)
                   (partial store conn prefix key-fn))))

(defn evict [conn prefix fs & [key-fn]]
  (w/wrap fs
          (c/evict (partial delete conn prefix key-fn))))

(defn put [conn prefix fs & [key-fn]]
  (w/wrap fs
          (c/put (partial store conn prefix key-fn))))

