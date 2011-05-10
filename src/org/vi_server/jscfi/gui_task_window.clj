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
  fields [
    {:label "Name:",         :type :textfield,      :tf :name, :regex #"^[a-zA-Z0-9_]{1,32}$"},
    {:label "Status:",       :type :label,          :tf :status},
    {:label "Task id:",      :type :label,          :tf :pbs-id},
    {:label "Source file:",  :type :textfield-file, :tf :source-file},
    {:label "Source mode:",  :type :combobox,       :tf :source-mode, :set (get-source-modes jscfi)},
    {:label "Input file:",   :type :textfield-file, :tf :input-file},
    {:label "Output file:",  :type :textfield-file, :tf :output-file},
    {:label "Node count:",   :type :textfield,      :tf :node-count, :regex #"^[0-9]{1,10}$"},
    {:label "Walltime:",     :type :textfield,      :tf :walltime, :regex #"^\d\d:\d\d:\d\d$"},
  ]
  fields2 (into {} (map (fn[x] [(:tf x) (let [v (get task (:tf x))] (case (:type x)
   :label                                                           
	(let [c (JLabel. (str v))] 
	 {:widget c, :get #(.getText c), :set #(.setText c (str %))})
   (:textfield :textfield-file) 
	(let [c (JTextField. (str v))]     
         {:widget c, :get #(.getText c), :set #(.setText c (str %))})
   :combobox                    
	(let [c (combobox-create (:set x))] 
	 (combobox-set c v)
	 {:widget (combobox-field c), :get #(combobox-get c), :set #(combobox-set c %)})
   ))]) fields))
  task-id (atom (:id task))
  update-ui-traits (fn[] "Updates various things like changed/not-changed or enabled/disabled things"
   (if @task-id
    nil
    nil
    )
   )
  action-display (create-action "Debug Print" (fn [_] (prn (get-task jscfi @task-id)))
      { Action/SHORT_DESCRIPTION  "Display it", Action/ACCELERATOR_KEY (KeyStroke/getKeyStroke KeyEvent/VK_L Event/CTRL_MASK) })
  action-create (create-action "Create/Change" (fn [_] 
	  (try (let [
	   task (if @task-id (get-task jscfi @task-id) {})
           newtask (into task (map (fn[x] [x ((:get (get fields2 x)))])
	    [:name :source-file :source-mode :input-file :output-file :node-count :walltime]))]
	   (println "QQQ " task " QQ " newtask)
	   (try
	    (doall (map 
	     #(when (:regex %) (when-not (re-find (:regex %) (get newtask (:tf %)))
	      (throw (Exception. (str (:label %) " must match" (:regex %)))))) fields))
	    (if @task-id
	     (alter-task jscfi newtask)
	     (swap! task-id (fn[_](register-task jscfi newtask))))
	  (catch Exception e (println "pln1" e) (msgbox (str e))))) (catch Throwable e (println "pln5" e))))
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
	  (doall (map #((:set (get fields2 %)) (get task %)) 
	    [:name :status :pbs-id :source-file :input-file :output-file :node-count :walltime :source-mode]))
	  (catch Exception e (println "pln2" e)))
	  )))))
  ]
  (doto frame 
   (.setSize 600 380)
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
   (.add (JLabel. "Name:"))          (.add (:widget (:name fields2))         "growx,wrap,span 2")
   (.add (JLabel. "Status:"))        (.add (:widget (:status fields2))       "growx,wrap,span 2")
   (.add (JLabel. "Task id:"))       (.add (:widget (:pbs-id fields2))       "growx,wrap,span 2")
   (.add (JLabel. "Source file:"))   (.add (:widget (:source-file fields2))  "growx") 
				           (.add (create-file-chooser-button (:widget (:source-file fields2)) :open) "wrap")
   (.add (JLabel. "Source mode:"))   (.add (:widget (:source-mode fields2))  "growx,wrap,span 2")
   (.add (JLabel. "Input file:"))    (.add (:widget (:input-file fields2))   "growx")
				           (.add (create-file-chooser-button (:widget (:input-file fields2)) :open) "wrap")
   (.add (JLabel. "Output file:"))   (.add (:widget (:output-file fields2))  "growx")
				           (.add (create-file-chooser-button (:widget (:output-file fields2)) :save) "wrap")
   (.add (JLabel. "Node count:"))    (.add (:widget (:node-count fields2))   "growx,wrap,span 2")
   (.add (JLabel. "Walltime:"))      (.add (:widget (:walltime fields2))     "growx,wrap,span 2")
   (.add button-panel "span 3")
   (.revalidate))
  (.setLocationRelativeTo frame nil)
  (add-observer jscfi observer)
  frame))
