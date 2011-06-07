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

(def button-enabledness-per-status {
 :created      #{:compile :remove},
 :purged       #{:compile :remove},
 :compiled     #{:compile :schedule :upload :purge},
 :aborted      #{:compile :schedule :upload :purge :download},
 :scheduled    #{:cancel},
 :running      #{:nodes-stats},
 :completed    #{:compile :download :upload :schedule :purge}, 
})

(defn create-task-window [task jscfi]
 (let [
  panel (JPanel. (MigLayout. "", "[pref][grow][pref]", "[pref]7[grow]"))
  frame (JFrame.)
  fields [
    {:label "Name:",         :type :textfield, :tf :name, :regex #"^[a-zA-Z0-9_]{1,32}$"},
    {:label "Status:",       :type :label,     :tf :status},
    {:label "Task id:",      :type :label,     :tf :pbs-id},
    {:label "Source file:",  :type :textfield, :tf :source-file, :file :open},
    {:label "Source mode:",  :type :combobox,  :tf :source-mode, :set (get-source-modes jscfi)},
    {:label "Input file:",   :type :textfield, :tf :input-file, :file :open},
    {:label "Output file:",  :type :textfield, :tf :output-file, :file :save},
    {:label "Node count:",   :type :textfield, :tf :node-count, :regex #"^[0-9]{1,10}(:ppn=[1-8])?$"},
    {:label "Walltime:",     :type :textfield, :tf :walltime, :regex #"^\d\d:\d\d:\d\d$"},
  ]
  fields2 (into {} (map (fn[x] [(:tf x)
   (let [v (get task (:tf x)), l (JLabel. (:label x))] (case (:type x)
    :label                                                           
	(let [c (JLabel. (str v))] 
	 {:info x, :label l, :widget c, :get #(.getText c), :set #(.setText c (str %)),
	 :adder (fn[panel]
	    (.add panel l)
	    (.add panel c "growx,wrap,span 2"))
	 })
    :textfield 
	(let [c (JTextField. (str v))]     
         {:info x, :label l, :widget c, :get #(.getText c), :set #(.setText c (str %))
	 :adder (fn[panel]
	    (.add panel l)
	    (if (:file x)
	     (do
	      (.add panel c "growx") 
	      (.add panel (create-file-chooser-button c (:file x)) "wrap"))
	     (.add panel c "growx,wrap,span 2")))
	 })
    :combobox                    
	(let [c (combobox-create (:set x))] 
	 (combobox-set c v)
	 {:info x, :label l, :widget (combobox-field c), :get #(combobox-get c), :set #(combobox-set c %)
	 :adder (fn[panel]
	    (.add panel l)
	    (.add panel (combobox-field c) "growx,wrap,span 2"))
	 })
   ))]) fields))
  task-id (atom (:id task))

  action-display (create-action "Debug Print" (fn [_] (prn (get-task jscfi @task-id)))
      { Action/SHORT_DESCRIPTION  "Display it"})

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
      { Action/SHORT_DESCRIPTION  "Create/change a task"})

  action-remove (create-action "Remove" (fn [_] (remove-task jscfi @task-id) (swap! task-id (fn[_]nil)))
      { Action/SHORT_DESCRIPTION  "Remove the task"})

  action-compile (create-action "Compile" (fn [_] (compile-task jscfi @task-id))
      { Action/SHORT_DESCRIPTION  "Upload the source code and compile the task"})

  action-upload (create-action "Upload" (fn [_] (upload-task jscfi @task-id))
      { Action/SHORT_DESCRIPTION  "Upload the input data as input.txt"})

  action-schedule (create-action "Schedule" (fn [_] (schedule-task jscfi @task-id))
      { Action/SHORT_DESCRIPTION  "Schedule the task for execution"})

  action-download (create-action "Download" (fn [_] (download-task jscfi @task-id))
      { Action/SHORT_DESCRIPTION  "Download output.txt"})

  action-purge (create-action "Purge" (fn [_] (purge-task jscfi @task-id))
      { Action/SHORT_DESCRIPTION  "Remove task files from server"})

  action-cancel (create-action "Cancel" (fn [_] (cancel-task jscfi @task-id))
      { Action/SHORT_DESCRIPTION  "Call \"qdel\" to unschedule the task"})

  action-nodesstats (create-action "Nodes stats" (fn [_] (nodes-stats jscfi @task-id))
      { Action/SHORT_DESCRIPTION  "Show info about nodes the task is running on"})

  button-panel (JPanel. (MigLayout. "", "[pref][pref]", "[grow]5"))
  buttons {
   :display (JButton. action-display),
   :create (JButton. action-create),
   :compile (JButton. action-compile),
   :upload (JButton. action-upload),
   :schedule (JButton. action-schedule),
   :download (JButton. action-download),
   :purge (JButton. action-purge),
   :remove (JButton. action-remove),
   :cancel (JButton. action-cancel),
   :nodes-stats (JButton. action-nodesstats),
   }
  update-ui-traits (fn[] "Updates various things like changed/not-changed or enabled/disabled things"
   (if @task-id
    (do 
     (.setLabel (:create buttons) "Save changes")
     (let [
      ts (get button-enabledness-per-status (:status (get-task jscfi @task-id)))
      ts2 (if ts ts #{:compile :download :cancel :upload :schedule :purge :remove :nodes-stats})
      ]
      (->> [:compile :download :cancel :upload :schedule :purge :remove :nodes-stats] 
       (map #(.setEnabled (% buttons) (contains? ts2 %))) 
       (doall))
     )
     ;(doall (map (fn[[i x]] (.setText (:label x) (:label (:info x)))) fields2))
    )
    (do 
     (.setLabel (:create buttons) "Create")
     (->> [:compile :upload :schedule :download :purge :remove] (map #(.setEnabled (% buttons) false)) (doall))
    )
   ))
  observer (reify JscfiObserver 
      (something-changed [this] (SwingUtilities/invokeLater (fn []
	(let [task (get-task jscfi @task-id)]
	 (try
	  (doall (map #((:set (get fields2 %)) (get task %)) 
	    [:name :status :pbs-id :source-file :input-file :output-file :node-count :walltime :source-mode]))
	  (update-ui-traits)
	  (catch Exception e (println "pln2" e)))
	  )))))
  ]
  (doto frame 
   (.setSize 600 400)
   (.setContentPane panel)
   (.setTitle "Jscfi task")
   (.addWindowListener (proxy [WindowAdapter] [] (windowClosing [_] (remove-observer jscfi observer))))
   )
  (doto button-panel
   (.add (:display buttons) "growx")
   (.add (:create buttons) "growx")
   (.add (:compile buttons) "growx")
   (.add (:upload buttons) "growx,wrap")
   (.add (:schedule buttons) "growx")
   (.add (:download buttons) "growx")
   (.add (:purge buttons) "growx")
   (.add (:remove buttons) "growx,wrap")
   (.add (:cancel buttons) "growx")
   (.add (:nodes-stats buttons) "growx")
   (.revalidate))
   (try 
   (doall (map (fn[x] ((:adder (get fields2 (:tf x))) panel)) fields))
   (catch Throwable e (println e)))
  (doto panel  
   (.add button-panel "span 3")
   (.revalidate))
  (.setLocationRelativeTo frame nil)
  (add-observer jscfi observer)
  (update-ui-traits)
  frame))
