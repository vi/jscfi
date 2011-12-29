(ns org.vi-server.jscfi.gui-main-window
    "GUI for Jscfi (main window)"
    (:use org.vi-server.jscfi.jscfi)
    (:use org.vi-server.jscfi.gui-common)
    (:use org.vi-server.jscfi.gui-task-window)
    (:use org.vi-server.jscfi.gui-authentication-window)
    (:use org.vi-server.jscfi.gui-settings-window)
    (:import 
     (javax.swing JPanel JFrame JLabel JTextField JTextArea JButton SwingUtilities JList JScrollPane DefaultListModel AbstractAction Action KeyStroke)
     (javax.swing JMenu JMenuBar JPasswordField)
     (java.awt.event KeyEvent MouseAdapter WindowAdapter)
     (java.awt Event)
     (net.miginfocom.swing MigLayout))
    (:import (java.util TimerTask Timer)))

(defprotocol TaskListEntry (task [this]))
(deftype TaskListEntryImpl [task] TaskListEntry
    (toString [this] (format "%s    %s    %s    %s" (:status task) (:source-file task) (:name task) (:pbs-id task)))
    (task [this] task))

(defn create-textinfo-window [text]
 (let [
  panel (JPanel. (MigLayout. "", "[grow]", "[grow]"))
  frame (JFrame.)
  textarea (JTextArea.)
  ]
  (doto frame 
   (.setSize 600 400)
   (.setContentPane panel)
   (.setLocationRelativeTo nil)
   (.setTitle "Text info"))
  (.setText textarea text)
  (doto panel
   (.add (JScrollPane. textarea) "grow")
   (.revalidate))
  frame))

(defn create-main-window [jscfi] 
 (let [
  panel (JPanel. (MigLayout. "", "[grow][pref][pref]", "[grow]"))
  frame (JFrame.)
  list-model (DefaultListModel.)
  jlist (JList. list-model)
  action-create (create-action "Create task" (fn [_] (.setVisible (create-task-window 
		  {:name "Untitled", :node-count "1", :walltime "00:01:00", :source-mode :single-c-file} jscfi) true))
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
  action-debug-print (create-action "Debug print" 
      (fn [_] 
       (debug-print jscfi))
      { Action/SHORT_DESCRIPTION  "Print debugging information", Action/ACCELERATOR_KEY (KeyStroke/getKeyStroke KeyEvent/VK_P Event/CTRL_MASK) })
  action-settings (create-action "Settings" 
      (fn [_] 
       (.show (create-settings-window)))
      { Action/SHORT_DESCRIPTION  "Show settings window"})
  action-read-your-nodes (create-action "Read your-nodes" 
      (fn [_] (read-your-nodes jscfi))
      { Action/SHORT_DESCRIPTION  "Output $HOME/your-nodes file"})
  action-check-your-nodes (create-action "Check your nodes" 
      (fn [_] (check-your-nodes jscfi))
      { Action/SHORT_DESCRIPTION  "Run /share/check-your-nodes script"})
  action-check-nodes-loadavg (create-action "Show nodes load average" 
      (fn [_] (check-nodes-loadavg jscfi))
      { Action/SHORT_DESCRIPTION  "Traverse nodes in your-nodes file and show load average"})
  action-start-manual-monitoring (create-action "Set up manual monitoring" 
      (fn [_] 
       (let [
        node-list (javax.swing.JOptionPane/showInputDialog nil "Enter the comma-separated list of nodes to monitor")
        ])
       )
      { Action/SHORT_DESCRIPTION  "Enter the list of nodes to start monitoring on"})
  menubar (JMenuBar.)
  view-menu (JMenu. "View")
  action-menu (JMenu. "Action")
  observer (reify JscfiObserver 
      (connected [this] (.setVisible frame true))
      (compilation-failed [this task text] 
	   (msgbox (str (:name task) "\n" text)))
      (message [this task text]
	   (msgbox (str (:name task) "\n" text)))
      (text-info [this task message] 
	   (.setVisible (create-textinfo-window (str "Text info for task " (:name task) ":\n\n" message)) true))
      (something-changed [this] (SwingUtilities/invokeLater (fn [](.actionPerformed action-refresh nil))))
  )
  ]
  (doto frame 
   (.setSize 600 400)
   (.setContentPane panel)
   (.setJMenuBar menubar)
   (.setLocationRelativeTo nil)
   (.setTitle (format "Jscfi v%s - SuperComputer Phoenix Initiative GUI", jscfi-version)))
  (doto panel
   (.add (JScrollPane. jlist) "grow,span 3")
   (.revalidate))
  (.add view-menu    action-refresh)
  (.add view-menu    action-update)
  (.addSeparator view-menu)
  (.add view-menu    action-open)
  (.addSeparator view-menu)
  (.add view-menu    action-settings)

  (.add action-menu    action-create)
  (.addSeparator action-menu)
  (.add action-menu    action-start-manual-monitoring)
  (.addSeparator action-menu)
  (.add action-menu    action-read-your-nodes)
  (.add action-menu    action-check-your-nodes)
  (.add action-menu    action-check-nodes-loadavg)
  (.addSeparator action-menu)
  (.add action-menu    action-debug-print)
  (doto menubar
   (.add action-menu)
   (.add view-menu))
  (add-observer jscfi observer)
  (.setVisible (create-authentication-window jscfi) true)
  (.addMouseListener jlist (proxy [MouseAdapter] [] (mouseClicked [event] (when (= (.getClickCount event) 2) (.actionPerformed action-open nil)))))
  (.addWindowListener frame (proxy [WindowAdapter] [] (windowClosing [_] (exit-if-needed))))
  (let [task (proxy [TimerTask] []
      	(run [] (periodic-update jscfi)))]
   (. (new Timer) (schedule task (long 3000) (long 5000))))
  frame))

(defn gui-main [jscfi-engine]
 (SwingUtilities/invokeLater (fn [](create-main-window jscfi-engine))))


