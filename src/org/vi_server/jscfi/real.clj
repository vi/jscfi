(ns org.vi-server.jscfi.real
 "Real Jscfi that interacts with with SSH"
 (:use clojure.walk)
 (:use [clojure.set :only [difference intersection]])
 (:use org.vi-server.jscfi.jscfi)
 (:use [clojure.contrib.string :only [split join upper-case lower-case trim blank?]])
 (:use [clojure.contrib.str-utils :only [chomp]])
 ;(:require [org.danlarkin.json :as json])
 ;(:require [clj-yaml.core :as yaml])
 (:import (com.jcraft.jsch JSch Channel Session UserInfo UIKeyboardInteractive ChannelSftp SftpException))
 (:import (java.io.ByteArrayInputStream))
 )  

(defn read-script [script-name & args] 
 (apply format (slurp (ClassLoader/getSystemResourceAsStream 
	 (str "org/vi_server/jscfi/scripts/" script-name))) args))

(defn serialise [object]
 (binding [*print-dup* true] (with-out-str (prn object))))

(defn deserialise [string] (read-string string))

(defn ^String ssh-execute [session command input-str]
 (println "ssh-execute" command)
 (try
  (let [
   channel (.openChannel session "exec")
   output (java.io.ByteArrayOutputStream.)
   input (if input-str (java.io.ByteArrayInputStream. (.getBytes input-str)) nil)
   ]
   (println "ssh-execute stage 2")
   (doto channel
    (.setOutputStream output)
    (.setExtOutputStream System/err)
    (.setInputStream input)
    (.setCommand command)
    (.connect 3000))
   (println "ssh-execute progress")
   (while (not (.isClosed channel)) (Thread/sleep 100))
   (println "ssh-execute finished")
   (str output))
  (catch Exception e (.printStackTrace e) (println "ssh-execute fail" e) nil)))

(defn interpret-tasks [^String source] 
 (if (empty? source) {}
  (deserialise source)))

(defn emit-impl[state-agent closure]
 (send state-agent (fn[state]
  (let [observers (:observers state)] 
   (doall (map (fn[observer]
    (try
	(closure observer)	
    (catch Exception e (println "Observe exception: " e)))) observers))) state)))
