(ns ch4.single)

;; Now that we have everything ready under our belt to define entities, collections and functions
;; for manipulating them, we now have to think about how all these things relate to each other
;; during the application execution especially in regard to multiple execution threads accessing
;; and updating them.

;; MODELING CHANGE
;; first of all, it is important to see how `change` is approached in two (2) of the mostly used/known
;; programming paradigm today :
;;   - Object-oriented programming a.k.a OOP
;;   - Functional programming a.k.a FP (which Clojure is part of)

;; in OOP, we are used to approaching change in terms of `mutability`. The data structures involved
;; are themselves `mutable` meaning they can be `modified` and incidentally we tend to apply changes
;; to entities `in place` i.e directly because well, we can. This approach although simple, becomes
;; radically complex when multiple execution threads are involved. In fact, managing changes in
;; this case is about coordinating changes(update), access(read) to these shared data among the
;; execution threads.
;; Unfortunately, the tools available today on the platform to deal with these change management
;; are rather low-level and their use often error-prone, think use of `locks` among other things.
;; It often leads to a lot of ceremony and complexity for programmers especially when considering
;; that these are `not` problems related to the domain but rather code infrastructure which should
;; be handled by the paradigm/platform/framework allowing the developer or programmer to focus on
;; solving the real domain problem at hand.
;; Another consequence is that managing changes this way is a `destructive` process in that we only
;; retrieve the result of the update function which is the most up-to-date value of the data structure
;; as of `now`. There is no way of knowing the value or set of values that may have been assigned to
;; the data structure prior to the current one.



;; On the other hand, in FP unlike in OOP, the approach lean towards `immutability` where data structures
;; are `immutable` and functions for manipulating/changing them creates new data structures when applying
;; those functions instead of changing the data in place. Even though this approach may be counter-intuitive,
;; since we may be confused into thinking that it involves a lot of copying, it is actually quite the opposite.
;; In fact in FP, creating new data from the ones to be modified make use of a process called `structural sharing`
;; or `data sharing`.
;; In this approach, instead of copying the whole data to be modified, we reuse the existing data structure
;; and only add/remove the relevant `parts` which are the resultant of applying the modifying function, with
;; the use of `references` pointing at each other to combine and form new data structures.
;; There is no real removal or adding per se, it only consists in creating `references` pointing at different
;; locations in the involved data structures :

;; let's consider a linked-list for example. When creating the list, we are provided with a new `reference` pointing
;; to the `head` of the list, let's call it (X). If we want the removal of this linked list head element for
;; example, we create a `totally new` reference (Y) which, instead of a real removal of the linked list head, will
;; just point to the element of the linked-list pointed to by (X), right after the `head`. The original
;; linked-listed pointed to by (X) is still available and it shares a subpart of its structure with the new
;; linked-listed pointed to by (Y).

;; As a consequence, entities supported and implemented with `immutable` data structures act as containers
;; of a series of values corresponding to the different `states` (represented by the `references`) the data
;; structure has been through after update function has been applied one more multiple times. A set of discrete
;; values, a series of snapshots as if it were its `history` allowing to go back at a certain point in time
;; to a particular `state`, at will. Hence this `non-destructive` approach eases the process of changing state
;; because of the `immutability` nature of the data structure involved.



;; ID AND STATE
;; Clojure being a functional programming language embracing `immutability` as we have seen earlier, it is important
;; to talk about `identity` and `state` and how they relate to each other to get a grasp of how it approaches
;; and handles state management.
;;
;; an `identity` is a serie of `states` separated in time. It is represented by a `reference`, which is a mutable
;; container containing a succession of immutable values (successive `states`)
;;
;; a `state` is a snapshot of an `identity's` value and it represents its value at that particular time. There is
;; a notion of temporality involved here.


