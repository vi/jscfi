(ns org.vi-server.jscfi.gui-authentication-window
    "GUI for Jscfi (authentication window)"
    (:use org.vi-server.jscfi.jscfi)
    (:use org.vi-server.jscfi.gui-common)
    (:import 
     (javax.swing JPanel JFrame JLabel JTextField JTextArea JButton SwingUtilities JList JScrollPane DefaultListModel AbstractAction Action KeyStroke)
     (javax.swing JMenu JMenuBar JPasswordField)
     (java.awt.event KeyEvent MouseAdapter WindowAdapter)
     (java.awt Event)
     (net.miginfocom.swing MigLayout)))

(defn create-authentication-window [jscfi]
 (let [
  panel (JPanel. (MigLayout. "", "[][grow][pref]", "[grow]5[grow][grow]"))
  frame (JFrame.)
  prefs (.node (java.util.prefs.Preferences/userRoot) "/org/vi-server/jscfi")
  user-field (JTextField. (.get prefs "login" "zimyanin"))
  server-field (JTextField. (.get prefs "hostname" "217.21.43.14"))
  password-field (JPasswordField.)
  keyfile-field (JTextField. (.get prefs "keyfile" ""))
  hostsfile-field (JTextField. (.get prefs "known_hosts" ""))
  directory-field (JTextField. (.get prefs "directory" "default"))
  connstage-label (JLabel.)
  auth-observer (reify AuthObserver
      (get-password [this] (.getText password-field))
      (get-keyfile [this] (.getText keyfile-field))
      (get-hostsfile [this] (.getText hostsfile-field))
      (auth-succeed [this] 
       (.put prefs "login" (.getText user-field))
       (.put prefs "hostname" (.getText server-field))
       (.put prefs "keyfile" (.getText keyfile-field))
       (.put prefs "known_hosts" (.getText hostsfile-field))
       (.put prefs "directory" (.getText directory-field))
       (doto frame (.setVisible false) (.dispose))
       )
      (auth-failed [this] (msgbox "Login failed"))
      (connection-stage [this msg] (SwingUtilities/invokeLater (fn [](.setText connstage-label msg))))
      )
  action-connect (create-action "Connect" (fn [_] 
      (if (re-find #"^[A-Za-z0-9_]{1,32}$" (.getText directory-field))
       (connect jscfi auth-observer (.getText server-field) (.getText user-field) (.getText directory-field))
       (msgbox "Directory should match ^[A-Za-z0-9_]{1,32}$"))) {})
  ]
  (doto frame 
   (.setSize 400 240)
   (.setContentPane panel)
   (.addWindowListener (proxy [WindowAdapter] [] (windowClosing [_] (exit-if-needed))))
   (.setTitle "Login to SCFI")
  )
  (doto panel
   (.add (JLabel. "Server:"))
   (.add server-field "growx,wrap,span 2")

   (.add (JLabel. "Login:"))
   (.add user-field "growx,wrap,span 2")

   (.add (JLabel. "Password:"))
   (.add password-field "growx,wrap,span 2")

   (.add (JLabel. "Keyfile:"))
   (.add keyfile-field "growx")
   (.add (create-file-chooser-button keyfile-field :open) "wrap")

   (.add (JLabel. "Known hosts:"))
   (.add hostsfile-field "growx")
   (.add (create-file-chooser-button hostsfile-field :open) "wrap")
   
   (.add (JLabel. "Directory:"))
   (.add directory-field "growx,wrap,span 2")

   (.add connstage-label "span 2,wrap")
   (.add (JButton. action-connect) "span 2")
   (.revalidate))
  (.setLocationRelativeTo frame nil)

  ;; if known_hosts is not saved in preferences, try to detect it
  (try
   (when (= "" (.getText hostsfile-field))
    (let [
     c1 (System/getenv "HOME")
     c2 (System/getenv "APPDATA")
     ]
     (if c2
      (.setText hostsfile-field (str c2 "\\known_hosts"))
      (when c1 (.setText hostsfile-field (str c1 "/.ssh/known_hosts"))
       (.mkdirs (java.io.File. (str c1 "/.ssh")))))))
   (catch Exception e (.printStackTrace e)))
  frame))

