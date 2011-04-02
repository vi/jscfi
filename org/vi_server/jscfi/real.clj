(ns org.vi-server.jscfi.real
 "Real Jscfi that interacts with with SSH"
 (:use org.vi-server.jscfi.jscfi)
 (:import (com.jcraft.jsch JSch Channel Session UserInfo UIKeyboardInteractive ChannelSftp))
 (:import (java.io.ByteArrayInputStream))
 )      
(deftype RealJscfi [state-agent] Jscfi
    (get-tasks [this] (:tasks @state-agent))
    (get-task [this id] (loop [t (get-tasks this)] (if (= (:id (first t)) id) (first t) (if (empty? t) nil (recur (next t) )))))
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
	     (-> state (assoc :connected true) (assoc :auth-observer auth-observer))
	     (catch Exception e 
	      (.printStackTrace e)
	      (auth-failed auth-observer)
	      (connection-stage auth-observer (.toString e))
	      state)
	    )))))

)


(defn get-real-jscfi [] (new RealJscfi (agent {:observer nil, :auth-observer nil, :tasks [], :connected false})))

