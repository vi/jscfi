(ns org.vi-server.jscfi.gui
    "GUI for Jscfi"
    (:use org.vi-server.jscfi.jscfi)
    (:use org.vi-server.jscfi.real)
    (:import 
     (javax.swing JPanel JFrame JLabel JTextField JTextArea JButton SwingUtilities JList JScrollPane DefaultListModel AbstractAction Action KeyStroke)
     (javax.swing JMenu JMenuBar JPasswordField)
     (java.awt.event KeyEvent MouseAdapter WindowAdapter)
     (java.awt Event)
     (net.miginfocom.swing MigLayout))
    (:import (java.util TimerTask Timer)))

(defn create-action "Creates an implementation of AbstractAction." [name behavior options]
 (let [
  action (proxy [AbstractAction] [name]
      (actionPerformed [event] (behavior event)))]
  (if options
   (doseq [key (keys options)] (.putValue action key (options key))))
  action))

(defprotocol TaskListEntry (task [this]))
(deftype TaskListEntryImpl [task] TaskListEntry
    (toString [this] (format "%s    %s    %s    %s" (:status task) (:source-file task) (:name task) (:outer-id task)))
    (task [this] task))

(defn create-task-window [task jscfi]
 (let [
  panel (JPanel. (MigLayout. "", "[pref][grow]", "[pref]7[grow]"))
  frame (JFrame.)
  name-field        (JTextField.   (:name task))
  status-field      (JLabel.       (str (:status task)))
  outer-id-field     (JLabel.       (:outer-id task))
  source-file-field (JTextField.   (:source-file task))
  input-file-field  (JTextField.   (:input-file task))
  output-file-field (JTextField.   (:output-file task))
  node-count-field  (JTextField.   (str (:node-count task)))
  task-id (atom (:id task))
  action-display (create-action "Debug Print" (fn [_] (prn (get-task jscfi @task-id)))
      { Action/SHORT_DESCRIPTION  "Display it", Action/ACCELERATOR_KEY (KeyStroke/getKeyStroke KeyEvent/VK_L Event/CTRL_MASK) })
  action-create (create-action "Create/Change" (fn [_] 
	  (let [
	   task (if @task-id (get-task jscfi @task-id) {})
           newtask (-> task
	      (assoc :name           (.getText name-field))
	      (assoc :source-file    (.getText source-file-field))
	      (assoc :input-file     (.getText input-file-field))
	      (assoc :output-file    (.getText output-file-field))
	      (assoc :node-count     (.getText node-count-field))
	      )]
	   (if @task-id
	    (alter-task jscfi newtask)
	    (swap! task-id (fn[_](register-task jscfi newtask))))))
      { Action/SHORT_DESCRIPTION  "Create/change a task", Action/ACCELERATOR_KEY (KeyStroke/getKeyStroke KeyEvent/VK_ENTER Event/CTRL_MASK) })

  action-remove (create-action "Remove" (fn [_] (remove-task jscfi @task-id) (swap! task-id (fn[_]nil)))
      { Action/SHORT_DESCRIPTION  "Remove the task", Action/ACCELERATOR_KEY (KeyStroke/getKeyStroke KeyEvent/VK_D Event/CTRL_MASK) })
  action-compile (create-action "Compile" (fn [_] (compile-task jscfi @task-id))
      { Action/SHORT_DESCRIPTION  "Upload the source code and compile the task", Action/ACCELERATOR_KEY (KeyStroke/getKeyStroke KeyEvent/VK_L Event/CTRL_MASK) })
  action-upload (create-action "Upload" (fn [_] (upload-task jscfi @task-id))
      { Action/SHORT_DESCRIPTION  "Upload the input data as input.txt", Action/ACCELERATOR_KEY (KeyStroke/getKeyStroke KeyEvent/VK_U Event/CTRL_MASK) })
  action-schedule (create-action "Schedule" (fn [_] (schedule-task jscfi @task-id))
      { Action/SHORT_DESCRIPTION  "Schedule the task for execution", Action/ACCELERATOR_KEY (KeyStroke/getKeyStroke KeyEvent/VK_F Event/CTRL_MASK) })
  action-download (create-action "Download" (fn [_] (download-task jscfi @task-id))
      { Action/SHORT_DESCRIPTION  "Download output.txt", Action/ACCELERATOR_KEY (KeyStroke/getKeyStroke KeyEvent/VK_D Event/CTRL_MASK) })
  button-panel (JPanel. (MigLayout. "", "[pref][pref]", "[grow]5"))
  ]
  (doto frame 
   (.setSize 600 340)
   (.setContentPane panel)
   (.setTitle "Jscfi task")
   )
  (doto button-panel
   (.add (JButton. action-display) "growx")
   (.add (JButton. action-create) "growx")
   (.add (JButton. action-compile) "growx")
   (.add (JButton. action-upload) "growx,wrap")
   (.add (JButton. action-schedule) "growx")
   (.add (JButton. action-download) "growx")
   (.add (JButton. action-remove) "growx")
   (.revalidate))
  (doto panel
   (.add (JLabel. "Name:"))          (.add name-field         "growx,wrap")
   (.add (JLabel. "Status:"))        (.add status-field       "growx,wrap")
   (.add (JLabel. "Task id:"))       (.add outer-id-field     "growx,wrap")
   (.add (JLabel. "Source file:"))   (.add source-file-field  "growx,wrap")
   (.add (JLabel. "Input file:"))    (.add input-file-field   "growx,wrap")
   (.add (JLabel. "Output file:"))   (.add output-file-field  "growx,wrap")
   (.add (JLabel. "Node count:"))    (.add node-count-field   "growx,wrap")
   (.add button-panel "span 2")
   (.revalidate))
  (.setLocationRelativeTo frame nil)
  frame))