(defmacro emit [mname & whatever]
 `(emit-impl ~'state-agent (fn[~'observer11] (~mname ~'observer11 ~@whatever))))

(defn persist-tasks [session state-agent tasks]
    (ssh-execute session (read-script "save-tasks.txt" (:directory @state-agent)) (serialise tasks))
    (emit something-changed)
    tasks
)

(defn interpret-task-list [qstat-output] "Returns vector of maps - PBS's tasks"
    (comment #_"
    Job Id: 89.server-2
    Job_Name = mpiwc
    Job_Owner = shukela@server-2
    resources_used.cput = 00:00:00
    resources_used.mem = 412kb
    resources_used.vmem = 8284kb
    resources_used.walltime = 00:00:02
    job_state = E
    queue = batch
    server = server-2
    Checkpoint = u
    ctime = Mon Apr  4 14:29:29 2011
    Error_Path = server-2:/home/shukela/testscript/mpiwc.e89
    exec_host = node-099/0+node-098/0
    Hold_Types = n
    Join_Path = n
    Keep_Files = n
    Mail_Points = a
    mtime = Mon Apr  4 14:29:31 2011
    Output_Path = server-2:/home/shukela/testscript/mpiwc.o89
    Priority = 0
    qtime = Mon Apr  4 14:29:29 2011
    Rerunable = True
    Resource_List.nodect = 2
    Resource_List.nodes = 2
    Resource_List.walltime = 00:00:01
    Variable_List = PBS_O_HOME=/home/shukela,PBS_O_LANG=en_US.UTF-8,PBS_O_LOGNAME=shukela,PBS_O_PATH=/usr/kerberos/bin:/usr/local/bin:/bin:/usr/bin:/usr/local/cnet/bin/:/usr/NX/bin:/usr/libexec/nx:/home/shukela/bin,PBS_O_MAIL=/var/spool/mail/shukela,PBS_O_SHELL=/bin/bash,PBS_SERVER=server-2,PBS_O_HOST=server-2,PBS_O_WORKDIR=/home/shukela/testscript,PBS_O_QUEUE=batch
    etime = Mon Apr  4 14:29:29 2011
    exit_status = 0
    submit_args = run.pbs 
    ")
    (let [
     taskwise (filter #(not (empty? %)) (split #"Job Id: " qstat-output))
     ]
     (map (fn[task-description] 
	(let [
	 strings (split #"\n\s+" task-description)
	 task-id (first strings)
	 strprops (next strings)
	 properties (reduce (fn [coll strprop] 
		 (let [re-results (re-find #"\s*(.*?)\s*=\s*(.*)" strprop)]
		  (if re-results 
		   (assoc coll (keyword (nth re-results 1)) (nth re-results 2))
		   coll)
		  )) {:job-id task-id} strprops)
	 ]
	    properties
	)
	) taskwise)))

(defmacro expand-first [the-set & code] `(do ~@(prewalk #(if (and (list? %) (contains? the-set (first %))) (macroexpand-all %) %) code)))


(defmacro ^{:private true} rj-method [name add-args & new-state] 
 `(~name  [~'this ~@add-args] 
     (send ~'state-agent 
      (fn[~'state] ;; does not need to be hygienic
       (let[
	~'tasks (:tasks ~'state)
	~'observers (:observers ~'state)
	~'auth-observer (:auth-observer ~'state)
	~'connected (:connected ~'state)
	~'session (:session ~'state)
	~'directory (:directory ~'state)
	] 
	(try
	 ~@new-state
	 (catch Exception ~'e (.printStackTrace ~'e) (println "Exception:" ~'e) ~'state)))))
     nil))


(defn exists-scheduled-tasks [tasks] 
 (> (apply + (map (fn[task] (if (= (:status task) :scheduled) 1 0)) (vals tasks))) 0))

(defmacro newtasks [tt] "For use inside rj-method"
 `(assoc ~'state :tasks (persist-tasks ~'session ~'state-agent ~tt)))


(expand-first #{rj-method} 
 (deftype RealJscfi [state-agent] Jscfi
    (rj-method periodic-update ()
      (if (and connected (exists-scheduled-tasks tasks))
	(let [
	 tasklist (ssh-execute session (read-script "qstat.txt") nil)
	 qstat (interpret-task-list tasklist)
	 ]
	 (println "Periodic qstat")
	 (let [
	  pbs-id-to-task-id-map (reduce (fn[col task] 
		(if (:pbs-id task) (assoc col (:pbs-id task) (:id task)) col)) {} (vals tasks))
	  pbs-ids-still-present-in-qstat (map (fn[x] (:job-id x)) qstat)

	  #_"When the task disappears from qstat it means it's completed" 
	  completed-pbs-ids (difference (set (keys pbs-id-to-task-id-map)) (set pbs-ids-still-present-in-qstat)) 
	  completed-task-ids (set (map #(get pbs-id-to-task-id-map %) completed-pbs-ids))

	  tasks-new (reduce (fn[col tid] 
	    (assoc col tid (assoc (get tasks tid) :status :completed))) tasks completed-task-ids)
	  ]
	  (println "Completed tasks:" completed-task-ids)
	  (newtasks tasks-new)))
      state))

    (get-tasks [this] (vals (:tasks @state-agent)) )
    (get-task [this id] (get (:tasks @state-agent) id) )
    (register-task [this task] (println "Task registered") (println task) (let [rnd-id (.toString (rand))] 
	(send state-agent #(assoc % :tasks (persist-tasks (:session %) state-agent (assoc (:tasks %) rnd-id 
	    (-> task (assoc :id rnd-id) (assoc :status :created))))))
	rnd-id))
    (rj-method alter-task (task) (println "Task altered") (newtasks (assoc tasks (:id task) task)))
    (rj-method remove-task (task-id) (println "Task removed") (newtasks (dissoc tasks task-id)))

    (rj-method compile-task (task-id) 
     (println "Compile task") 
     (let [task (get tasks task-id)]
      (let [sftp (.openChannel session "sftp")]
       (.connect sftp 3000)
       (try 
	(.mkdir sftp (format "jscfi/%s/%s" directory (:id task))) 
	(catch SftpException e (println "The directory does already exist")))
       (.put sftp (:source-file task) (format "jscfi/%s/%s/source.c" directory (:id task)) ChannelSftp/OVERWRITE)
       (.disconnect sftp))
      (println "Source code uploaded")
      (let [
       compilation-result (ssh-execute session (read-script "compile.txt" directory (:id task)) nil)
       compilation-ok (ssh-execute session (read-script "compile-ret.txt" directory (:id task)) nil)
       ]
       (println "Compilation:" compilation-ok)
       (if 
	(not= compilation-ok "0")
        (do (emit compilation-failed task compilation-result) state)
        (newtasks (assoc tasks (:id task) (-> task (assoc :status :compiled)))))
     )))

    (rj-method schedule-task (task-id) 
     (println "Schedule task") 
     (let [task (get tasks task-id)]
      (let [
       schedule-result (chomp (ssh-execute session 
	   (read-script "schedule.txt" directory (:id task))
	   (read-script "run.pbs.txt" (:walltime task) (:node-count task) (:name task) directory (:id task))))
       ]
       (println "Schedule id:" schedule-result)
       (if 
	(= schedule-result "")
        (do (emit compilation-failed task schedule-result) state)
        (newtasks (assoc tasks (:id task)
	    (-> task (assoc :status :scheduled) (assoc :pbs-id schedule-result)))))
     )))
    
    (rj-method purge-task (task-id) 
     (println "Purge task")
     (let [task (get tasks task-id)]
      (ssh-execute session (read-script "purge.txt" directory (:id task)) nil)
      (newtasks (assoc tasks task-id (assoc task :status :purged)))))

    (rj-method upload-task (task-id) 
     (println "Uploading input file") 
     (let [task (get tasks task-id)]
      (let [sftp (.openChannel session "sftp")]
       (.connect sftp 3000)
       (.put sftp (:input-file task) (format "jscfi/%s/%s/input.txt" directory (:id task)) ChannelSftp/OVERWRITE)
       (.disconnect sftp))
      (println "Input file uploaded")) state)

    (rj-method download-task (task-id) 
     (println "Downloading output file") 
     (let [task (get tasks task-id)]
      (let [sftp (.openChannel session "sftp")]
       (.connect sftp 3000)
       (.get sftp (format "jscfi/%s/%s/output.txt" directory (:id task)) (:output-file task))
       (.disconnect sftp))
      (println "Output file downloaded")) state)

    (rj-method add-observer (observer_) (assoc state :observers (conj observers observer_)))
    (rj-method remove-observer (observer_) (assoc state :observers (disj observers observer_)))
    (connect [this auth-observer address username directory]
	(send state-agent (fn[state]
	;(do (let [state @state-agent]
	   (let [
	    jsch (JSch.)
	    _1 (when (not= (get-keyfile auth-observer) "")
		(try (.addIdentity jsch (get-keyfile auth-observer)) (catch Exception e (.printStackTrace e))))
	    _2 (when (not= (get-hostsfile auth-observer) "")
		(try (.setKnownHosts jsch (get-hostsfile auth-observer)) (catch Exception e (.printStackTrace e))))
	    session (.getSession jsch username address 22)
	    password-attempts (atom 0)
	    ui (proxy [UserInfo UIKeyboardInteractive][]
		(promptYesNo               [message] 
		    (println message) (connection-stage auth-observer message) (println "(Accepting it)") true)
		(promptPassphrase          [message] (println message) (connection-stage auth-observer message) true)
		(promptPassword            [message] (println message) (connection-stage auth-observer message) true)
		(promptKeyboardInteractive [message] (println message) (connection-stage auth-observer message) true)
		(getPassword [] 
		 (when (> @password-attempts 0) 
		  (throw (Exception. "Invalid password")))
		 (println "*** Trying password authentication")
		 (swap! password-attempts inc)
		 (get-password auth-observer)
		 )
	       )]
	    (.setUserInfo session ui)
	    (connection-stage auth-observer (format "Connecting to %s@%s" username address))
	    (try
	     (.connect session 3000)
	     (connection-stage auth-observer (format "Connected to %s@%s" username address))
	     (auth-succeed auth-observer)
	     (emit-impl state-agent #(connected %))
	     (emit-impl state-agent #(something-changed %))
	     (let [tasks (interpret-tasks (ssh-execute session (read-script "read-tasks.txt" directory) nil))]
	      (-> state 
	       (assoc :connected true) 
	       (assoc :auth-observer auth-observer) 
	       (assoc :session session)
	       (assoc :tasks tasks)
	       (assoc :directory directory)
	      ))
	     (catch Exception e 
	      (.printStackTrace e)
	      (auth-failed auth-observer)
	      (connection-stage auth-observer (.toString e))
	      state)
	    )))))
))


(defn get-real-jscfi [] (new RealJscfi (agent {
    :observers #{},
    :auth-observer nil,
    :tasks {},
    :connected false,
    :directory "",
    })))

