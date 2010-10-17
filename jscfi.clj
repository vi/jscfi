;; Task uploader and executer for SCFI BSU
;; Clojure Swing example by Keith Bennett, March 2009 ;; kbennett -at- bbsinc -dot- biz


(ns jscfi
  (:import (java.awt BorderLayout Event GridLayout Toolkit)
           (java.awt.event KeyEvent)
           (javax.swing AbstractAction Action BorderFactory 
           JFrame JPanel JButton JMenu JMenuBar JTextField JLabel
           KeyStroke)
           (javax.swing.event DocumentListener)))

(defn create-textfields-panel
"Creates panel containing the labels and text fields."
[]

  (let [
    create-an-inner-panel #(JPanel. (GridLayout. 0 1 5 5))
    label-panel           (create-an-inner-panel)
    text-field-panel      (create-an-inner-panel)
    outer-panel           (JPanel. (BorderLayout.))]

    (doto label-panel
      (.add (JLabel. "Login:"))
      (.add (JLabel. "Password:"))
      (.add (JLabel. "Source code file:"))
      (.add (JLabel. "Save the result to:"))
    )
    
    (doto text-field-panel
	(.add (JTextField. 15))
	(.add (JTextField. 15))
	(.add (JTextField. 15))
	(.add (JTextField. 15))
    )

    (doto outer-panel
      (.add label-panel BorderLayout/WEST)
      (.add text-field-panel BorderLayout/CENTER))))


(defn create-action
"Creates an implementation of AbstractAction."
[name behavior options]

  (let [
    action (proxy [AbstractAction] [name]
      (actionPerformed [event] (behavior event)))]

    (if options
      (doseq [key (keys options)] (.putValue action key (options key))))
    action))

(def exit-action (create-action "Exit"
    (fn [_] (System/exit 0))

    { Action/SHORT_DESCRIPTION  "Exit this program",
      Action/ACCELERATOR_KEY
            (KeyStroke/getKeyStroke KeyEvent/VK_X Event/CTRL_MASK) }))

(def upload-action (create-action "Upload"
    (fn [_] (. javax.swing.JOptionPane (showMessageDialog nil "Uploading is not implemented")))

    { Action/SHORT_DESCRIPTION  "Upload and compile the source code to SCFI BSU",
      Action/ACCELERATOR_KEY
            (KeyStroke/getKeyStroke KeyEvent/VK_U Event/CTRL_MASK) }))

(def execute-action (create-action "Execute"
    (fn [_] (. javax.swing.JOptionPane (showMessageDialog nil "Executing is not implemented")))

    { Action/SHORT_DESCRIPTION  "Execute the code in SCFI BSU",
      Action/ACCELERATOR_KEY
            (KeyStroke/getKeyStroke KeyEvent/VK_E Event/CTRL_MASK) }))


(defn create-menu-bar
"Creates the menu bar with File, Edit, and Convert menus."
[]

  (let [
    menubar (JMenuBar.)
    file-menu (JMenu. "File")
    action-menu (JMenu. "Action")]

    (.add file-menu    exit-action)
    (.add action-menu    upload-action)
    (.add action-menu    execute-action)
    
    (doto menubar
      (.add file-menu)
      (.add action-menu))))


(defn create-buttons-panel 
"Creates the panel with conversion, clear, and exit buttons."
[]
  (let [
    inner-panel (JPanel. (GridLayout. 1 0 5 5))
    outer-panel (JPanel. (BorderLayout.))]

    (doto inner-panel
      (.add (JButton. upload-action))
      (.add (JButton. execute-action))
      (.add (JButton. exit-action)))

    (doto outer-panel
      (.add inner-panel BorderLayout/EAST)
      (.setBorder (BorderFactory/createEmptyBorder 10 0 0 0)))))


(defn center-on-screen 
"Centers a component on the screen based on the screen dimensions
reported by the Java runtime."
[component]
  
  (let [
    screen-size   (.. Toolkit getDefaultToolkit getScreenSize)
    screen-width  (.getWidth screen-size)
    screen-height (.getHeight screen-size)
    comp-width    (.getWidth component)
    comp-height   (.getHeight component)
    new-x         (/ (- screen-width comp-width) 2)
    new-y         (/ (- screen-height comp-height) 2)]

    (.setLocation component new-x new-y))

    component
)



(defn create-frame
"Creates the main JFrame used by the program."
[]

  (let [
    f (JFrame. "SCFI BSU uploader")
    content-pane (.getContentPane f)]

    (doto content-pane
      (.add (create-textfields-panel) BorderLayout/CENTER)
      (.add (create-buttons-panel) BorderLayout/SOUTH)
      (.setBorder (BorderFactory/createEmptyBorder 12 12 12 12)))

    (doto f
      (.setDefaultCloseOperation JFrame/EXIT_ON_CLOSE)
      (.setJMenuBar (create-menu-bar))
      (.pack))

    (center-on-screen f)))


(defn main
"Main is defined here to indicate the location of the program's
entry point more explicitly than merely including statements
outside of a function."
[]
    ; We create a global variable here so that the frame can be
    ; be inspected when this file is loaded in REPL (the Clojure
    ; interactive command line, something like irb in Ruby), as in:

    ; (load-file "FrameInClojure.clj")
    ; (in-ns 'jscfi)
    ; (println main-frame)  ; illustrates access to the program's frame

    (def main-frame (create-frame))
    (.setVisible main-frame true))


(main)
