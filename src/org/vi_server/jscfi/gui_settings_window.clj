(ns org.vi-server.jscfi.gui-settings-window
    "GUI for Jscfi (settings window)"
    (:use org.vi-server.jscfi.gui-common)
    (:import 
     (javax.swing JPanel JFrame JLabel JTextField JTextArea JButton SwingUtilities JList JScrollPane DefaultListModel AbstractAction Action KeyStroke)
     (javax.swing JMenu JMenuBar JPasswordField)
     (java.awt.event KeyEvent MouseAdapter WindowAdapter)
     (java.awt Event)
     (net.miginfocom.swing MigLayout)))

(def jscfi-version "1.3")

(defn load-settings []
  (let [
    prefs (.node (java.util.prefs.Preferences/userRoot) "/org/vi-server/jscfi")
    known-hosts (.get prefs "known_hosts" "")
    log-viewer (.get prefs "log_viewer" "")

    ;; if known_hosts is not saved in preferences, try to detect it
    known-hosts2 
     (try
      (if (empty? known-hosts)
       (let [
        c1 (System/getenv "HOME")
        c2 (System/getenv "APPDATA")
        ]
        (if c2
         (str c2 "\\known_hosts")
         (when c1 
          (.mkdirs (java.io.File. (str c1 "/.ssh")))
          (str c1 "/.ssh/known_hosts")
          )))
       known-hosts)
      (catch Exception e (.printStackTrace e) known-hosts))
   ]
   {:known-hosts known-hosts2, :log-viewer log-viewer}   
  )
 )

(def settings (atom (load-settings)))

(defn create-settings-window []
 (let [
  panel (JPanel. (MigLayout. "", "[][grow][pref]", "[grow]2[grow][grow]"))
  frame (JFrame.)
  prefs (.node (java.util.prefs.Preferences/userRoot) "/org/vi-server/jscfi")
  hostsfile-field (JTextField. (:known-hosts @settings))
  log-viewer-field (JTextField. (:log-viewer @settings))
  action-save (create-action "Save" (fn [_] 
      (let [
        known-hosts (.getText hostsfile-field),
        log-viewer (.getText log-viewer-field)
        ]
       (.put prefs "known_hosts" known-hosts)
       (.put prefs "log_viewer" log-viewer)
       (swap! settings (fn[_]{:known-hosts known-hosts, :log-viewer log-viewer})))        
      ) {})
  ]
  (doto frame 
   (.setSize 430 135)
   (.setContentPane panel)
   (.addWindowListener (proxy [WindowAdapter] [] (windowClosing [_] (exit-if-needed))))
   (.setTitle "Jscfi settings")
  )
  (doto panel
   (.add (JLabel. "Known hosts:"))
   (.add hostsfile-field "growx")
   (.add (create-file-chooser-button hostsfile-field :open) "wrap")
   
   (.add (JLabel. "Log viewer:"))
   (.add log-viewer-field "growx")
   (.add (create-file-chooser-button log-viewer-field :open) "wrap")

   (.add (JButton. action-save))

   (.revalidate))
  (.setLocationRelativeTo frame nil)

  frame))

