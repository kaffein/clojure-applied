(ns ch1.money)

;; This instruction is a function declaration allowing the named function to be
;; used even if its implementation is defined further in the file
(declare validate-same-currency)

;; This record represents a Currency
(defrecord Currency [divisor sym desc])

;; This record represents Money in the provided currency.
;; It implements the standard java Comparable interface allowing comparison based
;; on the currency and the amount
(defrecord Money [amount ^Currency currency]
  java.lang.Comparable
  (compareTo [m1 m2]
    (validate-same-currency m1 m2)
    (compare (:amount m1) (:amount m2))))

(def currencies {:usd (->Currency 100 "USD" "US Dollars")
                 :eur (->Currency 100 "EUR" "Euro")})

;; This is the implementation of the validate-same-currency method declared earlier.
;; The comparison logic is pretty obvious : two (2) Money entities have the same currency
;; if the :currency fields/slots of both have the same value
(defn- validate-same-currency
  [m1 m2]
  (or (= (:currency m1) (:currency m2))
      (throw
        (ex-info "Currencies do not match" {:m1 m1 :m2 m2}))))

;; This function allows equality comparison between Money entities.
;; It is a multi-arity function and provides comparison logic based on the number of
;; provided arguments : one (1) argument, two (2) arguments, two or more (2+) arguments.
;; REMINDER : the comparison function in the #java.lang.Comparable interface returns 0 if
;; the compared arguments are equal, hence the use of zero? in the comparison logic.
(defn =$
  ([m1] true)
  ([m1 m2] (zero? (.compareTo m1 m2)))
  ([m1 m2 & monies] (every? zero? (map #(.compareTo m1 %) (conj monies m2)))))

;; This function provides the logic for adding Money entities. Like the equality function,
;; it is a multi-arity function and provides addition logic based on the number of
;; provided arguments : one (1) argument, two (2) arguments, two or more (2+) arguments.
;; The third arity variant also uses a recursion call to the function itself as a reduction
;; function to process its input.
(defn +$
  ([m1] m1)
  ([m1 m2]
   (validate-same-currency m1 m2)
   (->Money (+ (:amount m1) (:amount m2)) (:currency m1)))
  ([m1 m2 & monies]
   (reduce +$ m1 (conj monies m2))))

;; This function mulitplies the Money (m) entity value by n
(defn *$
  [m n]
  (* (:amount m) n) (:currency m))

;; This is a convenient constructor allowing for sensible defaults when currency and/or amounts
;; are not provided by the caller.
;; Note that the third arity of the function is a generalization of the Money entity creation hence
;; the first and second arity of the function are using this third arity when some arguments are
;; missing (e.g : no argument provided in the first arity, amount-only in the second arity)
;; In the end, the third arity is always the variant to be called.
(defn make-money
  ([] (make-money 0))
  ([amount] (make-money amount (:usd currencies)))
  ([amount currency] (->Money amount currency)))

;; When destructuring by position, values likely to be used are placed earlier in the argument
;; list which is constraining

;; Sometimes, we may be tempted to represent zero quantity or any empty container with a function
;; returning the same value when called like so
(defn new-money
  "$0.00 usd"
  []
  (->Money 0 :usd))

;; It is easier though, to take advantage of Clojure's immutable values and to, instead, return
;; a "value" rather than a function
(def zero-dollars (->Money 0 :usd))