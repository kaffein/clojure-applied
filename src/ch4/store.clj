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


;; Now that we have everything set and tested, we can now re-work our `go-shopping-naive` function from single.clj
(defn shop-for-item
  "Shop for an item from the inventory and return the updated cart"
  [cart item]
  (if (grab item)
    (conj cart item)
    cart))

(defn go-shopping
  "Return the list of purchased items"
  [shopping-list]
  (reduce shop-for-item [] shopping-list))

;; We even allowed ourselves to be `idiomatic` by having the `loop` replaced with a higher-level construct :
;; `reduce`.
;; Given the example inventory we had earlier :
; (init items)
; => {:banana 7, :butter 2, :salt 1, :pizza 3, :juice 3}

;; shopping for banana, salt and juice would give us our cart content updated with these items :
; (go-shopping [:banana :salt :juice])
; => [:banana :salt :juice]

;; and listing what is available within the inventory would give us the content minus the items
;; we put in our cart :
; @inventory
; => {:banana 6, :butter 2, :salt 0, :pizza 3, :juice 2}

;; At this stage, trying to grab another salt item would give us the following exception :
; (go-shopping [:salt])
; IllegalStateException Invalid reference state  clojure.lang.ARef.validate (ARef.java:33)

;; our validation function triggered its logic to ensure that there are no items whose count is negative,
;; in the inventory. Since we did not have any salt item left (count = 0), the validation-function did not
;; allow the operation of grabbing one from the inventory to occur.

;; at this stage, if we check again the content of the inventory, we see that the previous content has not been
;; updated because of the exception thrown by the validation function :
; @inventory
; => {:banana 6, :butter 2, :salt 0, :pizza 3, :juice 2}


;; We have seen earlier the use of `validation functions` as a way to ensure that data within the system
;; remain coherent if a change is applied to a reference type value, but also to prevent such operation from
;; occuring if applying the new value may cause inconsistencies within the system.
;; Here, we introduce `watches` as a way to get notified when the actual changes to the reference type value
;; has been applied i.e the new value has been assigned to the reference type.
;; `watches` are nothing more than an implementation of the `Observer pattern` in OOP but Clojure's FP flavor of
;; it is simpler and easier to grasp and use.

;; So what exactly is a `watch` : it simply is a function taking four parameters :
;; (defn watch-fn [watch-key reference old-value new-value] ,,,)
;; The last three parameters are self-explanatory. The `watch-key` is used as a label or an ID that identifies the watch
;; function among other watch functions attached to the reference. In fact, you can attach more than one
;; watch function to a reference and the `watch-key` allows us to refer to a particular watch function in order
;; to update (add-watch) or remove (remove-watch) it from the reference for example.
;; Each time the value of the reference changes, i.e : `old-value` is different from `new-value`, all watch functions
;; attached to the reference are triggered.

;; As an illustration, let's now say that we would like to be able to have a record of all grabbed items (let's call
;; it `sold-items`) from the inventory but also be notified that a 'restock' is needed.
;; We first define the `sold-items` reference
(def sold-items (atom {}))

;; we then implement a watch function, let's call it `restock-order`, that will be attached to our `inventory` reference.
(defn restock-order
  "A watch function that notifies that an item restock is needed."
  [k r old-value new-value]
  (doseq [item (for [item-key (keys old-value)
                     :when (not= (item-key old-value) (item-key new-value))] item-key)]
    ;; we first record the grabbed item (from the inventory) into the sold-items reference
    (swap! sold-items update-in [item] (fnil inc 0))
    (println "A restock is needed for" item)))

;; let's label the watch function with the :restock key
(add-watch inventory :restock restock-order)
; => #object[clojure.lang.Atom 0x74cb5dbe {:status :ready, :val {:banana 3, :butter 2, :salt 0, :pizza 3, :juice 2}}]

;; let's make sure that our watch function has actually been attached to our `inventory` reference, we can invoke :
; (.getWatches inventory)
; => {:restock #object[user$restock_order 0xee0aaf5 "user$restock_order@ee0aaf5"]}

;; Now if we go shopping for :banana, we get notified as we grab the item from the inventory :
; (go-shopping [:banana])
; A restock is needed for :banana
; => [:banana]

;; checking `sold-items` and `inventory` references content respectively, gives :
; @sold-items
; => {:banana 1}
; @inventory
; => {:banana 2, :butter 2, :salt 0, :pizza 3, :juice 2}