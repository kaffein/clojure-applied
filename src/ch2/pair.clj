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
  (import [clojure.lang Seqable Counted Indexed ILookup]
          [java.io Writer]))

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
;p
;=> #object[ch2.pair.Pair 0xca6913c "ch2.pair.Pair@ca6913c"]


;; on the last line of this REPL session though, we tried to display p. The REPL
;; then displayed the default representation of the type which consists
;; in the class name and an identifier. Not very usable nor obvious at first sight.
;; We would like to change that so that it displays a more human-readable representation
;; but also one that the underlying Clojure evaluation mechanism can understand.

;; In Clojure, there is a mechanism that allows converting a textual representation of
;; a data type, i.e a literal representation, to actual internal Clojure data, understood
;; by the underlying interpretation machinery.

;; This is the same mechanism used by the REPL. In fact, the actual R and E in REPL stand for :
;;  - R : read-ing the textual representation from the input (*out* by default) via
;;        the 'read-str' function.
;;  - E : converting the textual representation to Clojure data and eval-uating that data
;;        as a Clojure expression.

;; if the R-eading mechanism allows converting from textual data representation to actual
;; Clojure data to be fed into the REPL, there is an equivalent but opposite operation that
;; allows converting a Clojure internal data representation to the actual textual representation.
;; The 'pr-str' function does just that. This function is based on the 'pr' function whose
;; documentation states its use as :

; (doc pr)
; -------------------------
; clojure.core/pr
; ([] [x] [x & more])
; Prints the object(s) to the output stream that is the current value
; of *out*.  Prints the object(s), separated by spaces if there is
; more than one.  By default, pr and prn print in a way that objects
; can be read by the reader

;; The whole 'print'-ing mechanism is supported by an open system with multimethods defining
;; two (2) main functions that has to be implemented :
;;  * print-method : for displaying a user/human-readable representation
;;  * print-dup : for displaying a 'reader'(CLJ machinery)-readable representation

;; for the sake of example, we will print the same thing for both functions
(defmethod print-method Pair
  [pair ^Writer writer]
  (.write writer "#ch2.pair.Pair")
  (print-method (vec (seq pair)) writer))


(defmethod print-dup Pair
  [pair ^Writer writer]
  (print-method pair writer))

;; Now on the REPL, if we define a Pair p...
;(def p (->Pair 1 2))
;=> #'user/p

;; printing p now displays its Clojure data textual representation
;p
;=> #ch2.pair.Pair[1 2]

;; To demonstrate that we can now go back and forth form textual
;; representation to clojure data, we can take this textual representation to
;; be fed as string to the 'reader' and be eval-ed
;(read-string "#ch2.pair.Pair[1 2]")
;=> #ch2.pair.Pair[1 2]

;; To go further, we can even invoke the 'class' function on it to make
;; sure its type/class is what we expect it to be, i.e a 'Pair'...
;(class (read-string "#ch2.pair.Pair[1 2]"))
;=> ch2.pair.Pair