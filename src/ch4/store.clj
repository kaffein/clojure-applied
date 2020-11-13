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


;; Sometimes, `transactionality` can be a requirement when updating reference types. For e.g to ensure that
;; values of multiple refs are to be udpated altogether because they are interdependent.
;; When multiple reference values need to be changed `transactionally` and in a `synchronous fashion`, we use `ref`s.
;; `ref` used within the context of a transaction allows the update of a reference value and ensures that the changes
;; occur altogether or not-at-all. This latter case occurs when a value entering the transaction context is modified
;; by another external concurrently running operation trying to modify the same `ref`s.
;; In fact, as values enter the context of a transaction, their values are stored within the context of that transaction
;; and will be used as a `checkpoint` by comparing the `ref`s stored values with their most up-to-date values, when the
;; transaction (at the end) is about to commit the newly processed values for the involved `ref`s . If at some point
;; any of the `ref`s values stored within the transaction is not equal to its value in the running process, because some
;; other external concurrent running operation may have updated it in the context of another transaction, then a `retry`
;; is triggered. A `retry` consists in re-running the aborted transaction but by taking into account the new externally-modified
;; `ref`s values.

;; Let's take an example from the shopping scenario we had so far. Given a list of items to be shopped we divide our shopping
;; list among children and then send them out to collect things. Of course, this assumes that all of our children have the same
;; attention span and will do their best to find the items on their lists, not get distracted, and not bring anything else back.
;; If all of this is true, then we can expect things to go swimmingly. In reality, things are a little bit different. When they
;; return, they put their assigned item—and maybe some candy—in the cart and receive their next assignment. Our more focused
;; gophers will do more work, but the items get crossed off the list eventually.
;;
;;
;; The key to making the shopping work is then to have clear rules and being able to enforce them like with the following :
;;  • An item in the shopping list gets crossed off when it’s assigned to a child.
;;  • An item remains assigned to a child until it’s placed in the cart.
;;  • Candy isn’t allowed in the shopping list or in the cart.
;; These rules also state (by deduction) that the 'cart' and 'shopping list' have to be updated together in the same context
;; or not at all when an item is recovered (or not). Some transactionality has the to be introduced when modifying these
;; two references which means we need to use `ref`s to represent these elements.

;; First of all, let's define the needed references for our scenario :
(def shopping-list (ref #{}))                               ;; we use a set to ensure uniqueness of shopping items
(def shopping-cart (ref #{}))
(def assigned-items (ref #{}))

(def kids #{:alice :bobby :cindy :donnie})

;; We first state that every kid is available to receive an assignment
(def availability-map (ref (into {} (map (juxt identity (constantly true)) kids))))
;; @availability-map
;=> {:alice true, :bobby true, :cindy true, :donnie true}

(defn available-kids
  "Returns a set of currently available kids"
  []
  (reduce-kv #(if %3 (conj % %2) %) [] @availability-map))

(defn next-kid
  "Returns the next kid available to grab an item"
  []
  (loop
    [av (available-kids)]
    (if (not-empty av)
      (rand-nth (available-kids))
      (do (Thread/sleep 500)
          (recur (available-kids))))))

(defn buy-candy
  "Grabs a candy and put it in the shopping cart"
  []
  (dosync
    ;; no need to use `alter` here since we only have one operation and hence no operation
    ;; ordering involved
    (commute shopping-cart conj (rand-nth #{"candy bar" "gum drops" "toy"}))))

;; Let's also init the store shelves with some items
(defn init-store
  "Init store"
  []
  (init {:eggs   2 :bacon 3 :apples 3
         :candy  5 :soda 2 :milk 1
         :bread  3 :carrots 1 :potatoes 1
         :cheese 3})

  ;; Modifying `ref`s involves changing them in the context of a transaction. The `dosync` macro introduces and marks
  ;; the boundaries of such transaction. Every instructions contained within the `dosync` macro are then executed atomically
  ;; in an all-or-nothing fashion.
  (dosync
    (ref-set shopping-list #{:milk :butter :bacon :eggs
                             :carrots :potatoes :cheese :apples})
    (ref-set shopping-cart #{})
    (ref-set assigned-items #{})))

;; There are 3 ways (and their matching-function) to update a reference value within the context of a transaction :
;;  * `ref-set` is used to directly replace the value of a `ref`. Its main use is for initialization.
;;  * `alter` is used when an `update` function is provided to process the new `ref` value
;;  * `commute` is used like `alter` except that it does not require the operations to be applied in the same order as they
;; are declared within the context of the transaction i.e when the order of operations does not matter since they are 'commutative'.

;; Let's for example illustrate the action of assigning an item to be grabbed to a child
(defn assign-item-to-child
  "Assigns an item on the shopping list to a child"
  [child item]
  (dosync
    ;; we first tag the child as not available since he/she has been assigned the task of
    ;; grabbing a new item
    (alter availability-map update-in [child] (fn [_] false))
    ;; we then add the item to the list of already-assigned items
    (alter assigned-items conj item)))

(defn maybe? [f] (if (= 0 (rand-int 3)) (f)))

(defn dawdle
  "screw around, get lost, maybe buy candy"
  []
  (Thread/sleep (rand-int 10000))
  (maybe? buy-candy))

(defn send-kid
  "Sends a kid for an item"
  [kid item]
  (assign-item-to-child kid item))

;; Recovering an item from a child works in a very similar way
(defn recover-item-from-child
  "Recovers item, tags child as available and puts the item in the shopping cart"
  [item child]
  (dosync
    ;; When recovering, we first add the item to the shopping cart
    (alter shopping-cart conj item)
    ;; we then tag the child as available for a new item grabbing
    (alter availability-map update-in [child] (fn [_] true))
    ;; and last we remove the recovered item from the assigned item
    (alter assigned-items disj item)))
