(ns org.vi-server.jscfi.gui
    "GUI for Jscfi"
    (:use org.vi-server.jscfi.jscfi)
    (:use org.vi-server.jscfi.fake)
    (:import 
     (javax.swing JPanel JFrame JTextField JTextArea JButton SwingUtilities JList JScrollPane DefaultListModel AbstractAction Action KeyStroke)
     (java.awt.event KeyEvent)
     (java.awt Event)
     (net.miginfocom.swing MigLayout)))      

(defn create-action "Creates an implementation of AbstractAction." [name behavior options]
 (let [
  action (proxy [AbstractAction] [name]
      (actionPerformed [event] (behavior event)))]
  (if options
   (doseq [key (keys options)] (.putValue action key (options key))))
  action))

(defprotocol TaskListEntry (task [this]))
(deftype TaskListEntryImpl [task] TaskListEntry
    (toString [this] (format "%s    %s    %s    %s" (:status task) (:source-file task) (:name task) (:task-id task)))
    (task [this] task))

(defn create-main-window [] 
 (let [
  jscfi (connect (get-fake-jscfi-factory) nil "scfi" "test")
  panel (JPanel. (MigLayout. "", "[grow][pref][pref]", "[pref][grow]"))
  frame (JFrame.)
  text-field (JTextField.)
  list-model (DefaultListModel.)
  jlist (JList. list-model)
  action-add (create-action "Add" (fn [_] (.add list-model 0 (TaskListEntryImpl. {:name (.getText text-field)})))
      { Action/SHORT_DESCRIPTION  "Add new entry to the list", Action/ACCELERATOR_KEY (KeyStroke/getKeyStroke KeyEvent/VK_L Event/CTRL_MASK) })
  action-display (create-action "Display" (fn [_] (let [index (.getSelectedIndex jlist)] (when (not= index -1) (prn (task (.elementAt list-model index))))))
      { Action/SHORT_DESCRIPTION  "Add new entry to the list", Action/ACCELERATOR_KEY (KeyStroke/getKeyStroke KeyEvent/VK_L Event/CTRL_MASK) })
  ]
  (doto frame 
   (.setSize 600 400)
   (.setContentPane panel))
  (doto panel
   (.add text-field "growx")
   (.add (JButton. action-add) )
   (.add (JButton. action-display) "wrap")
   (.add (JScrollPane. jlist) "grow")
   (.revalidate))
  (doall (map #(.add list-model 0 (TaskListEntryImpl. %)) (get-tasks jscfi)))  
  frame))

(defn create-and-show-window []
 (let [frame (create-main-window)]
  (.setLocationRelativeTo frame nil)
  (.setVisible frame true) 
  frame))

(defn main []
 (SwingUtilities/invokeLater create-and-show-window))


