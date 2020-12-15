(ns ch5.stats)

;; Nowadays, computers and all their incantations (phones, tablets etc.) are built
;; with multicore chips with multiple independent threads of control available
;; for programs to use.
;; Programs though spend most of their time waiting for I/O (files, IO streams,
;; external network etc.). We then have to find a way to optimize this
;; waiting time so that those programs can handle other tasks in parallel
;; while waiting for their IO to complete.

;; One of the main problems we have to solve is for example how to move work
;; off the main thread so that this latter goes on with its task without having
;; to block while waiting for the task to finish.

;; For longer-lived tasks, we can take advantage of the plethora of options offered
;; by the JVM for farming them out to a pool of worker threads.



;; PUSH WAITING TO THE BACKGROUND

;; FIRE AND FORGET
;; As stated earlier, most programs spend a substantial amount of time waiting for
;; I/O. We then need a way to optimize this awaiting time by allowing the program
;; to do something else in the meantime.
;; That is exactly what `future`s are for in Clojure. When launching a `future`, we
;; delegate the execution of a task to another thread in a fire-and-forget fashion.
;; The thread launching the `future` then off-loads its task to a background thread-pool
;; managed by Clojure, allowing the former to not be blocked (and wait) and proceed
;; with its execution.

(future
  (Thread/sleep 3000)
  (println "If My Calculations Are Correct, When This Baby Hits 88 Miles Per Hour, You're Gonna See Some Serious S***"))

;; Instead of passing-in a body, we can also opt to use the `future-call` fn which takes
;; a function with no arguments which will be invoked on another thread.
(defn back-to-the-future
  []
  (do
    (Thread/sleep 5000)
    (println "Wait a minute, Doc!! Are you telling me you built a time machine")))

(future-call back-to-the-future)

;; both `future` and `future-call` return java.lang.Future which can be used to control
;; and inspect the asynchronous activity.
;; `future-cancel` for example tries to cancel a `future` if posssible while `future-done?`
;; and `future-cancelled?` check the state of the future whether it has completed its task
;; or has been cancelled respectively
(defn terminate-john-connor
  []
  (do
    (println "Hasta la vista baby")))

(let [f (future-call terminate-john-connor)]
  (when (not (future-done? f))
    (future-cancel f)))



;; ASYNCHRONOUS AND STATEFUL
;; So far, we have mainly talked about 3 kinds of state containers in Clojure that are
;; updated using the Unified Update Model :
;; - vars
;; - atoms
;; - refs
;; But there is a fourth one that we have omitted so far : `agent`s.

;; While an atom is a state container which is to be updated atomically and synchronously
;; and that refs are used when there needs to be coordination regarding the synchronous
;; update of multiple state containers, `agent`s on the other hand allow single shared state
;; `asynchronous` update.
;; Modifying the state of an agent is done via an `action function` which is merely a
;; function that is applied to the current state of the agent with some optional arguments
;; giving the new state of the agent. We retrieve here the same pattern that we saw with
;; the Unified Update Model. The application of this function is done `asynchronously` and
;; ordering the application of the function is called a `dispatch`.

;; There are two ways (functions) for dispatching an action :
;; - send : used for CPU-limited actions (non blocking). The underlying thread-pool uses
;; a fixed number of threads and thus rely on the completion of the actions in a timely
;; manner
;; - send-off : used for potentially-blocking actions (network call, I/O etc.). The 
;; underlying thread-pool used here will grow on demand, so potentially blocking operations
;; is not an issue.

;; Let's take the example of a terminator to which we would like to send kill orders.
;; Instead of going back and forth in the time machine (because it is expensive as sh**)
;; let's say that the terminator will only start terminating its target when it has
;; 3 people from the resistance in its kill-list.

;; we model the terminator as an agent (kinda makes sense here)
(def terminator (agent #{}))

;; in order for it to proceed to its doings, we need a way to check whether the kill-list
;; has at least 3 person from the resistance. Let's add a watch function that would allow to
;; check that fact. A little reminder, a watch-fn associated with a state container is called
;; each time the value of that state container changes.

(defn put-terminator-in-time-machine
  "This is just a helper function for fun"
  [kill-list]
  (let [target-list (clojure.string/join ", " (map :surname kill-list))]
    (println "I have to terminate : " target-list)
    (println "I'll be back!!!")))

(add-watch terminator :kill-list-count
           (fn [_ _ _ new-value]
             (if (= 3 (count new-value))
               (put-terminator-in-time-machine new-value))))

;; now that we have a way to trigger the sending of our terminator, we now can focus on
;; providing it with a list of target to be terminated.
(send-off terminator conj {:name "Connor" :surname "John"})

;; at this point, we have inserted John as the first target to be terminated. To make sure
;; that the order has been ack'ed by our killer robot, we just use the same old trick as for
;; the other reference types to check its content, which is deref-ing the agent.
@terminator
;; > #{{:name "Connor", :surname "John"}} and we indeed have John in the kill-list

;; let's add two more targets to the list :
(send-off terminator conj {:name "Reeves" :surname "Kyle"})
(send-off terminator conj {:name "Connor" :surname "Sarah"})

;; as soon as our kill-list have 3 targets in it, the watch that we have registered earlier
;; will trigger the sending of our killer robot ...
;; and indeed from the repl output we have :
;; > I have to terminate :  Sarah, Kyle, John
;; > I'll be back!!!

;; To sum up on how `agent`s work. When a `dispatch` is made via `send` or `send-off` :
;; - the call returns immediately
;; - at some point later, in another thread, the new value of the agent is computed by
;; applying the `action function` with the current state of the agent and any eventual
;; arguments passed to it via the `dispatch`
;; - if a `validator` function has been attached to the agent, it is triggered and
;; provided with the newly processed value. If the new value passed validation, it
;; is committed and becomes the new value of the agent
;; - at this point, if one or more `watch` functions has been attached to the agent
;; they are also triggered as soon as the new value is committed.

;; Finally, some details worth mentionning regarding errors and exceptions in agents:
;; when an exception or an error occurs on a agent, any subsequent interactions with
;; this agent will throw an exception. It is then necessary to restart the agent with
;; the `restart-agent` function.
