(ns org.vi-server.jscfi.gui-log-window
  "GUI for Jscfi (log window)"
  (:import (javax.swing JFrame JPanel JScrollPane JTextArea)))


(def log-window-size {:width 400, :height 600 })

(defn create-log-window []
 (let [
  panel (JPanel. (net.miginfocom.swing.MigLayout. "", "[grow]", "[grow]"))
  frame (JFrame.)
  textarea (JTextArea.)
  ]
  (doto frame 
   (.setSize (:width log-window-size) (:height log-window-size))
   (.setContentPane panel)
   (.setLocationRelativeTo nil)
   (.setTitle "Text info"))
  (.setText textarea "ololo")
  (doto panel
   (.add (JScrollPane. textarea) "grow")
   (.revalidate))
  frame))
