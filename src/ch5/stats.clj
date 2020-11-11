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
