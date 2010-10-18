(ns jscfi
  (:import (java.awt BorderLayout Event GridLayout Toolkit)
           (java.awt.event KeyEvent)
           (javax.swing AbstractAction Action BorderFactory 
           JFrame JPanel JButton JMenu JMenuBar JTextField JPasswordField JLabel KeyStroke)
           (javax.swing.event DocumentListener)))

(defn create-action
"Creates an implementation of AbstractAction."
[name behavior options]

  (let [
    action (proxy [AbstractAction] [name]
      (actionPerformed [event] (behavior event)))]

    (if options
      (doseq [key (keys options)] (.putValue action key (options key))))
    action))

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

(defmacro lazy-init [f & args] 
  `(let [x# (delay (~f ~@args))] 
    #(force x#)))

(defn create-a-text-field
  "Creates a temperature text field."
  [default tooltip & flags]
 
  (doto (if (contains? (set flags) :password) (JPasswordField. 15) (JTextField. 15))
    (.setToolTipText tooltip)
    (.setText default))
)
