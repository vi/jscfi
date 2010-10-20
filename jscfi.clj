;; Task uploader and executer for SCFI BSU
;; Clojure Swing example by Keith Bennett, March 2009 ;; kbennett -at- bbsinc -dot- biz

(ns jscfi
  (:import (java.awt BorderLayout Event GridLayout Toolkit)
           (java.awt.event KeyEvent)
           (javax.swing AbstractAction Action BorderFactory 
           JFrame JPanel JButton JMenu JMenuBar JTextField JLabel KeyStroke)
           (javax.swing.event DocumentListener))
  (:import (com.jcraft.jsch JSch Channel Session UserInfo UIKeyboardInteractive ChannelSftp))
  (:import (java.io.ByteArrayInputStream))
  (:use jscfi.guilib)
  )

(def prefs (.node (java.util.prefs.Preferences/userRoot) "/org/vi-server/jscfi"))  

(def server-text-field (lazy-init create-a-text-field (.get prefs "hostname" "127.0.0.1") "IP address or hostname of the server"))
(def login-text-field (lazy-init create-a-text-field (.get prefs "login" "test") "Username"))
(def password-text-field (lazy-init create-a-text-field (.get prefs "password" "test99test") "Password" :password))
(def source-file-text-field (lazy-init create-a-text-field (.get prefs "sourcefile" "/tmp/hello.c") "Local path of source code file"))
(def resulting-file-text-field (lazy-init create-a-text-field (.get prefs "outfile" "/tmp/hello.out") "Local path for output file"))

(defn get-ssh-session [] 
    (def jsch (JSch.))
    (def session (.getSession jsch (.getText (login-text-field)) (.getText (server-text-field)) 22))
    (def ui (proxy [UserInfo UIKeyboardInteractive][] 
      (promptYesNo               [message] (print message) (newline) true)
      (promptPassphrase          [message] (print message) (newline) true)
      (promptPassword            [message] (print message) (newline) true)
      (promptKeyboardInteractive [message] (print message) (newline) true)
      (getPassword [] (.getText (password-text-field))
       )))
    (.setUserInfo session ui)
    (.connect session 30000)
    session
)

(defn upload-file-to-server [arg] 
    (def session (get-ssh-session))
    (def sftp (.openChannel session "sftp"))
    (.connect sftp 3000)
    (.put sftp (.getText (source-file-text-field)) "source.c" ChannelSftp/OVERWRITE)
    (.disconnect sftp)

    (def shell (.openChannel session "shell"))
    (.setOutputStream shell System/out)
    (.setExtOutputStream shell System/err)
    (.setInputStream shell (java.io.ByteArrayInputStream. (.getBytes "gcc -O2 -g -Wall source.c -o program\nexit\n")))
    (.connect shell 3000)
)

(defn execute-program-on-server [arg] 
    (def session (get-ssh-session))
    (def shell (.openChannel session "shell"))
    (.setOutputStream shell System/out)
    (.setExtOutputStream shell System/err)
    (.setInputStream shell (java.io.ByteArrayInputStream. (.getBytes "./program > output.txt\nexit\n")))
    (.connect shell 3000)
)

(defn create-textfields-panel
"Creates panel containing the labels and text fields."
[]

  (let [
    create-an-inner-panel #(JPanel. (GridLayout. 0 1 5 5))
    label-panel           (create-an-inner-panel)
    text-field-panel      (create-an-inner-panel)
    outer-panel           (JPanel. (BorderLayout.))]

    (doto label-panel
      (.add (JLabel. "Server:"))
      (.add (JLabel. "Login:"))
      (.add (JLabel. "Password:"))
      (.add (JLabel. "Source code file:"))
      (.add (JLabel. "Save the result to:"))
    )
    
    (doto text-field-panel
	(.add (server-text-field))
	(.add (login-text-field))
	(.add (password-text-field))
	(.add (source-file-text-field))
	(.add (resulting-file-text-field))
    )

    (doto outer-panel
      (.add label-panel BorderLayout/WEST)
      (.add text-field-panel BorderLayout/CENTER))))



(def exit-action (create-action "Exit"
    (fn [_] (System/exit 0))

    { Action/SHORT_DESCRIPTION  "Exit this program",
      Action/ACCELERATOR_KEY
            (KeyStroke/getKeyStroke KeyEvent/VK_X Event/CTRL_MASK) }))

(def upload-action (create-action "Upload"
    upload-file-to-server

    { Action/SHORT_DESCRIPTION  "Upload and compile the source code to SCFI BSU",
      Action/ACCELERATOR_KEY
            (KeyStroke/getKeyStroke KeyEvent/VK_U Event/CTRL_MASK) }))

(def execute-action (create-action "Execute"
    execute-program-on-server

    { Action/SHORT_DESCRIPTION  "Execute the code in SCFI BSU",
      Action/ACCELERATOR_KEY
            (KeyStroke/getKeyStroke KeyEvent/VK_E Event/CTRL_MASK) }))


(defn create-menu-bar
"Creates the menu bar with File, Edit, and Convert menus."
[]

  (let [
    menubar (JMenuBar.)
    file-menu (JMenu. "File")
    action-menu (JMenu. "Action")]

    (.add file-menu    exit-action)
    (.add action-menu    upload-action)
    (.add action-menu    execute-action)
    
    (doto menubar
      (.add file-menu)
      (.add action-menu))))


(defn create-buttons-panel 
"Creates the panel with conversion, clear, and exit buttons."
[]
  (let [
    inner-panel (JPanel. (GridLayout. 1 0 5 5))
    outer-panel (JPanel. (BorderLayout.))]

    (doto inner-panel
      (.add (JButton. upload-action))
      (.add (JButton. execute-action))
      (.add (JButton. exit-action)))

    (doto outer-panel
      (.add inner-panel BorderLayout/EAST)
      (.setBorder (BorderFactory/createEmptyBorder 10 0 0 0)))))





(defn create-frame
"Creates the main JFrame used by the program."
[]

  (let [
    f (JFrame. "SCFI BSU uploader")
    content-pane (.getContentPane f)]

    (doto content-pane
      (.add (create-textfields-panel) BorderLayout/CENTER)
      (.add (create-buttons-panel) BorderLayout/SOUTH)
      (.setBorder (BorderFactory/createEmptyBorder 12 12 12 12)))

    (doto f
      (.setDefaultCloseOperation JFrame/EXIT_ON_CLOSE)
      (.setJMenuBar (create-menu-bar))
      (.pack))

    (center-on-screen f)))


(defn main []
    (def main-frame (create-frame))
    (.setVisible main-frame true))

(main)
