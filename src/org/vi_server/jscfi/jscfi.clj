(ns org.vi-server.jscfi.jscfi "Jscfi facade between engine and GUI")

;; task is a map like  {:name "Qqq", :source-file "qqq.c", :input-file "input.txt", :output-file "output.txt", :pbs-id "server-33" :node-count 4, :status :waiting, :id "0.3324234"} 
;;
;; States: :none :compiled :uploaded :error :deletion :hold :running :restarted :suspended :transferring :threshold :waiting :finished :downloaded :cleaned

(defprotocol JscfiObserver
    "Callbacks from Jscfi object"
    (compilation-failed [this task text])
    (connected [this]) ; dup from auth-observer's auth-succeed
    (something-changed [this])
)

(defprotocol AuthObserver
    "Authentication-related callbacks from Jscfi object"
    (^String get-keyfile [this])
    (^String get-password [this])
    (^String get-hostsfile [this])
    (auth-succeed [this])
    (auth-failed [this])
    (connection-stage [this msg])
)

;; The object is stateful and expected to use agents inside.
(defprotocol Jscfi
    "Connection to SCFI"
    (^clojure.lang.PersistentArrayMap get-task [this task-id])
    (^clojure.lang.PersistentVector get-tasks [this])
    
    (connect [this auth-observer address user directory])
    (add-observer [this observer])
    (remove-observer [this observer])
    
    ;; The task lifecycle:
    ;; 1. The task is created (and may be registered in Jscfi): register-task
    ;; 2. Source code is uploaded to SCFI and gets compiled: compile-task
    ;; 3. Input data is uploaded to SCFI: upload-task
    ;; 4. The task is scheduled for execution: schedule-task
    ;; 	      It can be suspended or canceled
    ;; 5. The task runs.
    ;; 6. The output file is downloaded from SCFI: download-task
    ;; 7. Source code, executable, input and output files and other things are cleared from SCFI: clear-task

    (^String register-task [this task])
    (^String alter-task [this task])
    (remove-task [this task-id])
    (compile-task [this task-id])
    (upload-task [this task-id])
    (schedule-task [this task-id])
    (cancel-task [this task-id])
    (suspend-task [this task-id])
    (resume-task [this task-id])
    (download-task [this task-id])
    (purge-task [this task-id])

    ;; Update information about tasks
    (periodic-update [this])
    
    (debug-print [this])
)