;; UNIFIED UPDATE MODEL
;; Clojure's approach to modelling and managing `values that change` is opinionated and to some extent, is really simple in
;; essence. In fact, it relies on a concept called the `unified update model` which is a generic `form pattern`
;; involving the `reference` which represents the `identity` as we have seen, a function for updating the identity's
;; state called the `data function` and `arguments` used by the data function in conjunction with the reference's
;; current value to compute the reference's new value (state).
;; Depending on the problem at hand, the `reference` type may vary, but the syntax and approach to handling the update
;; remains the same and always follows the same pattern.


;; (update-fn container-ref data-fn &args)


;; `update-fn` triggers the update process and is solely determined by the type of `reference` (atom, ref, var, agent)
;; whose value is to be updated.
;; `container-ref` is the reference name which represents the `identity` whose value is to be updated
;; `data-fn` is a function allowing the processing of the value to be set as the new value of the `identity`. It takes
;; the current value of the `container-ref` as its first argument and eventually additional arguments provided to the
;; `update-fn` call in its `args` argument.
;; `args` (optional) are arguments provided to the `data-fn` in addition to the current value of the `container-ref` to
;; process the value to be set as its new value.


;; There are two (2) dimensions involved when choosing the `right` reference type for the unified update model
;;  - a notion of `scope` defining to which extent the change has to be applied also referred to as COORDINATION
;;  - a notion of `temporality` and `context` defining when and in which context the change has to be applied also referred to
;; as SYNCHRONIZATION


;; There are (2) scopes : atomic and transactional

;; ATOMIC SCOPE
;; It is used when a single `standalone` value is changed `independently` of all other stateful references within the system.
;; It then does not require `coordination` because of this independence. We can also qualify it as an UNCOORDINATED operation.
;; The way it works is that given an identity and an update function, the latter is applied to the current value of the
;; former, eventually with arguments (seen earlier) to calculate the new value of the identity. If everything goes well,
;; this newly processed value will replace the current identity value.
;; Problem occur though when multiple threads try to update the same identity in the same time. The mechanism then reorders
;; the updates so that only one at a time occurs. That is, if a thread is on the verge of committing the identity newly
;; created value but a second thread has updated it in the same time, then the first thread has to renew/retry the overall
;; process of calculating the identity new value but this time taking into account the current value of the identity as
;; the newly committed value set by the second thread.


;; TRANSACTIONAL SCOPE
;; It is used when multiple identities or stateful references have to change their values `together`. These multiple changes
;; have to happen altogether or not at all, hence the `transactional` qualification reminding us the terminology used in
;; RDBMS. Because of this `interdependence`, there needs to be a `coordination` when changing the stateful references values.
;; That is why we can also qualify it as a COORDINATED operation.
;; In Clojure, this coordination is supported by an `emulation` of the very same techniques encountered in database management
;; systems when dealing with `updates` via `transactions`, to ensure all changes happen altogether or not at all.
;; Clojure's flavor is called `Software Transactional Memory` and it has almost the same ACI(D) properties except for the
;; `Durability` property which is not relevant since everything is done `in memory` as the name STM suggests.
;; The way it works is that when the transaction starts, it takes the `initial value` of the identities involved and considers
;; these as their `real` value (i.e system-wide) within the scope of the transaction. As the transaction progresses, it
;; executes the instructions to process the new values of the identities and in the same time it keeps track of their
;; `original value`, when they entered the transaction.
;; When the transaction reaches its `end`, it has the choice of `committing` the new values it has calculated to be the new
;; values of the identities and make them available system-wide (among all threads within the system).
;; It then compares the identities `original` values that it kept tracking during the course of the transaction, with the
;; current value of these same identities outside the transaction i.e system-wide. If they are still `the same`, it means that
;; no other threads have updated them and the transaction can then `commit` or `write` the values it calculated within its scope
;; as the new values of the identities.
;; Otherwise, if the original values of the identities as they entered the transaction is not equal to their `real` values outside the
;; scope of the transaction at the end of it when it tries to commit the new values, then it probably means that some other
;; threads within the system may have already committed some other values to these identities. The transaction then has to
;; restart but this time, it takes the values of the identities `updated outside` its scope as the initial value of these
;; identities and whose values conditions the `committing` or not of the new calculated values as the new values of the identity
;; system-wide.


