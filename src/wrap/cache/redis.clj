(ns wrap.cache.redis
  (:require [wrap.core :as w]
            [wrap.cache.core :as c]
            [wrap.tools :as t]
            [taoensso.nippy :as nippy]
            [obiwan.core :as redis]
            [clojure.tools.logging :as log]))

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

(defn- ->set-params [time-to-live]
  ;; this function will apply critical params to keys like ttl
  ;; and can expand to cater redis.clients.jedis.params.SetParams.
  (cond-> {}
          (and (some? time-to-live)
               (pos? time-to-live)) (merge {:ex time-to-live})))

(defn store [conn prefix cache-by time-to-live args v]
  (when (and v args)
    (redis/set conn
               (make-key prefix cache-by args)
               (nippy/freeze-to-string v {:incl-metadata? false})
               (->set-params time-to-live))))

(defn delete [conn prefix cache-by args]
  (when args
    (redis/del conn
               [(make-key prefix cache-by args)])))

(defn- suppress [f op]
  (fn [& args]
    (try
      (apply f args)
      (catch Throwable e
        (log/error e "cache" op "failed; serving from source")
        nil))))

;; wrappers
(defn cache
  ":on-error :throw  => cache errors propagate (default)
             :ignore => cache errors are logged; lookup errors
                        become misses (fn is called), store
                        errors are dropped (result is still
                        returned to the caller)"
  [conn fs {:keys [prefix cache-by time-to-live on-error]
            :or   {on-error :throw}}]
  (let [lookup-fn (cond-> (partial lookup conn prefix cache-by)
                          (= on-error :ignore) (suppress :lookup))
        store-fn  (cond-> (partial store conn prefix cache-by time-to-live)
                          (= on-error :ignore) (suppress :store))]
    (w/wrap fs
            (c/cache lookup-fn store-fn))))

(defn evict [conn fs {:keys [prefix cache-by]}]
  (w/wrap fs
          (c/evict (partial delete conn prefix cache-by))))

(defn put [conn fs {:keys [prefix cache-by]}]
  (w/wrap fs
          (c/put (partial store conn prefix cache-by))))
