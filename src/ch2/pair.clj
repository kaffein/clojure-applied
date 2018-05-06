;; In Clojure, Collections are not based on some specific implementations but on
;; a generic set of traits defining key abstractions, which are the 'behaviour'
;; (i.e functions) these collection types are expected to have implemented, in order
;; to be considered as having the declared 'trait'.
;; e.g : ILookup, Reversible, Sorted etc.

;; These key abstractions are 'contracts' and Clojure traits are implemented internally
;; by Java interfaces with predicates provided within the standard library, allowing the
;; detection of the fulfillment of these contracts on a specific collection implementation.

;; It is then possible to define new collection types when the ones provided
;; with the standard library are not sufficient for a specific need/use case, by
;; implementing those traits and the abstractions defined within.

;; Let's define a new collection type, a 'Pair', which does not exist in the Clojure
;; standard library. In order to have it implemented : we first need to define
;; the behaviour this new collection is exptected to have.

;; Let's say that we want the Pair collection elements to :
;;  - be stored in a traversable order
;;  - be countable
;;  - be indexable/rankable and accessible via its index
;;  - be search-able via its index

;; based on this behaviour specification, we can deduce the set of traits that
;; the Pair collection has to implement.
;;  * to be stored in a traversable order means : it has to implement the 'seq'
;;    abstraction defined in the 'Seqable' trait.

;;  * to be be countable means : it has to implement the 'count' abstraction
;;    defined in the 'Counted' trait.

;;  * to be indexable/rankable and accessible via its index means : it has to
;;    implement the 'nth' abstraction defined in the 'Indexed' trait.

;;  * to be search-able via its index means : it has to implement the 'get'
;;    abstraction defined in the 'ILookup' trait.

;; We then first begin by importing those traits
(ns ch2.pair
  (import [clojure.lang Seqable Counted Indexed ILookup]))

;; In Clojure, there are two (2) ways of defining types :
;;  - defrecord (seen earlier in ch2/modeling.clj)
;;  - deftype

;; defrecord is used for defining and modeling 'domain' types which are
;; entities/concepts related to the domain the application is solving
;; the problem for (e.g : Person, Order etc.).
;; It is a higher level construct that provides features such as the map
;; factory function, for instantiating entities, and other niceties helping
;; the developer focus on the domain problem to be solved.

;; deftype on the other side is a lower level construct and is intended
;; to be used to define types as programming constructs, i.e types that
;; are part of the language type infrastructure (like String, Array,
;; custom collections etc.) Unlike defrecord for instance, it does not
;; offer the map factory function as a constructor and it allows the use
;; of mutable and unsynchronized fields, which are very low-level construct
;; allowing for performance tweaking etc.

;; Since we are defining a new collection type, we will then use the 'deftype'
;; macro to define it by implementing each key abstractions provided by the
;; traits seen earlier : Seqable, Counted, Indexed and ILookup
(deftype Pair [a b]
  Seqable
  (seq [_] (seq [a b]))

  Counted
  (count [_] 2)

  Indexed
  (nth [_ i]
    (case i
      0 a
      1 b
      (throw (IllegalArgumentException.))))
  (nth [this i _] (nth this i))

  ILookup
  (valAt [_ k _]
    (case k
      0 a
      1 b
      (throw (IllegalArgumentException.))))
  (valAt [this k]
    (.valAt this k nil)))

;; a REPL session with this newly-defined type shows that we actually
;; succeed at implementing our own logic for the key abstractions
;; defined in the traits we wanted our custom collection to have.
;(use 'ch2.pair)
;=> nil
;(def p (->Pair :a :b))
;=> #'user/p
;(seq p)
;=> (:a :b)
;(count p)
;=> 2
;(nth p 1)
;=> :b
;(nth p 2)
;IllegalArgumentException   ch2.pair.Pair (pair.clj:69)
;(get p 0)
;=> :a
;(get p 3)
;IllegalArgumentException   ch2.pair.Pair (pair.clj:77)