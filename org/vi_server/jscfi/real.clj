(ns org.vi-server.jscfi.real
 "Real Jscfi that interacts with with SSH"
 (:use clojure.walk)
 (:use [clojure.set :only [difference intersection]])
 (:use org.vi-server.jscfi.jscfi)
 (:use [clojure.contrib.string :only [split join upper-case lower-case trim blank?]])
 (:use [clojure.contrib.str-utils :only [chomp]])
 ;(:require [org.danlarkin.json :as json])
 ;(:require [clj-yaml.core :as yaml])
 (:import (com.jcraft.jsch JSch Channel Session UserInfo UIKeyboardInteractive ChannelSftp))
 (:import (java.io.ByteArrayInputStream))
 )  

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

(defn persist-tasks [session tasks]
    (ssh-execute session "mkdir -p jscfi && cd jscfi && cat > tasks.clj" (serialise tasks))
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
	~'observer (:observer ~'state)
	~'auth-observer (:auth-observer ~'state)
	~'connected (:connected ~'state)
	~'session (:session ~'state)
	] 
	(try
	 ~@new-state
	 (catch Exception ~'e (.printStackTrace ~'e) (println "Exception:" ~'e) ~'state)))))
     nil))


(defn exists-scheduled-tasks [tasks] 
 (> (apply + (map (fn[task] (if (= (:status task) :scheduled) 1 0)) (vals tasks))) 0))

(defmacro newtasks [tt] "For use inside rj-method"
 `(assoc ~'state :tasks (persist-tasks ~'session ~tt)))

(expand-first #{rj-method} 
 (deftype RealJscfi [state-agent] Jscfi
    (rj-method periodic-update ()
      (println tasks)
      (when (and connected (exists-scheduled-tasks tasks))
	(let [
	 tasklist (ssh-execute session "qstat -f1" nil)
	 qstat (interpret-task-list tasklist)
	 ]
	 (println "Periodic qstat")
	 (let [
	  v1 (reduce (fn[col task] (if (:pbs-id task) (assoc col (:pbs-id task) (:id task)) col)) {} (vals tasks))
	  v2 (map (fn[x] (:job-id x)) qstat)
	  v3 (difference (set (keys v1)) (set v2)) #_"When the task disappears from qstat it means it's completed"
	  v4 (set (map #(get v1 %) v3))
	  newtasks (reduce (fn[col tid] (assoc col tid (assoc (get tasks tid) :status :completed))) tasks v4)
	  ]
	  (println newtasks)
	  )
	 ))
      state)

    (get-tasks [this] (vals (:tasks @state-agent)) )
    (get-task [this id] (get (:tasks @state-agent) id) )
    (register-task [this task] (println "Task registered") (println task) (let [rnd-id (.toString (rand))] 
	(send state-agent #(assoc % :tasks (persist-tasks (:session %) (assoc (:tasks %) rnd-id 
	    (-> task (assoc :id rnd-id) (assoc :status :created))))))
	rnd-id))
    (rj-method alter-task (task) (println "Task altered") (newtasks (assoc tasks (:id task) task)))
    (rj-method remove-task (task-id) (println "Task removed") (newtasks (dissoc tasks task-id)))

    (rj-method compile-task (task-id) 
     (println "Compile task") 
     (let [task (get tasks task-id)]
      (let [sftp (.openChannel session "sftp")]
       (.connect sftp 3000)
       (.put sftp (:source-file task) "jscfi/source.c" ChannelSftp/OVERWRITE)
       (.disconnect sftp))
      (println "Source code uploaded")
      (let [
       compilation-result (ssh-execute session "cd jscfi && rm -f program && mpicc source.c -o program 2>&1; echo -n $? > ret" nil)
       compilation-ok (ssh-execute session "cat jscfi/ret" nil)
       ]
       (println "Compilation:" compilation-ok)
       (if 
	(not= compilation-ok "0")
        (do (compilation-failed observer task compilation-result) state)
        (newtasks (assoc tasks (:id task) (-> task (assoc :status :compiled)))))
     )))

    (rj-method schedule-task (task-id) 
     (println "Schedule task") 
     (let [task (get tasks task-id)]
      (let [
       schedule-result (chomp (ssh-execute session 
	   "cd jscfi && cat > run.pbs && qsub run.pbs" 
	   (format "#PBS -l walltime=00:00:01
#PBS -l nodes=%s
#PBS -N %s
hostname
cd ~/jscfi/
date >> ~/jscfi/log
hostname >> ~/jscfi/log
echo \"PBS_O_WORKDIR=$PBS_O_WORKDIR\" >> ~/jscfi/log
cp $PBS_NODEFILE ~/jscfi/pbs-nodes
mpirun --hostfile ~/jscfi/pbs-nodes prog 2> stderr > stdout"
	    (:node-count task) (:name task))))
       ]
       (println "Schedule id:" schedule-result)
       (if 
	(= schedule-result "")
        (do (compilation-failed observer task schedule-result) state)
        (newtasks (assoc tasks (:id task)
	    (-> task (assoc :status :scheduled) (assoc :pbs-id schedule-result)))))
     )))

    (rj-method upload-task (task-id) 
     (println "Uploading input file") 
     (let [task (get tasks task-id)]
      (let [sftp (.openChannel session "sftp")]
       (.connect sftp 3000)
       (.put sftp (:input-file task) "jscfi/input.txt" ChannelSftp/OVERWRITE)
       (.disconnect sftp))
      (println "Input file uploaded")) state)

    (rj-method download-task (task-id) 
     (println "Downloading output file") 
     (let [task (get tasks task-id)]
      (let [sftp (.openChannel session "sftp")]
       (.connect sftp 3000)
       (.get sftp "jscfi/output.txt" (:output-file task))
       (.disconnect sftp))
      (println "Output file downloaded")) state)

    (rj-method set-observer (observer_) (assoc state :observer observer_))
    (connect [this auth-observer address username]
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
	     (connected (:observer state))
	     (let [tasks (interpret-tasks 
		 (ssh-execute session "mkdir -p jscfi && cd jscfi && touch tasks.clj && cat tasks.clj" nil))]
	      (-> state 
	       (assoc :connected true) 
	       (assoc :auth-observer auth-observer) 
	       (assoc :session session)
	       (assoc :tasks tasks)
	      ))
	     (catch Exception e 
	      (.printStackTrace e)
	      (auth-failed auth-observer)
	      (connection-stage auth-observer (.toString e))
	      state)
	    )))))
))


(defn get-real-jscfi [] (new RealJscfi (agent {
    :observer nil,
    :auth-observer nil,
    :tasks {},
    :connected false
    })))

