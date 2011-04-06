(ns org.vi-server.jscfi.real
 "Real Jscfi that interacts with with SSH"
 (:use clojure.walk)
 (:use org.vi-server.jscfi.jscfi)
 (:use [clojure.contrib.string :only [split join upper-case lower-case trim blank?]])
 ;(:require [org.danlarkin.json :as json])
 (:require [clj-yaml.core :as yaml])
 (:import (com.jcraft.jsch JSch Channel Session UserInfo UIKeyboardInteractive ChannelSftp))
 (:import (java.io.ByteArrayInputStream))
 )  

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
  (yaml/parse-string source)))

(defn persist-tasks [session tasks]
    (ssh-execute session "mkdir -p jscfi && cd jscfi && cat > tasks.yaml" (yaml/generate-string tasks))
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
	~@new-state)))
     nil))

(expand-first #{rj-method} 
 (deftype RealJscfi [state-agent] Jscfi
    (rj-method periodic-update ()
      (when connected
	(let [tasklist (ssh-execute session "qstat -f1" nil)]                                                     
	 (println (yaml/generate-string (interpret-task-list tasklist)))))
      state)

    (get-tasks [this] (vals (:tasks @state-agent)) )
    (get-task [this id] (get (:tasks @state-agent) id) )
    (register-task [this task] (println "Task registered") (println task) (let [rnd-id (.toString (rand))] 
	(send state-agent #(assoc % :tasks (persist-tasks (:session %) (assoc (:tasks %) rnd-id 
	    (-> task (assoc :id rnd-id) (assoc :status :created))))))
	rnd-id))
    (rj-method alter-task (task) (println "Task altered") (assoc state :tasks (persist-tasks session (assoc tasks (:id task) task))))
    (rj-method remove-task (task-id) (println "Task removed") (assoc state :tasks (persist-tasks session (dissoc tasks task-id))))

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
		 (ssh-execute session "mkdir -p jscfi && cd jscfi && touch tasks.yaml && cat tasks.yaml" nil))]
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

