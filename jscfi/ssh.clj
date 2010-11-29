(ns jscfi.ssh
  (:import (com.jcraft.jsch JSch Channel Session UserInfo UIKeyboardInteractive ChannelSftp))
  (:import (java.io.ByteArrayInputStream))
  )                


(defn get-ssh-session [server port user password] 
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
    (.connect session 3000)
    session
)

(def ssh-session (agent nil))

(defn sendee-upload-file-to-server [sess file server port user password]
    (def session (if (nil? sess) (get-ssh-session server port user password) sess))
    (def sftp (.openChannel session "sftp"))
    (.connect sftp 3000)
    (.put sftp file "source.c" ChannelSftp/OVERWRITE)
    (.disconnect sftp)

    (def shell (.openChannel session "shell"))
    (.setOutputStream shell System/out)
    (.setExtOutputStream shell System/err)
    (.setInputStream shell (java.io.ByteArrayInputStream. (.getBytes "gcc -O2 -g -Wall source.c -o program\nexit\n")))
    (.connect shell 3000)
    session
)

(defn sendee-execute-program-on-server [sess server port user password]
    (def session (if (nil? sess) (get-ssh-session server port user password) sess))
    (def shell (.openChannel session "shell"))
    (.setOutputStream shell System/out)
    (.setExtOutputStream shell System/err)
    (.setInputStream shell (java.io.ByteArrayInputStream. (.getBytes "./program > output.txt\nexit\n")))
    (.connect shell 3000)
    session
)

(defn upload-file-to-server [file server port user password] 
    (send ssh-session sendee-upload-file-to-server file server port user password)
)

(defn execute-program-on-server [server port user password] 
    (send ssh-session sendee-execute-program-on-server server port user password)
)
