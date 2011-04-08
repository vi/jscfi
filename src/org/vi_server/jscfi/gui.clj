(ns org.vi-server.jscfi.gui
    "GUI for Jscfi (main window)"
    (:use org.vi-server.jscfi.jscfi)
    (:use org.vi-server.jscfi.gui-common)
    (:use org.vi-server.jscfi.gui-task-window)
    (:use org.vi-server.jscfi.gui-authentication-window)
    (:import 
     (javax.swing JPanel JFrame JLabel JTextField JTextArea JButton SwingUtilities JList JScrollPane DefaultListModel AbstractAction Action KeyStroke)
     (javax.swing JMenu JMenuBar JPasswordField)
     (java.awt.event KeyEvent MouseAdapter WindowAdapter)
     (java.awt Event)
     (net.miginfocom.swing MigLayout))
    (:import (java.util TimerTask Timer)))

(defprotocol TaskListEntry (task [this]))
(deftype TaskListEntryImpl [task] TaskListEntry
    (toString [this] (format "%s    %s    %s    %s" (:status task) (:source-file task) (:name task) (:outer-id task)))
    (task [this] task))

(defn create-main-window [jscfi] 
 (let [
  panel (JPanel. (MigLayout. "", "[grow][pref][pref]", "[grow]"))
  frame (JFrame.)
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
  view-menu (JMenu. "View")
  action-menu (JMenu. "Action")
  observer (reify JscfiObserver 
      (connected [this] (.setVisible frame true))
      (compilation-failed [this task message] (javax.swing.JOptionPane/showMessageDialog nil 
				message (:name task) javax.swing.JOptionPane/INFORMATION_MESSAGE))
      (something-changed [this] (SwingUtilities/invokeLater (fn [](.actionPerformed action-refresh nil))))
  )
  ]
  (doto frame 
   (.setSize 600 400)
   (.setContentPane panel)
   (.setJMenuBar menubar)
   (.setLocationRelativeTo nil)
   (.setTitle "Jscfi - SuperComputer Phoenix Initiative GUI"))
  (doto panel
   (.add (JScrollPane. jlist) "grow,span 3")
   (.revalidate))
  (.add view-menu    action-refresh)
  (.add view-menu    action-update)
  (.add view-menu    action-open)
  (.add action-menu    action-create)
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


