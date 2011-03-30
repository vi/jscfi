(ns org.vi-server.jscfi.fake
    "Fake Jscfi for testing GUI without server"
    (:use org.vi-server.jscfi.jscfi))      

;; If you want to test fake-jscfi from console, you should go to directory where org/vi_server/jscfi/fake.clj does exist and execute:
;; 1. Run "java -cp clojure.jar:. clojure.main"
;; 2. Execute (without "#_")
#_ (ns qqq 
    (:use org.vi-server.jscfi.fake :reload-all)
    (:use org.vi-server.jscfi.jscfi))
#_ (def j (connect (get-fake-jscfi-factory)
	    (reify JscfiObserver (get-password [this] "passwd")) "scfi" "test")) 
#_ (def t (get-task  j "server-33"))
;; 3. Play with Jscfi object "j" and task example task "t"

(defn ^{:private true} changed-status [tasks task-id status] (map #(if (= task-id (:id %)) (update-in % [:status] (fn[_] status)) %) tasks))
(def ^{:pritave true} step {:scheduled :waiting, :running :completed, :waiting :running})
(defn ^{:private true} step-states [tasks] (doall (map #(if (contains? step (:status %)) (update-in % [:status] (fn[x] (x step))) %) tasks)))

(defn ^{:private true} fj-method [name add-args new-tasks] 
 `(~name  [~'this ~@add-args] 
     (send ~'state-agent 
      (fn[~'state] ;; does not need to be hygienic
       (let[
	~'tasks (:tasks ~'state)
	~'observer (:observer ~'state)
	~'auth-observer (:auth-observer ~'state)
	~'connected (:connected ~'state)
	] 
	(assoc ~'state :tasks ~new-tasks))))
     nil))
(defmacro ^{:private true} deftype-my [methods-vector & rest]
 `(deftype ~@rest ~@(map #(apply fj-method %) methods-vector)))

(deftype-my [
    [compile-task   (task-id) (changed-status tasks task-id :compiled)]
    [upload-task    (task-id) (changed-status tasks task-id :uploaded)]
    [schedule-task  (task-id) (changed-status tasks task-id :scheduled)]
    [cancel-task    (task-id) (changed-status tasks task-id :cancelled)]
    [suspend-task   (task-id) (changed-status tasks task-id :suspended)]
    [resume-task    (task-id) (changed-status tasks task-id :resumed)]
    [download-task  (task-id) (changed-status tasks task-id :downloaded)]
    [clear-task     (task-id) (changed-status tasks task-id :cleared)]

    [register-task (task) (conj tasks (update-in task [:id] (fn[_] (.toString (rand)))))]

    [periodic-update () (step-states tasks)]

    ] FakeJscfi [state-agent] Jscfi
    (get-tasks [this] (:tasks @state-agent))
    (get-task [this id] (loop [t (get-tasks this)] (if (= (:id (first t)) id) (first t) (if (empty? t) nil (recur (next t) )))))
    
    (connect [this auth-observer address username]
        (printf "Connecting to %s@%s\n" username address)
	(send state-agent (fn[state]
	   (if (and (= address "scfi") (= username "test"))
	     (if (not= (get-password auth-observer) "passwd") 
	      (do 
	       (auth-failed auth-observer)
	       state)
	      (do
	       (auth-succeed auth-observer)
	       (connected (:observer state))
	       (-> state (assoc :connected true) (assoc :auth-observer auth-observer))))
	    state))))
    (set-observer [this observer]
     (send state-agent (fn[state]
	     (-> state (assoc :observer observer)))))     
)

(defn get-fake-jscfi [] (new FakeJscfi (agent {:observer nil, :auth-observer nil, :tasks [
   {:name "Qqq" :source-file "qqq.c" :input-file "input.txt" :output-file "output.txt"
	:id 0.3232 :outer-id "server-33" :node-count 4 :status :waiting}
   {:name "Www" :source-file "www.c" :input-file "input.txt" :output-file "output.txt"
	:id 0.2342 :outer-id "server-34" :node-count 1 :status :running}
   ], :connected false})))
