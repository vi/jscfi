(ns jscfi.ssh
  (:import (com.jcraft.jsch JSch Channel Session UserInfo UIKeyboardInteractive ChannelSftp))
  (:import (java.io.ByteArrayInputStream))
  )                

(def ssh-session (agent nil))

(defn login-to-server [sess status-observer server port user password]
    (status-observer "initializing connection")
    (def jsch (JSch.))
    (def session (.getSession jsch user server port))
    (def ui (proxy [UserInfo UIKeyboardInteractive][] 
      (promptYesNo               [message] (print message) (newline) true)
      (promptPassphrase          [message] (print message) (newline) true)
      (promptPassword            [message] (print message) (newline) true)
      (promptKeyboardInteractive [message] (print message) (newline) true)
      (getPassword [] password)
       ))
    (.setUserInfo session ui)
    (status-observer "connecting")
    (.connect session 3000)
    (status-observer "connected")
    {
	:session,session
	:status-observer,status-observer
    }
)

(defn upload-file-to-server [sess file]
    ((get sess :status-observer) "initializing upload")
    (def sftp (.openChannel (get sess :session) "sftp"))
    (.connect sftp 3000)
    ((get sess :status-observer) "uploading")
    (.put sftp file "source.c" ChannelSftp/OVERWRITE)
    ((get sess :status-observer) "uploded")
    (.disconnect sftp)

    ((get sess :status-observer) "initializing complilation")
    (def shell (.openChannel (get sess :session) "shell"))
    (.setOutputStream shell System/out)
    (.setExtOutputStream shell System/err)
    (.setInputStream shell (java.io.ByteArrayInputStream. (.getBytes "gcc -O2 -g -Wall source.c -o program\nexit\n")))
    ((get sess :status-observer) "compiling")
    (.connect shell 3000)
    ((get sess :status-observer) "compiled")
    sess
)

(defn execute-program-on-server [sess]
    ((get sess :status-observer) "initializing execution")
    (def shell (.openChannel (get sess :session) "shell"))
    (.setOutputStream shell System/out)
    (.setExtOutputStream shell System/err)
    (.setInputStream shell (java.io.ByteArrayInputStream. (.getBytes "./program > output.txt\nexit\n")))
    ((get sess :status-observer) "executing")
    (.connect shell 3000)
    ((get sess :status-observer) "executed")
    sess
)


