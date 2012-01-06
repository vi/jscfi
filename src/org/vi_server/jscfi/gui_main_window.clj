(ns org.vi-server.jscfi.gui-main-window
  "GUI for Jscfi (main window)"
  (:use
    [clojure.tools.logging :only [info warn info debug]]
    [org.vi-server.jscfi.gui-authentication-window
     :only
     [create-authentication-window]]
    [org.vi-server.jscfi.gui-common
     :only
     [create-action
      exit-if-needed
      get-monitoring-output
      jscfi-version
      msgbox]]
    [org.vi-server.jscfi.gui-settings-window
     :only
     [create-settings-window]]
    [org.vi-server.jscfi.gui-log-window
     :only
     [create-log-window]]
    [org.vi-server.jscfi.gui-task-window :only [create-task-window]]
    [org.vi-server.jscfi.jscfi
     :only
     [add-observer
      check-nodes-loadavg
      check-your-nodes
      close-connection
      debug-print
      get-tasks
      monitor-nodes
      periodic-update
      read-your-nodes]])
  (:import
    (java.awt Event)
    (java.awt.event KeyEvent MouseAdapter WindowAdapter)
    (java.util Timer TimerTask)
    (javax.swing
      Action
      DefaultListModel
      JFrame
      JList
      JMenu
      JMenuBar
      JPanel
      JScrollPane
      JTextArea
      KeyStroke
      SwingUtilities)
    (org.vi_server.jscfi.jscfi JscfiObserver)))


(def main-window-size {:width 600, :height 400 })
(def textinfo-window-size {:width 600, :height 400 })


(defprotocol TaskListEntry (task [this]))
(deftype TaskListEntryImpl [task] TaskListEntry
    (toString [this] (format "%s    %s    %s    %s" (:status task) (:source-file task) (:name task) (:pbs-id task)))
    (task [this] task))

(defn create-textinfo-window [text]
 (let [
  panel (JPanel. (net.miginfocom.swing.MigLayout. "", "[grow]", "[grow]"))
  frame (JFrame.)
  textarea (JTextArea.)
  ]
  (doto frame 
   (.setSize (:width textinfo-window-size) (:height textinfo-window-size))
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
  panel (JPanel. (net.miginfocom.swing.MigLayout. "", "[grow][pref][pref]", "[grow]"))
  frame (JFrame.)
  timer (Timer.)
  list-model (DefaultListModel.)
  jlist (JList. list-model)
  action-create (create-action "Create task" (fn [_] (.setVisible (create-task-window 
		  {:name "Untitled", :node-count "1", :walltime "00:01:00", :source-mode :single-c-file} jscfi) true))
      { Action/SHORT_DESCRIPTION  "Open window for task creation", Action/ACCELERATOR_KEY (KeyStroke/getKeyStroke KeyEvent/VK_N Event/CTRL_MASK) })
  action-refresh (create-action "Refresh" (fn [_] 
	  (.clear list-model)
	  (doall (map #(.add list-model 0 (TaskListEntryImpl. %)) (get-tasks jscfi))))
      { Action/SHORT_DESCRIPTION  "Refresh the list of tasks",  })
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
        ]
        (when (not (empty? node-list))
        (monitor-nodes jscfi node-list (get-monitoring-output)))
        )
       )
      { Action/SHORT_DESCRIPTION  "Enter the list of nodes to start monitoring on"})
  action-restart (create-action "Restart Jscfi" 
      (fn [_] 
        (info "Closing conenction")
        (close-connection jscfi)
        (info "Closing window")
        (.cancel timer)
        (.dispose frame)
        (info "Restarting")
        ((resolve 'org.vi-server.jscfi.main2/-main))
       )
      { Action/SHORT_DESCRIPTION  "Restart Jscfi (and reload user-specified source code)"
        Action/ACCELERATOR_KEY (KeyStroke/getKeyStroke KeyEvent/VK_R Event/CTRL_MASK)})
  action-log (create-action "Show log" 
      (fn [_] 
       (.setVisible (create-log-window) true)
       )
      { Action/SHORT_DESCRIPTION  "Show messages (like on console)"
        Action/ACCELERATOR_KEY (KeyStroke/getKeyStroke KeyEvent/VK_L Event/CTRL_MASK)})
  menubar (JMenuBar.)
  view-menu (JMenu. "View")
  action-menu (JMenu. "Action")
  debug-menu (JMenu. "Debug")
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
   (.setSize (:width main-window-size) (:height main-window-size))
   (.setContentPane panel)
   (.setJMenuBar menubar)
   (.setLocationRelativeTo nil)
   (.setTitle (format "Jscfi v%s - SuperComputer Phoenix Initiative GUI", jscfi-version)))
  (doto panel
   (.add (JScrollPane. jlist) "grow,span 3")
   (.revalidate))
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
  
  (.add debug-menu action-debug-print)
  (.add debug-menu action-refresh)
  (.add debug-menu action-update)
  (.addSeparator debug-menu)
  (.add debug-menu action-log)
  (.addSeparator debug-menu)
  (.add debug-menu action-restart)
  (doto menubar
   (.add action-menu)
   (.add view-menu)
   (.add debug-menu))
  (add-observer jscfi observer)
  (.setVisible (create-authentication-window jscfi) true)
  (.addMouseListener jlist (proxy [MouseAdapter] [] (mouseClicked [event] (when (= (.getClickCount event) 2) (.actionPerformed action-open nil)))))
  (.addWindowListener frame (proxy [WindowAdapter] [] (windowClosing [_] (exit-if-needed))))
  (let [task (proxy [TimerTask] []
      	(run [] (periodic-update jscfi)))]
   (. timer (schedule task (long 3000) (long 5000))))
  frame))

(defn gui-main [jscfi-engine]
 (SwingUtilities/invokeLater (fn [](create-main-window jscfi-engine))))