(defn create-authentication-window [jscfi]
 (let [
  panel (JPanel. (MigLayout. "", "[][grow]", "[grow]5[grow][grow]"))
  frame (JFrame.)
  prefs (.node (java.util.prefs.Preferences/userRoot) "/org/vi-server/jscfi")
  user-field (JTextField. (.get prefs "login" "zimyanin"))
  server-field (JTextField. (.get prefs "hostname" "217.21.43.14"))
  password-field (JPasswordField.)
  keyfile-field (JTextField. (.get prefs "keyfile" ""))
  hostsfile-field (JTextField. (.get prefs "known_hosts" ""))
  connstage-label (JLabel.)
  auth-observer (reify AuthObserver
      (get-password [this] (.getText password-field))
      (get-keyfile [this] (.getText keyfile-field))
      (get-hostsfile [this] (.getText hostsfile-field))
      (auth-succeed [this] 
       (.put prefs "login" (.getText user-field))
       (.put prefs "hostname" (.getText server-field))
       (.put prefs "keyfile" (.getText keyfile-field))
       (.put prefs "known_hosts" (.getText hostsfile-field))
       (doto frame (.setVisible false) (.dispose))
       )
      (auth-failed [this] (javax.swing.JOptionPane/showMessageDialog nil 
			   "Authentication failed" "jscfi" javax.swing.JOptionPane/INFORMATION_MESSAGE))
      (connection-stage [this msg] (SwingUtilities/invokeLater (fn [](.setText connstage-label msg))))
      )
  action-connect (create-action "Connect" (fn [_] 
	  (connect jscfi auth-observer (.getText server-field) (.getText user-field))) {})
  ]
  (doto frame 
   (.setSize 400 220)
   (.setContentPane panel)
   (.setDefaultCloseOperation JFrame/DO_NOTHING_ON_CLOSE)
   (.addWindowListener (proxy [WindowAdapter] [] (windowClosing [_] (comment "There was password delivery here") (System/exit 0))))
   (.setTitle "Login to SCFI")
  )
  (doto panel
   (.add (JLabel. "Server:"))
   (.add server-field "growx,wrap")
   (.add (JLabel. "Login:"))
   (.add user-field "growx,wrap")
   (.add (JLabel. "Password:"))
   (.add password-field "growx,wrap")
   (.add (JLabel. "Keyfile:"))
   (.add keyfile-field "growx,wrap")
   (.add (JLabel. "Known hosts:"))
   (.add hostsfile-field "growx,wrap")
   (.add connstage-label "span 2,wrap")
   (.add (JButton. action-connect) "span 2")
   (.revalidate))
  (.setLocationRelativeTo frame nil)

  ;; if known_hosts is not saved in preferences, try to detect it
  (try
   (when (= "" (.getText hostsfile-field))
    (let [
     c1 (System/getenv "HOME")
     c2 (System/getenv "APPDATA")
     ]
     (if c2
      (.setText hostsfile-field (str c2 "\\known_hosts"))
      (when c1 (.setText hostsfile-field (str c1 "/.ssh/known_hosts"))
       (.mkdirs (java.io.File. (str c1 "/.ssh")))))))
   (catch Exception e (.printStackTrace e)))
  frame))




