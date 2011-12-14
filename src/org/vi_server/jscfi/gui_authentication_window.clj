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


(defn nat-traversal [host1 port1 host2 port2]
  (println host1 port1 host2 port2) 
  (loop [] 
   (try
    (let [
        forwarder (fn[is os knock] 
         (println "Port-forwarded direction opened")
         (let [buffer (byte-array 65536)]
          (loop [knocked false]
           (let [ret (.read is buffer)]
              (when (> ret 0)
               (.write os buffer 0 ret)
               (.flush os)
               (when-not knocked (knock))
               (recur true)))))
         (.close os)
         (.close is)
         (println "Port-forwarded direction closed"))
        socket1 (java.net.Socket.), socket2 (java.net.Socket.)]
      (.connect socket1 (java.net.InetSocketAddress. host1 port1))
      (.connect socket2 (java.net.InetSocketAddress. host2 port2))
      (println "Port-forwarded connection establised")
      (let [
        is1 (.getInputStream socket1)
        is2 (.getInputStream socket2)
        os1 (.getOutputStream socket1)
        os2 (.getOutputStream socket2)
        ]
        (.start (Thread. (fn[]
           (forwarder is2 os1 (fn[])))))
       (forwarder is1 os2 (fn[]))
      ))
    (catch Exception e (println e)))
   (Thread/sleep 10000) 
   (recur)))

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
  nat-traversal-field (JTextField. (.get prefs "nat_traverse" "78.138.100.25:5588:217.21.43.14:22"))
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
  action-nat (create-action "Set" (fn [_] 
    (let [q (agent nil), line (.getText nat-traversal-field)]
     (send q (fn[_]
      (try
       (let [rr (re-find #"(.+):(\d+):(.+):(\d+)" line)]
        (when rr
         (let [
            host1 (get rr 1)
            port1 (Integer/parseInt (get rr 2))
            host2 (get rr 3)
            port2 (Integer/parseInt (get rr 4))
            ]
            (.put prefs "nat_traverse" line)
            (nat-traversal host1 port1 host2 port2))))
       (catch Exception e (println e)))))))
    {Action/SHORT_DESCRIPTION  "Connect to both hosts and exchange data"})
  ]
  (doto frame 
   (.setSize 430 315)
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
   
   (.add (JLabel. "NAT traversal:"))
   (.add nat-traversal-field "growx")
   (.add (JButton. action-nat) "wrap")

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