;; There are (2) kinds of `temporality/context` notions : Synchronous vs Asynchronous

;; SYNCHRONOUS
;; These operations are used when the caller's thread of execution has to block/wait for an exclusive access to a context in order
;; to perform the operation.

;; ASYNCHRONOUS
;; Asynchronous operations, instead of blocking/waiting for an exclusive access to a context, spins off another thread to get
;; a different context of its own for the execution. It does not block the caller's thread.




;; TOOLS FOR MANAGING CHANGES a.k.a Reference types
;; The two (2) concepts `coordination` and `synchronization` and their `duals` let us define exactly the type of concurrent operations
;; that we would like to achieve, hence the `reference type` to be used as well. Considering the duals then, we have 3 of the 4 kinds
;; of `reference type` :
;;   - Coordinated and Synchronous : REFS reference type
;;   - Coordinated and Asynchronous : this one does not exist in Clojure since it is only interested in addressing in-memory concurrency
;; whereas `Coordinated and asynchronous` semantics is more common in distributed systems where changes are only guaranteed to merge
;; into a unified model over time and potentially spanning over multiple systems beyond the local in-process system.
;;   - Uncoordinated and Synchronous : ATOM reference type
;;   - Uncoordinated and Asynchronous : AGENT reference type

;; There is a fourth reference type that is different from the (3) we have seen so far : `Var`s.
;; `Var` differs from the other reference types in that their state is not managed in time. Rather, they provide a namespace-scoped identity
;; that can be `rebound` to have a different value on a `per-thread` basis.


;; Manipulating `Reference type`s
;; There are 3 main operations for manipulating reference type values/state and for each, a corresponding function. Note that, as seen with
;; the Unified Update Model, these function calls have a `consistent` syntax and follow the same `pattern` :
;;  - Identity Initialization with the correct `reference type` via a `creation function`
;;    Pattern : (create-fn container)
;;  - Identity value update with an `update function` as seen with the `Unified Update Model` seen earlier
;;    Pattern : (update-fn container data-fn & args)
;;  - Identity value reset with a `set function`
;;    Pattern : (set-fn container new-val)

;; `Reference type`   |   `create-fn`   |   `update-fn`           |    `set-fn`             |
;; ==========================================================================================
;;  Atom              |   (atom ...)    |   (swap! ...)           |    (reset! ...)         |
;;  Ref               |   (ref ...)     |   (alter ...)           |    (ref-set ...)        |
;;                    |                 |   (commute ...)         |                         |
;;  Var               |   (def ...)     |   (alter-var-root ...)  |    (var-set ...)        |
;;  Agent             |   (agent ...)   |   (send ...)            |    (restart-agent ...)  |
;;                    |                 |   (send-off ...)        |                         |


;; In the end, the choice for a concurrent implementation consists in reflecting and deciding what information to manage and what information
;; to leave unmanaged.




;; MANAGING UPDATES WITH ATOM
;; Let's walk through an example : a grocery store.
;; In its simplest form, shopping only consists in having a list of items to be bought and a person (a thread) going to the store to get the items.

(defn go-shopping-naive
  "Returns a cart containing the list of bought items"
  [shopping-list]
  (loop [[item & items] shopping-list
         cart []]
    (if item
      (recur items (conj cart item))
      cart)))

;; This scenario does not involve state management because we only have one `thread` (person) interacting with the program. Furthermore, in the example,
;; the items rest on a infinite shelf and the program only consists in moving items from one list to another.
;; In a more elaborate version though (and in the real world), we would probably have a kind of `store inventory` containing the store `available` items.