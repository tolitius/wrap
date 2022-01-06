(ns wrap.cache.redis
  (:require [wrap.core :as w]
            [wrap.cache.core :as c]
            [wrap.tools :as t]
            [obiwan.core :as redis]
            [obiwan.search.core :as search]
            [taoensso.encore :as encore]
            [taoensso.nippy :as nippy]
            [clojure.string :as str]))

(defn- make-key [k key-fn args]
  ;; using the function user sends to extract the key
  ;; else hash the entire object as the key
  (let [key-val (key-fn args)]
    (str k ":" key-val)))

(defn build-search-filter [arg]
  (if (map? arg)
    (let [search-by (some-> arg :search-by (str "*"))
          rest (dissoc arg :search-by :sort-by :page-number :page-size)
          filters (mapv #(str "@" (-> % key name) ":" (val %)) rest)]
      (->> (str/join " " filters)
           (str search-by " ")))
    arg))

;; if more than redis place this ns behind a couple of protocols
(defn lookup [conn prefix key-fn args]
  (when-let [v (redis/get conn
                          (make-key prefix key-fn args))]
    (-> v .getBytes nippy/thaw encore/read-edn)))

(defn store [conn prefix key-fn args v]
  (when (and v args)
    (redis/set conn
               (make-key prefix key-fn args)
               (-> v encore/pr-edn nippy/freeze String.))))

(defn delete [conn prefix key-fn args]
  (when args
    (redis/del conn
               [(make-key prefix key-fn args)])))

(defn do-search [conn index key-fn args]
  (when args
    (let [{:keys [pageNumber pageSize sortBy] :or {pageNumber 1 pageSize 10}} args
          offset  (* (dec pageNumber) pageSize)
          paging [{:limit {:number pageSize :offset offset}}]
          {:keys [field direction]} sortBy
          paging-with-sort (if sortBy (conj paging {:sort {:by {field (keyword direction)}}}) paging)
          {:keys [results found]} (search/ft-search conn index
                                            (-> args key-fn build-search-filter)
                                            paging-with-sort)]
    {:data        (mapv #(-> % first val (t/fmv t/safe-read)) results)
     :pageDetails (t/page-details pageNumber pageSize found)})))

;; wrappers
(defn cache [conn fs {:keys [prefix key-fn]
                      :or   {key-fn t/hash-it}}]
  (w/wrap fs
          (c/cache (partial lookup conn prefix key-fn)
                   (partial store conn prefix key-fn))))

(defn evict [conn fs {:keys [prefix key-fn]
                      :or   {key-fn t/hash-it}}]
  (w/wrap fs
          (c/evict (partial delete conn prefix key-fn))))

(defn put [conn fs {:keys [prefix key-fn]
                    :or   {key-fn t/hash-it}}]
  (w/wrap fs
          (c/put (partial store conn prefix key-fn))))

(defn search [conn fs {:keys [index key-fn]
                       :or   {key-fn identity}}]
  (w/wrap fs
          (c/search (partial do-search conn index key-fn))))

