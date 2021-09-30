(ns dev)

;; db
(defn take-time [seconds]
  (println "taking" seconds "seconds")
  (Thread/sleep (* seconds 1000)))

(def db (atom {:mars {:nick "the red planet"
                      :age "4.603 billion years"
                      :mass "639000000000000000000000"}
               :earth {:nick "the blue planet"
                       :age "4.543 billion years"
                       :mass "5974000000000000000000000"}}))

(defn find-planet [db {:keys [planet]}]
  (take-time 3)
  (@db planet))

(defn add-planet [db {:keys [planet intel]}]
  (swap! db assoc planet intel))

(defn remove-planet [db {:keys [planet]}]
  (swap! db dissoc planet))


(comment

(require '[obiwan.core :as redis]
         '[wrap.core :as w]
         '[wrap.cache.redis :as wc])

(def conn (redis/create-pool))

(wc/cache conn "planets" #{#'dev/find-planet})
(wc/evict conn "planets" #{#'dev/remove-planet})
(wc/put conn "planets" #{#'dev/add-planet})

(find-planet db {:planet :earth})
(find-planet db {:planet :earth})

(remove-planet db {:planet :earth})

(find-planet db {:planet :pluto})

(add-planet db {:planet :pluto
                 :intel {:nick "tombaugh regio"
                         :age "4.5 billion years"
                         :mass "13090000000000000000000"}})

(find-planet db {:planet :pluto})

(find-planet db {:planet :mars})
(find-planet db {:planet :mars})

(w/unwrap #{#'dev/find-planet})
(find-planet db {:planet :mars})
)
