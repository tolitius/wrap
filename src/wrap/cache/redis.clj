(ns wrap.cache.redis
  (:require [wrap.core :as w]
            [wrap.cache.core :as c]
            [wrap.tools :as t]
            [obiwan.core :as redis]
            [obiwan.search.core :as search]
            [taoensso.encore :as encore]
            [taoensso.nippy :as nippy]
            [clojure.string :as str]))

(defn- make-key [k cache-by args]
  ;; using the function user sends to extract the key
  ;; else hash the entire object as the key
  (let [key-val (cond cache-by (cache-by args)
                      (= 1 (count args)) (first args)
                      :else (t/hash-it (rest args)))]
    (str k ":" key-val)))


;; if more than redis place this ns behind a couple of protocols
(defn lookup [conn prefix cache-by args]
  (when-let [v (redis/get conn
                          (make-key prefix cache-by args))]
    (-> v .getBytes nippy/thaw encore/read-edn)))

(defn store [conn prefix cache-by args v]
  (when (and v args)
    (redis/set conn
               (make-key prefix cache-by args)
               (-> v encore/pr-edn nippy/freeze String.))))

(defn delete [conn prefix cache-by args]
  (when args
    (redis/del conn
               [(make-key prefix cache-by args)])))

(defn do-search [conn index cache-by args page-by]
  (when args
    (search/ft-search conn index
                      (cache-by args)
                      (some-> page-by args))))

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

(defn search [conn fs {:keys [index cache-by page-by]
                       :or   {cache-by identity}}]
  (w/wrap fs
          (c/search (partial do-search conn index cache-by page-by))))

