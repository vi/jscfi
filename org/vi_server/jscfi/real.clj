(ns org.vi-server.jscfi.real
 "Real Jscfi that interacts with with SSH"
 (:use org.vi-server.jscfi.jscfi)
 ;(:require [org.danlarkin.json :as json])
 (:require [clj-yaml.core :as yaml])
 (:use [clojure.contrib.string :only [split join upper-case lower-case trim blank?]])
 (:import (com.jcraft.jsch JSch Channel Session UserInfo UIKeyboardInteractive ChannelSftp))
 (:import (java.io.ByteArrayInputStream))
 )  

(defn ^String ssh-execute [session command input]
 (try
  (let [
   channel (.openChannel session "exec")
   output (java.io.ByteArrayOutputStream.)
   input (if input (java.io.ByteArrayOutputStream. (.getBytes input)) nil)
   ]
   (doto channel
    (.setOutputStream output)
    (.setExtOutputStream System/err)
    (.setInputStream input)
    (.setCommand command)
    (.connect 3000))
   (while (not (.isClosed channel)) (Thread/sleep 100))
   (str output))
  (catch Exception e (.printStackTrace e) nil)))

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

(deftype RealJscfi [state-agent] Jscfi
    (get-tasks [this] 
     (let [state @state-agent]
      (when (:connected state)
	(let [
	 session (:session state)
	 tasklist (ssh-execute session "qstat -f1" nil)
	 ]                                                     
	 (println (yaml/generate-string (interpret-task-list tasklist)))
	 ))
      (:tasks @state-agent)
      ))
    (get-task [this id] (loop [t (:tasks @state-agent)] (if (= (:id (first t)) id) (first t) (if (empty? t) nil (recur (next t) )))))
    (set-observer [this observer]
     (send state-agent (fn[state]
	     (-> state (assoc :observer observer)))))     
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
	     (-> state (assoc :connected true) (assoc :auth-observer auth-observer) (assoc :session session))
	     (catch Exception e 
	      (.printStackTrace e)
	      (auth-failed auth-observer)
	      (connection-stage auth-observer (.toString e))
	      state)
	    )))))

)


(defn get-real-jscfi [] (new RealJscfi (agent {:observer nil, :auth-observer nil, :tasks [], :connected false})))

