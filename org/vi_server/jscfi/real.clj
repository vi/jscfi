(ns org.vi-server.jscfi.real
 "Real Jscfi that interacts with with SSH"
 (:use org.vi-server.jscfi.jscfi)
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

(deftype RealJscfi [state-agent] Jscfi
    (get-tasks [this] 
     (let [state @state-agent]
      (when (:connected state)
	(let [session (:session state)]
	 (println (ssh-execute session "qstat -a | perl -ne 'chomp; next if /Job id\\s+Name/; next if /^-----------/; s/\\s+/:/g; print \"$_\n\"'" nil))))
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

