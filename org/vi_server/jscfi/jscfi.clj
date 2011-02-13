(ns org.vi-server.jscfi.jscfi "Jscfi facade between engine and GUI")

;; task is a map like  {:name "Qqq" :source-file "qqq.c" :input-file "input.txt" :output-file "output.txt" :task-id "server-33" :node-count 4 :status :waiting :id 0.3324234} 
;;
;; States: :none :compiled :uploaded :error :deletion :hold :running :restarted :suspended :transferring :threshold :waiting :finished :downloaded :cleaned

(defprotocol JscfiObserver
    "Callbacks from Jscfi object"
    (connection-status-changed [this])
    (task-status-changed [this task])
    (compilation-failed [this task text])
    (^String get-keyfile [this])
    (^String get-password [this])
)

(defprotocol Jscfi
    "Connection to SCFI"
    (^clojure.lang.PersistentArrayMap get-task [this id])
    (^clojure.lang.PersistentVector get-tasks [this])
    
    ;; The task lifecycle:
    ;; 1. The task is created (and may be registered in Jscfi): register-task
    ;; 2. Source code is uploaded to SCFI and gets compiled: compile-task
    ;; 3. Input data is uploaded to SCFI: upload-task
    ;; 4. The task is scheduled for execution: schedule-task
    ;; 	      It can be suspended or canceled
    ;; 5. The task runs.
    ;; 6. The output file is downloaded from SCFI: download-task
    ;; 7. Source code, executable, input and output files and other things are cleared from SCFI: clear-task

    (^Jscfi register-task [this task])
    (^Jscfi compile-task [this task])
    (^Jscfi upload-task [this task])
    (^Jscfi schedule-task [this task])
    (^Jscfi cancel-task [this task])
    (^Jscfi suspend-task [this task])
    (^Jscfi resume-task [this task])
    (^Jscfi download-task [this task])
    (^Jscfi clear-task [this task])

    ;; Update information about tasks
    (^Jscfi periodic-update [this])
)

(defprotocol JscfiFactory
    (connect [this observer address user])
)

