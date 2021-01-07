(ns ch6.components-impl)

;; So far, we have seen how to call components via their api and how we can connect
;; them together. It is now time to see how to implement the functionality behind those
;; apis.
;; While implementing components, we have to consider two things :
;; - that the component has some kind of `internal state` that callers are allowed to alter
;; and change via the exposed fns on the component's api. The component also ensures
;; the proper update of those states. We have seen previously notions related to concurrent
;; access through Software transaction memory and the usage of atoms, refs etc. in ensuring
;; that concurrent thread accessing those shared state do not accidentally corrupt them.

;; - that the component has a `lifecycle` : it can be created and provided with 
;; externally-defined `configuration data` and `dependencies` allowing it to be dynamic and
;; thus reusable in different settings by initializing internal states or connections to
;; external systems. It can also be started and stopped which is an important characteristic
;; for REPL-driven development.


;; GRANULARITY OF STATE
;; Holding the value of the component's internal state requires the use of `stateful` containers
;; as seen with STM. We then have to decide on the granularity of the state.
;; - We can opt to use `refs` if we have some `coordination` to handle i.e if two or more state
;; have to be updated in the same transaction.
;; - We can use `atom` if there are no coordination involved.

;; Let's consider for example an index of the terminator models and an index of missions that
;; we have to manage together.

(defrecord TerminatorMissions [models missions])

(comment

  ;; we can have the following component creation function
  (defn make-terminator-missions
    [models missions]
    (map->TerminatorMissions {:models (atom [])
                              :missions (atom [])}))

  ;; those are independent states that need to change separately. There is no coordination
  ;; involved...
  )


;; if on the other side the two states have to be modified atomically, we would implement it
;; like the following

(comment

  (defn make-terminator-missions
    [models missions]
    (map->TerminatorMissions {:models (ref {})
                              :missions (ref {})}))

  ;; the only issue with refs is that we would have to wrap all our interactions with the
  ;; the internal states within a transaction (dosync ...)
  )

;; an elegant solution would be to increase the granularity of the state container to 
;; encompass both :models and :missions

(comment

  (defn make-terminator-missions
    [models missions]
    (map->TerminatorMissions (atom {:models {}
                                    :missions {}})))

  ;; we would then have only one global state container for the component which is
  ;; much simpler to manage. Coarser-grained state use is often possible assuming that
  ;; the update functions are small.
  )