(defn create-main-window [jscfi] 
 (let [
  panel (JPanel. (MigLayout. "", "[grow][pref][pref]", "[pref][grow]"))
  frame (JFrame.)
  observer (reify JscfiObserver 
      (connected [this] (.setVisible frame true))
      (compilation-failed [this task message] (javax.swing.JOptionPane/showMessageDialog nil 
				message (:name task) javax.swing.JOptionPane/INFORMATION_MESSAGE))
      )
  text-field (JTextField.)
  list-model (DefaultListModel.)
  jlist (JList. list-model)
  action-create (create-action "Create task" (fn [_] (.setVisible (create-task-window {} jscfi) true))
      { Action/SHORT_DESCRIPTION  "Open window for task creation", Action/ACCELERATOR_KEY (KeyStroke/getKeyStroke KeyEvent/VK_N Event/CTRL_MASK) })
  action-refresh (create-action "Refresh" (fn [_] 
	  (.clear list-model)
	  (doall (map #(.add list-model 0 (TaskListEntryImpl. %)) (get-tasks jscfi))))
      { Action/SHORT_DESCRIPTION  "Refresh the list of tasks", Action/ACCELERATOR_KEY (KeyStroke/getKeyStroke KeyEvent/VK_R Event/CTRL_MASK) })
  action-update (create-action "Update" (fn [_] 
	  (periodic-update jscfi))
      { Action/SHORT_DESCRIPTION  "Run qstat to update things", Action/ACCELERATOR_KEY (KeyStroke/getKeyStroke KeyEvent/VK_U Event/CTRL_MASK) })
  action-open (create-action "Open" 
      (fn [_] 
       (let [index (.getSelectedIndex jlist)] 
	(when (not= index -1) 
	 (let [t (task (.elementAt list-model index))]
	  (.setVisible (create-task-window t jscfi) true)))))
      { Action/SHORT_DESCRIPTION  "Open selected task", Action/ACCELERATOR_KEY (KeyStroke/getKeyStroke KeyEvent/VK_O Event/CTRL_MASK) })
  menubar (JMenuBar.)
  view-menu (JMenu. "File")
  action-menu (JMenu. "Action")
  ]
  (doto frame 
   (.setSize 600 400)
   (.setContentPane panel)
   (.setJMenuBar menubar)
   (.setLocationRelativeTo nil)
   (.setTitle "Jscfi - SuperComputer Phoenix Initiative GUI"))
  (doto panel
   (.add text-field "growx")
   (.add (JButton. action-create) )
   (.add (JButton. action-open) "wrap")
   (.add (JScrollPane. jlist) "grow,span 3")
   (.revalidate))
  (.add view-menu    action-refresh)
  (.add view-menu    action-update)
  (.add view-menu    action-open)
  (.add action-menu    action-create)
  (doto menubar
   (.add view-menu)
   (.add action-menu))                
  (set-observer jscfi observer)
  (.setVisible (create-authentication-window jscfi) true)
  (.actionPerformed action-refresh nil) 
  (.addMouseListener jlist (proxy [MouseAdapter] [] (mouseClicked [event] (when (= (.getClickCount event) 2) (.actionPerformed action-open nil)))))
  (.addWindowListener frame (proxy [WindowAdapter] [] (windowClosing [_] (comment "There was password delivery here") (System/exit 0))))
  (let [task (proxy [TimerTask] []
      	(run [] (periodic-update jscfi)))]
   (. (new Timer) (schedule task (long 3000) (long 5000))))
  frame))

(defn main []
 (SwingUtilities/invokeLater (fn [](create-main-window (get-real-jscfi)))))


