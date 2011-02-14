(ns org.vi-server.jscfi.gui
    "GUI for Jscfi"
    (:use org.vi-server.jscfi.jscfi)
    (:use org.vi-server.jscfi.fake)
    (:import 
     (javax.swing JPanel JFrame JLabel JTextField JTextArea JButton SwingUtilities JList JScrollPane DefaultListModel AbstractAction Action KeyStroke)
     (javax.swing JMenu JMenuBar JPasswordField)
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
  action-display (create-action "Debug Print" (fn [_] (prn task))
      { Action/SHORT_DESCRIPTION  "Display it", Action/ACCELERATOR_KEY (KeyStroke/getKeyStroke KeyEvent/VK_L Event/CTRL_MASK) })
  action-create (create-action "Create" (fn [_] (register-task jscfi {
	      :name           (.getText name-field)
	      :status         :created
	      :source-file    (.getText source-file-field)
	      :input-file     (.getText input-file-field)
	      :output-file    (.getText output-file-field)
	      :node-count     (.getText node-count-field)}))
      { Action/SHORT_DESCRIPTION  "Create a task", Action/ACCELERATOR_KEY (KeyStroke/getKeyStroke KeyEvent/VK_ENTER Event/CTRL_MASK) })
  button-panel (JPanel. (MigLayout. "", "[pref]", "[grow]5"))
  ]
  (doto frame 
   (.setSize 600 300)
   (.setContentPane panel))
  (doto button-panel
   (.add (JButton. action-display) "growx")
   (.add (JButton. action-create) "growx")
   (.add (JButton. "compile") "growx")
   (.add (JButton. "upload") "growx")
   (.add (JButton. "schedule") "growx")
   (.add (JButton. "download") "growx")
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




(defn create-authentication-window [password-promise]
 (let [
  panel (JPanel. (MigLayout. "", "[grow]", "[grow][grow][grow]"))
  frame (JFrame.)
  password-field (JPasswordField.)
  action-ok (create-action "OK" (fn [_] (deliver password-promise (.getText password-field)) (doto frame (.setVisible false) (.dispose)) ) {})
  ]
  (doto frame 
   (.setSize 400 120)
   (.setContentPane panel))
  (doto panel
   (.add (JLabel. "Enter the password:") "wrap")
   (.add password-field "growx,wrap")
   (.add (JButton. action-ok) )
   (.revalidate))
  (.setLocationRelativeTo frame nil)
  frame))




(defn create-main-window [jscfi] 
 (let [
  observer (reify JscfiObserver 
      (get-password [this] 
       (let [password-promise (promise)] 
	(SwingUtilities/invokeLater #(.setVisible (create-authentication-window password-promise) true)) 
	@password-promise #_ "Will be delivered by the frame")))
  panel (JPanel. (MigLayout. "", "[grow][pref][pref]", "[pref][grow]"))
  frame (JFrame.)
  text-field (JTextField.)
  list-model (DefaultListModel.)
  jlist (JList. list-model)
  action-create (create-action "Create task" (fn [_] (.setVisible (create-task-window {} jscfi) true))
      { Action/SHORT_DESCRIPTION  "Open window for task creation", Action/ACCELERATOR_KEY (KeyStroke/getKeyStroke KeyEvent/VK_N Event/CTRL_MASK) })
  action-refresh (create-action "Refresh" (fn [_] 
	  (.clear list-model)
	  (doall (map #(.add list-model 0 (TaskListEntryImpl. %)) (get-tasks jscfi))))
      { Action/SHORT_DESCRIPTION  "Refresh the list of tasks", Action/ACCELERATOR_KEY (KeyStroke/getKeyStroke KeyEvent/VK_R Event/CTRL_MASK) })
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
   (.setJMenuBar menubar))
  (doto panel
   (.add text-field "growx")
   (.add (JButton. action-create) )
   (.add (JButton. action-open) "wrap")
   (.add (JScrollPane. jlist) "grow,span 3")
   (.revalidate))
  (.add view-menu    action-refresh)
  (.add view-menu    action-open)
  (.add action-menu    action-create)
  (doto menubar
   (.add view-menu)
   (.add action-menu))
  (connect jscfi observer "scfi" "test")
  (.actionPerformed action-refresh nil) 
  (.addMouseListener jlist (proxy [MouseAdapter] [] (mouseClicked [event] (when (= (.getClickCount event) 2) (.actionPerformed action-open nil)))))
  frame))

(defn create-and-show-window []
 (let [frame (create-main-window (get-fake-jscfi))]
  (.setLocationRelativeTo frame nil)
  (.setVisible frame true) 
  frame))

(defn main []
 (SwingUtilities/invokeLater create-and-show-window))


