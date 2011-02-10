(ns org.vi-server.jscfi.gui
    "GUI for Jscfi"
    (:use org.vi-server.jscfi.jscfi)
    (:use org.vi-server.jscfi.fake)
    (:import 
     (javax.swing JPanel JFrame JLabel JTextField JTextArea JButton SwingUtilities JList JScrollPane DefaultListModel AbstractAction Action KeyStroke)
     (java.awt.event KeyEvent MouseAdapter)
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

(defn create-task-window [task jscfi]
 (let [
  panel (JPanel. (MigLayout. "", "[pref][grow]", "[pref]7[grow]"))
  frame (JFrame.)
  name-field        (JTextField.   (:name task))
  status-field      (JLabel.       (str (:status task)))
  task-id-field     (JLabel.       (:task-id task))
  source-file-field (JTextField.   (:source-file task))
  input-file-field  (JTextField.   (:input-file task))
  output-file-field (JTextField.   (:output-file task))
  node-count-field  (JTextField.   (str (:node-count task)))
  action-display (create-action "Display" (fn [_] (prn task))
      { Action/SHORT_DESCRIPTION  "Display it", Action/ACCELERATOR_KEY (KeyStroke/getKeyStroke KeyEvent/VK_L Event/CTRL_MASK) })
  button-panel (JPanel. (MigLayout. "", "[pref]", "[grow]5"))
  ]
  (doto frame 
   (.setSize 600 300)
   (.setContentPane panel))
  (doto button-panel
   (.add (JButton. action-display) "growx")
   (.add (JButton. "Compile") "growx")
   (.add (JButton. "Upload") "growx")
   (.add (JButton. "Schedule") "growx")
   (.add (JButton. "Download") "growx")
   (.revalidate))
  (doto panel
   (.add (JLabel. "Name:"))          (.add name-field         "growx,wrap")
   (.add (JLabel. "Status:"))        (.add status-field       "growx,wrap")
   (.add (JLabel. "Task id:"))       (.add task-id-field      "growx,wrap")
   (.add (JLabel. "Source file:"))   (.add source-file-field  "growx,wrap")
   (.add (JLabel. "Input file:"))    (.add input-file-field   "growx,wrap")
   (.add (JLabel. "Output file:"))   (.add output-file-field  "growx,wrap")
   (.add (JLabel. "Node count:"))    (.add node-count-field   "growx,wrap")
   (.add button-panel "span 2")
   (.revalidate))
   frame))

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
  action-display (create-action "Display" 
      (fn [_] 
       (let [index (.getSelectedIndex jlist)] 
	(when (not= index -1) 
	 (let [t (task (.elementAt list-model index))]
	  (.setVisible (create-task-window t jscfi) true)))))
      { Action/SHORT_DESCRIPTION  "Display it", Action/ACCELERATOR_KEY (KeyStroke/getKeyStroke KeyEvent/VK_L Event/CTRL_MASK) })
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
  (.addMouseListener jlist (proxy [MouseAdapter] [] (mouseClicked [event] (when (= (.getClickCount event) 2) (.actionPerformed action-display nil)))))
  frame))

(defn create-and-show-window []
 (let [frame (create-main-window)]
  (.setLocationRelativeTo frame nil)
  (.setVisible frame true) 
  frame))

(defn main []
 (SwingUtilities/invokeLater create-and-show-window))


