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
 :compilation-failed #{:compile :purge},
 :purged       #{:compile :remove},
 :compiled     #{:compile :schedule :upload :purge},
 :aborted      #{:compile :schedule :upload :purge :download},
 :scheduled    #{:cancel},
 :running      #{:nodes-stats, :terminate},
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
    {:label "Node count/list:",   :type :textfield, :tf :node-count,
            :regex #"^([0-9]{1,10}(:ppn=[1-8])?)|(nopbs:[0-9_1\-\.a-zA-Z]+(?:,[0-9_\-\.a-zA-Z]+)*)$"},
            #_ "       node count                node list  "
    {:label "Walltime:",     :type :textfield, :tf :walltime, :regex #"^\d\d:\d\d:\d\d$"},
    {:label "Add-al cmdpars:",     :type :textfield, :tf :cmdadd},
    {:label "Last run time:", :type :label, :tf :last-timing},
    {:label "Completed date:", :type :label, :tf :completed-date},
  ]
  fields-to-update-when-reread-tasks [:name :status :pbs-id :source-file :input-file 
         :output-file :node-count :walltime :source-mode :last-timing :completed-date :cmdadd]
  fields-to-consider-in-save-task [:name :source-file :source-mode :input-file 
         :output-file :node-count :walltime :cmdadd]
  tooltips {
    :name "The name of this task, as will be reported tp PBS"
    :status "<html>
        Status of the task:<br/>
        :created - freshly created<br/>
        :compiled - successfully compiled<br/>
        :compilation-failed<br/>
        :sceduled - currently in PBS queue (or just recently added)<br/>
        :running - currently executing<br/>
        :completed - Finished executing (may be failed)<br/>
        :aborted - Aborted execution<br/>
        :purged - Files for this task have been deleted from server<br/>
        </html>"
    :pbs-id "ID of this task in PBS (or PID on server in nobps mode)"
    :source-file "File or directory to upload to server and compile (depends on source-mode)"
    :source-mode "<html>Mode of compilation and running:<br/>
         :single-c-file - Single C MPI file<br/>
         :single-cpp-file - Single C++ MPI file<br/>
         :directory-with-makefile - Directory with C/C++ MPI files and Makefile to build program<br/>
         :directory-with-makefile-make-run - Directory with C/C++ [MPI] files and Makefile with \"run\" target<br/>
            &nbsp;&nbsp;The \"run\" target should not be the first<br/>
         :single-lammps-file - Single file, input to LAMMPS<br/>
         :directory-with-lammps-file - Directory with \"input.txt\" file (input for LAMMPS) that can depend on other files in it<br/>
         </html>"
    :input-file "<html>Local path for input file or directory (will be accessible as 
            \"input.txt\" (even directory) for target program
            <br/>For LAMMPS it will be merged into the source directory</html>"
    :output-file "Local path for output file (or directory) to download from server (from \"output.txt\")"
    :node-count "<html>
        Either node count (possibly with \":ppn={number}\" appended) or node list (sets \"nopbs\" mode)<br/>
        Examples:<br/>
        3 - Three nodes, one (?) process per node<br/>
        3:ppn=4 - Three nodes, 4 processes per node (total 12 instances of the application)<br/>
        nopbs:node-040,node-041 - Start application without PBS, two instances on nodes node-040 and node-041.
        </html>"
    :walltime "Time this application is allowed to run (observed by PBS)"
    :cmdadd "<html>Additional command-line parameters<br/>
        For most modes it is additional parameters to \"mpirun\" command<br/>
        For :directory-with-makefile-make-run mode it is additional parameters for \"make run\" command<br/>
        For LAMMPS modes it is additional parameter for lmp
        </html>"
    :last-timing "<html>Time (in seconds) of the last run attempt.<br/>
        If failed (exit code > 0) then \"Command exited with non-zero status {number};\" is prepended.</html>"
    :completed-date "Date and time (on server) of the last task finish (successfull or not)"
  }

  fields2 (into {} (map (fn[x] [(:tf x)
   (let [
        v (get task (:tf x)),
        l (JLabel. (:label x)),
        _ (.setToolTipText l (get tooltips (:tf x) ""))
        ] (case (:type x)
    :label                                                           
	(let [c (JLabel. (str v))] 
	 {:info x, :label l, :widget c, :get #(.getText c), :set #(.setText c (str %)),
	 :adder (fn[panel onch]
	    (.add panel l)
	    (.add panel c "growx,wrap,span 2"))
	 })
    :textfield 
	(let [c (JTextField. (str v))]     
         {:info x, :label l, :widget c, :get #(.getText c), :set #(.setText c (str %))
	 :adder (fn[panel on-changed]
	    (.add panel l)
	    (if (:file x)
	     (do
	      (.add panel c "growx") 
	      (.add panel (create-file-chooser-button c (:file x)) "wrap"))
	     (.add panel c "growx,wrap,span 2"))
        (.addDocumentListener (.getDocument c) (proxy [javax.swing.event.DocumentListener] [] 
                                                (changedUpdate [e] (on-changed))
                                                (insertUpdate [e] (on-changed))
                                                (removeUpdate [e] (on-changed))
                                                ))
        )
	 })
    :combobox                    
	(let [c (combobox-create (:set x))] 
	 (combobox-set c v)
	 {:info x, :label l, :widget (combobox-field c), :get #(combobox-get c), :set #(combobox-set c %)
	 :adder (fn[panel on-changed]
	    (.add panel l)
	    (.add panel (combobox-field c) "growx,wrap,span 2")
        (.addItemListener (combobox-field c) (proxy [java.awt.event.ItemListener] []
                                              (itemStateChanged [event] (on-changed))))
       )
	 })
   ))]) fields))
  task-id (atom (:id task))

  action-display (create-action "debugpr" (fn [_] (prn (get-task jscfi @task-id)))
      { Action/SHORT_DESCRIPTION  "Display contents of internal task structure to stdout"})

  action-create (create-action "Create/Change" (fn [_] 
	  (try (let [
	   task (if @task-id (get-task jscfi @task-id) {})
           newtask (into task (map (fn[x] [x ((:get (get fields2 x)))])
	    fields-to-consider-in-save-task))]
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
  
  reread-task-info2 (atom nil)          #_ "Will be filled in with reread-task-info function defined below"
  action-revert (create-action "Undo" (fn [_] (@reread-task-info2))
      { Action/SHORT_DESCRIPTION  "Revert edits to the task"})

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
  
  action-download-all (create-action "Download all" (fn [_] (download-all-task jscfi @task-id))
      { Action/SHORT_DESCRIPTION  "Download all task files"})

  action-purge (create-action "Purge" (fn [_] (purge-task jscfi @task-id))
      { Action/SHORT_DESCRIPTION  "Remove task files from server"})

  action-cancel (create-action "Cancel" (fn [_] (cancel-task jscfi @task-id))
      { Action/SHORT_DESCRIPTION  "Call \"qdel\" to unschedule the task"})

  action-nodesstats (create-action "Nodes stats" (fn [_] (nodes-stats jscfi @task-id))
      { Action/SHORT_DESCRIPTION  "Show info about nodes the task is running on"})
  
  action-terminate (create-action "Terminate task" (fn [_] (terminate-task jscfi @task-id))
      { Action/SHORT_DESCRIPTION  "Kill all user's processes on nodes and mpirun"})
  
  button-panel (JPanel. (MigLayout. "", "[pref][pref]", "[grow]5"))
  buttons {
   :display (JButton. action-display),
   :create (JButton. action-create),
   :compile (JButton. action-compile),
   :upload (JButton. action-upload),
   :schedule (JButton. action-schedule),
   :download (JButton. action-download),
   :download-all (JButton. action-download-all),
   :purge (JButton. action-purge),
   :remove (JButton. action-remove),
   :cancel (JButton. action-cancel),
   :nodes-stats (JButton. action-nodesstats),
   :revert (JButton. action-revert),
   :terminate (JButton. action-terminate),
   }
  update-ui-traits (fn[] "Updates various things like changed/not-changed or enabled/disabled things"
   (if @task-id
    (do 
     (.setLabel (:create buttons) "Save changes")
     (let [
      ts (get button-enabledness-per-status (:status (get-task jscfi @task-id)))
      ts2 (if ts ts #{:compile :download :cancel :upload :schedule :purge :remove :nodes-stats :terminate})
      ]
      (->> [:compile :download :cancel :upload :schedule :purge :remove :nodes-stats :revert :terminate] 
       (map #(.setEnabled (% buttons) (contains? ts2 %))) 
       (doall))
     )
     ;(doall (map (fn[[i x]] (.setText (:label x) (:label (:info x)))) fields2))
     (.setTitle frame (:name (get-task jscfi @task-id)))
    )
    (do 
     (.setLabel (:create buttons) "Create")
     (->> [:compile :upload :schedule :download :purge :remove :terminate] (map #(.setEnabled (% buttons) false)) (doall))
    )
   ))
  reread-task-info (fn []
	(let [task (get-task jscfi @task-id)]
	 (try
	  (doall (map #((:set (get fields2 %)) (get task %)) 
	    fields-to-update-when-reread-tasks))
	  (update-ui-traits)
	  (catch Exception e (println "pln2" e)))
  ))
  _ (swap! reread-task-info2 (fn[_] reread-task-info)) #_ "Fill in reread-task-info2 above for action-revert"
  disable-buttons-per-edit (fn[]
    (try
        (doall (map #(.setEnabled (% buttons) false) [:compile :download :cancel :upload :schedule :purge :remove]))
        (doall (map #(.setEnabled (% buttons) true)  [:revert]))
        (catch Exception e (println "dbpe Exc: " e))
    )
  )

  observer (reify JscfiObserver 
      (something-changed [this] (SwingUtilities/invokeLater reread-task-info)))
  ]
  (doto frame 
   (.setSize 600 470)
   (.setContentPane panel)
   (.setTitle "Jscfi task")
   (.addWindowListener (proxy [WindowAdapter] [] (windowClosing [_] (remove-observer jscfi observer))))
   )
  (doto button-panel
   (.add (:display buttons) "growx")
   (.add (:create buttons) "growx")
   (.add (:compile buttons) "growx")
   (.add (:upload buttons) "growx")
   (.add (:schedule buttons) "growx,wrap")
   (.add (:download buttons) "growx")
   (.add (:purge buttons) "growx")
   (.add (:remove buttons) "growx")
   (.add (:cancel buttons) "growx")
   (.add (:nodes-stats buttons) "growx,wrap")
   (.add (:revert buttons) "growx")
   (.add (:terminate buttons) "growx")
   (.add (:download-all buttons) "growx")
   (.revalidate))
   (try 
   (doall (map (fn[x] ((:adder (get fields2 (:tf x))) panel disable-buttons-per-edit)) fields))
   (catch Throwable e (println e)))
  (doto panel  
   (.add button-panel "span 3")
   (.revalidate))
  (.setLocationRelativeTo frame nil)
  (add-observer jscfi observer)
  (update-ui-traits)
  frame))
