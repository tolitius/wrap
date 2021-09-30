# wrap

I wrap functions

I wrap them before<br/>
I wrap them after<br/>
I wrap them around<br/>

[![<! release](https://img.shields.io/badge/dynamic/json.svg?label=release&url=https%3A%2F%2Fclojars.org%2Fcom.tolitius%2Fwrap%2Flatest-version.json&query=version&colorB=blue)](https://github.com/tolitius/wrap/releases)
[![<! clojars>](https://img.shields.io/clojars/v/com.tolitius/wrap.svg)](https://clojars.org/com.tolitius/wrap)

- [what is it about?](#what-is-it-about)
- [caching](#caching)
- [is it for me?](#is-it-for-me)

## what is it about?

love

love is all you need

> _docs are TBD_

## caching

experimenting with an AOP style caching:

let's create a database and db functions:

```bash
$ make repl
```

```clojure
=>
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

;; #'dev/take-time
;; #'dev/db
;; #'dev/find-planet
;; #'dev/add-planet
;; #'dev/remove-planet
```

now let's wrap all these functions in cache<br/>
in this case redis:

```clojure
=> (require '[obiwan.core :as redis]
            '[wrap.core :as w]
            '[wrap.cache.redis :as wc])

=> (def conn (redis/create-pool))
```

```clojure
;; read through cache
=> (wc/cache conn "planets" #{#'dev/find-planet})

;; evict when removing the planet
=> (wc/evict conn "planets" #{#'dev/remove-planet})

;; put in cache when adding a planet
=> (wc/put conn "planets" #{#'dev/add-planet})
```

wrap follows [calip](https://github.com/tolitius/calip) so a set of functions in one or more namespaces<br/>
or a [star search](https://github.com/tolitius/calip) of functions to apply wrappers to will work the same

internally, these wrappers look something like [this](src/wrap/cache/redis.clj#L32) (you can define your own if these don't fit the need):

```clojure
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
```

now let's see wrappers in action:

```clojure
=> (find-planet db {:planet :earth})
;; taking 3 seconds
;; {:nick "the blue planet", :age "4.543 billion years", :mass "5974000000000000000000000"}
```

took some time since we "went" to the database for this search<br/>
but once we did that, now it is cached:

```clojure
=> (find-planet db {:planet :earth})
;; {:nick "the blue planet", :age "4.543 billion years", :mass "5974000000000000000000000"}
```

once we remove the planet, the cache entry for it is going to be evicted:

```clojure
=> (remove-planet db {:planet :earth})
=> (find-planet db {:planet :earth})
;; taking 3 seconds
;; nil
```

now let's try to add pluto:

```clojure
=> (find-planet db {:planet :pluto})
;; taking 3 seconds
;; nil

=> (add-planet db {:planet :pluto
                   :intel {:nick "tombaugh regio"
                           :age "4.5 billion years"
                           :mass "13090000000000000000000"}})
```

it is cached right away:

```clojure
=> (find-planet db {:planet :pluto})
;; {:nick "tombaugh regio", :age "4.5 billion years", :mass "13090000000000000000000"}
```

functions can be also unwrapped (wrappers will be stripped):

```clojure
=> (w/unwrap #{#'dev/find-planet})

=> (find-planet db {:planet :pluto})
;; taking 3 seconds
;; {:nick "tombaugh regio", :age "4.5 billion years", :mass "13090000000000000000000"}
```

## is it for me?

this lib is not exactly for general use since it does have deps:

* [nippy](https://github.com/ptaoussanis/nippy)
* [clojure hash](https://github.com/danboykis/cljhash)
* [obiwan](https://github.com/tolitius/obiwan)
* [calip](https://github.com/tolitius/calip)

plus it "assumes things"<br/>
feel free to either use as is, or to steal code from it should you find it useful

## license

Copyright © 2021 tolitius

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
