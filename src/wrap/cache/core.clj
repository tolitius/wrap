(ns wrap.cache.core)

(defn cache [lookup store]
  (fn [f & args]
    (or (lookup args)
        (let [v (apply f args)]
          (store args v)
          v))))

(defn evict [delete]
  (fn [f & args]
    (delete args)
    (apply f args)))

(defn put [store]
  (fn [f & args]
    (let [v (apply f args)]
      (store args v)
      v)))
