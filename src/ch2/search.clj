(ns ch2.search)

;; Unlike indexed collections where value lookup is done effectively in constant time,
;; searching in sequential collections like lists takes time proportional to the size
;; of the collection.
;; The most common way of searching for a value in an unindexed collection is via
;; 'some'. This function takes a predicate p, evaluates it with each value val
;; of the collection, i.e : (p val), and returns the first logically 'true' value
;; that matches its predicates if it finds one or returns nil instead.

;; For example :
(def units [:oz :lb :kg])

;; The Clojure idiomatic way of looking up a value with 'some' is by using the collection
;; itself as a predicate. In fact, Clojure collections are functions that look up value
;; associated with key or index it is provided with.
;; And since 'some' in our case will take each value of 'units' as a parameter for its
;; predicate, we can use the collection itself as the predicate (the function to be applied)
;; because the collection can look up any value associated with each key/index provided
;; to it, in our case each value of unit.
(some #{:oz} units)
;; which gives
;; :oz

;; There is a flaw though, our implementation breaks if we try to search for 'nil'
;; or 'false' since they are logically fals(e)-y values...