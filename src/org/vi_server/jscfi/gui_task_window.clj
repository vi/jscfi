(ns org.vi-server.jscfi.gui-task-window
    "GUI for Jscfi's task window"
    (:use org.vi-server.jscfi.jscfi)
    (:use org.vi-server.jscfi.gui-common)
    (:import 
     (javax.swing JPanel JFrame JLabel JTextField JTextArea JButton SwingUtilities JList JScrollPane DefaultListModel AbstractAction Action KeyStroke)
     (javax.swing JMenu JMenuBar JPasswordField)
     (java.awt.event KeyEvent MouseAdapter WindowAdapter)
     (java.awt Event)
     (net.miginfocom.swing MigLayout)))


(defn create-task-window [task jscfi]
 (let [
  panel (JPanel. (MigLayout. "", "[pref][grow][pref]", "[pref]7[grow]"))
  frame (JFrame.)
  name-field        (JTextField.   (:name task))
  status-field      (JLabel.       (str (:status task)))
  outer-id-field    (JLabel.       (:pbs-id task))
  source-file-field (JTextField.   (:source-file task))
  input-file-field  (JTextField.   (:input-file task))
  output-file-field (JTextField.   (:output-file task))
  node-count-field  (JTextField.   (str (:node-count task)))
  walltime-field    (JTextField.   (str (:walltime task)))
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
	      (assoc :walltime       (.getText walltime-field))
	      )]
	   (try
	    (doall (map 
	     #(when-not (re-find (:regex %) (get newtask (:field %)))
	      (throw (Exception. (str (:caption %) " must match" (:regex %))))) [
		   {:field :name,       :caption "Task name",  :regex #"^[a-zA-Z0-9_]{1,32}$"}
		   {:field :node-count, :caption "Node count", :regex #"^[0-9]{1,10}$"}
		   {:field :walltime,   :caption "Walltime",   :regex #"^\d\d:\d\d:\d\d$"}
	    ]))
	    (if @task-id
	     (alter-task jscfi newtask)
	     (swap! task-id (fn[_](register-task jscfi newtask))))
	  (catch Exception e (println e) (msgbox (str e))))))
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
  action-purge (create-action "Purge" (fn [_] (purge-task jscfi @task-id))
      { Action/SHORT_DESCRIPTION  "Remove task files from server", Action/ACCELERATOR_KEY (KeyStroke/getKeyStroke KeyEvent/VK_P Event/CTRL_MASK) })
  button-panel (JPanel. (MigLayout. "", "[pref][pref]", "[grow]5"))
  observer (reify JscfiObserver 
      (something-changed [this] (SwingUtilities/invokeLater (fn []
	(let [task (get-task jscfi @task-id)]
	 (try
          (.setText name-field         (str (:name task)))
	  (.setText status-field       (str (:status task))) 
          (.setText outer-id-field     (str (:pbs-id task))) 
          (.setText source-file-field  (str (:source-file task)))
          (.setText input-file-field   (str (:input-file task))) 
          (.setText output-file-field  (str (:output-file task))) 
          (.setText node-count-field   (str (:node-count task)))
          (.setText walltime-field     (str (:walltime task)))
	  (catch Exception e (println e)))
	  )))))
  ]
  (doto frame 
   (.setSize 600 360)
   (.setContentPane panel)
   (.setTitle "Jscfi task")
   (.addWindowListener (proxy [WindowAdapter] [] (windowClosing [_] (remove-observer jscfi observer))))
   )
  (doto button-panel
   (.add (JButton. action-display) "growx")
   (.add (JButton. action-create) "growx")
   (.add (JButton. action-compile) "growx")
   (.add (JButton. action-upload) "growx,wrap")
   (.add (JButton. action-schedule) "growx")
   (.add (JButton. action-download) "growx")
   (.add (JButton. action-purge) "growx")
   (.add (JButton. action-remove) "growx")
   (.revalidate))
  (doto panel
   (.add (JLabel. "Name:"))          (.add name-field         "growx,wrap,span 2")
   (.add (JLabel. "Status:"))        (.add status-field       "growx,wrap,span 2")
   (.add (JLabel. "Task id:"))       (.add outer-id-field     "growx,wrap,span 2")
   (.add (JLabel. "Source file:"))   (.add source-file-field  "growx") 
				           (.add (create-file-chooser-button source-file-field :open) "wrap")
   (.add (JLabel. "Input file:"))    (.add input-file-field   "growx")
				           (.add (create-file-chooser-button input-file-field :open) "wrap")
   (.add (JLabel. "Output file:"))   (.add output-file-field  "growx")
				           (.add (create-file-chooser-button output-file-field :save) "wrap")
   (.add (JLabel. "Node count:"))    (.add node-count-field   "growx,wrap,span 2")
   (.add (JLabel. "Walltime:"))      (.add walltime-field     "growx,wrap,span 2")
   (.add button-panel "span 3")
   (.revalidate))
  (.setLocationRelativeTo frame nil)
  (add-observer jscfi observer)
  frame))
