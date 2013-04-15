(ns org.vi-server.jscfi.gui-log-window
  "GUI for Jscfi (log window)"
  (:import (javax.swing JFrame JPanel JScrollPane JTextArea SwingUtilities))
  (:gen-class :extends org.apache.log4j.AppenderSkeleton)
  )

(def logging-callback (atom (fn[])))
(def logging-content (atom '()))

(defn get-current-log [] (apply str (interpose "\n" @logging-content)))

(def log-window-size {:width 400, :height 500 })

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
  (.setText textarea (get-current-log))
  (doto panel
   (.add (JScrollPane. textarea) "grow")
   (.revalidate))
  (swap! logging-callback 
   (fn[_] ;; to edit an atom
    (fn[] ;; called by logger
     (SwingUtilities/invokeLater 
      (fn[]  ;; invoked in GUI thread
       (.setText textarea (get-current-log)))))))
  frame))

  
(defn -append [self event] 
 (try
  (let [msg (.getMessage event)]
   (swap! logging-content #(take 50 (cons msg %))))
  (@logging-callback)
  (catch Exception _)))
(defn -requiresLayout [self] true)
