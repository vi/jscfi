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

(defmacro ^{:private true} fj-method [name new-tasks] 
 `(~name  [this task] (new FakeJscfi observer new-tasks connected)))

(deftype FakeJscfi [observer tasks connected] Jscfi
    (get-tasks [this] tasks)
    (get-task [this id] (loop [t tasks] (if (= (:task-id (first t)) id) (first t) (if (empty? t) nil (recur (next t) )))))
    (register-task [this task] (new FakeJscfi observer (conj tasks (update-in task [:id] (fn[_] (.toString (rand))))) connected))

    (compile-task  [this task] (new FakeJscfi observer (changed-status tasks task :compiled) connected))
    (upload-task   [this task] (new FakeJscfi observer (changed-status tasks task :uploaded) connected)) 
    (schedule-task [this task] (new FakeJscfi observer (changed-status tasks task :sheduled) connected)) 
    (cancel-task   [this task] (new FakeJscfi observer (changed-status tasks task :cancelled) connected)) 
    (suspend-task  [this task] (new FakeJscfi observer (changed-status tasks task :suspended) connected)) 
    (resume-task   [this task] (new FakeJscfi observer (changed-status tasks task :scheduled) connected)) 
    (download-task [this task] (new FakeJscfi observer (changed-status tasks task :downloaded) connected)) 
    (clear-task    [this task] (new FakeJscfi observer (changed-status tasks task :cleared) connected)) 

    (periodic-update [this] (new FakeJscfi observer (step-states tasks) connected))

    (connect [this observer address username]
        (prn "Connecting to " username "@" address)
	(if (and (= address "scfi") (= username "test"))
	 (let [jscfi (new FakeJscfi observer tasks true)]    
	  (when (not connected) (when (not= (get-password observer) "passwd") (throw (RuntimeException. "Invalid password"))))
	  jscfi)
	 nil))
)

(defn get-fake-jscfi [] (new FakeJscfi nil [
   {:name "Qqq" :source-file "qqq.c" :input-file "input.txt" :output-file "output.txt"
	:id 0.3232 :task-id "server-33" :node-count 4 :status :waiting}
   {:name "Www" :source-file "www.c" :input-file "input.txt" :output-file "output.txt"
	:id 0.2342 :task-id "server-34" :node-count 1 :status :running}
   ] false))
