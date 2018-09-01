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