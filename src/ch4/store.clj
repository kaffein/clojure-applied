(ns ch4.store)

;; Based on the ideas we developed from the previous explanations for our hypothetical inventory (in single.clj)
;; let's first define our inventory
(def inventory (atom {}))

;; let's also define a utility function that we will use as a `validator` function for our inventory.
;; A `validator` function is a function associated with a reference type and is called after the `data function` (via the `update function` call)
;; is applied to a `reference type`'s value and BEFORE the `NEW` value calculated by the `update function` is committed as the NEW value of the reference.
;; If the `validator` function returns `true`, then the new calculated value is committed to be the new value of the reference. Otherwise, the
;; whole update function fails and the reference value is reverted to its prior value i.e the value before the `data function` has been applied.
;; `validator` functions prevent data incoherence (in business parlance) e.g : we can not have a negative value as the number of items in the inventory.
(defn no-negative-values?
  "Checks that all item values within the inventory are positive"
  [m]
  (not-any? neg? (vals m)))

;; let's now implement a function, taking as arguments a list of items, to initialize our inventory
(defn init
  "Initializes the inventory with a list of items"
  [items]
  ;; we first associate the validator function we defined earlier to our inventory
  (set-validator! inventory no-negative-values?)
  ;; we `update` the reference type value using the appropriate `update function`, here `swap!` since it's an atom
  (swap! inventory merge items))

;; let's also define another utility function allowing to check whether an item is in stock or not
(defn in-stock?
  "Checks whether the provided item in argument is in stock or not"
  [item]
  (let [counter (item @inventory)]
    (and (pos? counter))))

;; finally, let's define two functions for grabing and stocking an item from/in the store shelves
(defn grab
  "Grabs an item from the store shelves"
  [item]
  (swap! inventory update-in [item] dec))

(defn stock
  "Stocks an item in the store shelves"
  [item]
  (swap! inventory update-in [item] inc))

;; if we define the following items
;(def items {:banana 7
;            :butter 2
;            :salt 1
;            :pizza 3
;            :juice 3})
;=> #'user/items

;; and init the store shelves with these items using the `init` function defined earlier
;(init items)
;=> {:banana 7, :butter 2, :salt 1, :pizza 3, :juice 3}

;; `grab`ing a banana from the store shelves would leave 6 bananas as shown in the following. The operation
;; would have ensured any concurrent access by another thread to be shielded from partial updates or
;; inconsistent data because we used the proper function, `swap!`, to update the reference value, which
;; is an `atom` here
;(grab :banana)
;=> {:banana 6, :butter 2, :salt 1, :pizza 3, :juice 3}

;; let's now consider, `grab`ing one salt item
;(grab :salt)
;=> {:banana 6, :butter 2, :salt 0, :pizza 3, :juice 3}

;; we now have 0 salt items left in the inventory and if we try to `grab` another salt item, we get the following
;; exception :
;(grab :salt)
;IllegalStateException Invalid reference state  clojure.lang.ARef.validate (ARef.java:33)

;; we got this exception because before committing the newly computed value of the reference (via `grab`), which
;; is -1 here, the underlying machinery triggered the `validator function` defined earlier (no-negative-values?)
;; to check whether the value to be committed complies with the `rule(s)` spec'ed within. In our case, the validation
;; function ensures that there is no negative values for items in the inventory.

;; now, let's `stock` an item on the shelves, let's say another salt pack since we do not have any left. This would
;; update the number of salt items by 1, giving us 1 salt pack
;(stock :salt)
;=> {:banana 6, :butter 2, :salt 1, :pizza 3, :juice 3}

