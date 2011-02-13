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

(defn ^{:private true} changed-status [tasks task status] (map #(if (= (:id task) (:id %)) (update-in % [:status] (fn[_] status)) %) tasks))
(def ^{:pritave true} step {:scheduled :waiting, :running :completed, :waiting :running})
(defn ^{:private true} step-states [tasks] (doall (map #(if (contains? step (:status %)) (update-in % [:status] (fn[x] (x step))) %) tasks)))

(deftype FakeJscfi [observer tasks] Jscfi
    (get-tasks [this] tasks)
    (get-task [this id] (loop [t tasks] (if (= (:task-id (first t)) id) (first t) (if (empty? t) nil (recur (next t) )))))
    (register-task [this task] (new FakeJscfi observer (conj tasks (update-in task [:id] (fn[_] (.toString (rand)))))))

    (compile-task  [this task] (new FakeJscfi observer (changed-status tasks task :compiled)))
    (upload-task   [this task] (new FakeJscfi observer (changed-status tasks task :uploaded))) 
    (schedule-task [this task] (new FakeJscfi observer (changed-status tasks task :sheduled))) 
    (cancel-task   [this task] (new FakeJscfi observer (changed-status tasks task :cancelled))) 
    (suspend-task  [this task] (new FakeJscfi observer (changed-status tasks task :suspended))) 
    (resume-task   [this task] (new FakeJscfi observer (changed-status tasks task :scheduled))) 
    (download-task [this task] (new FakeJscfi observer (changed-status tasks task :downloaded))) 
    (clear-task    [this task] (new FakeJscfi observer (changed-status tasks task :cleared))) 

    (periodic-update [this] (new FakeJscfi observer (step-states tasks)))

 )

(def ^{:dynamic true} *jscfi-fake* (future (reify JscfiFactory
    (connect [this observer address username]
        (prn "Connecting to " username "@" address)
	(if (and (= address "scfi") (= username "test"))
	 (let [jscfi (new FakeJscfi observer [
	    {:name "Qqq" :source-file "qqq.c" :input-file "input.txt" :output-file "output.txt"
		:id 0.3232 :task-id "server-33" :node-count 4 :status :waiting}
	    {:name "Www" :source-file "www.c" :input-file "input.txt" :output-file "output.txt"
		:id 0.2342 :task-id "server-34" :node-count 1 :status :running}
	    ])]
	  (when (not= (get-password observer) "passwd") (throw (RuntimeException. "Invalid password")))
	  jscfi)
	 nil)))))

(defn get-fake-jscfi-factory [] @*jscfi-fake*)
