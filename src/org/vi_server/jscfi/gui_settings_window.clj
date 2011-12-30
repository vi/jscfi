(ns org.vi-server.jscfi.gui-settings-window
    "GUI for Jscfi (settings window)"
    (:use org.vi-server.jscfi.gui-common)
    (:import 
     (javax.swing JPanel JFrame JLabel JTextField JTextArea JButton SwingUtilities JList JScrollPane DefaultListModel AbstractAction Action KeyStroke)
     (javax.swing JMenu JMenuBar JPasswordField)
     (java.awt.event KeyEvent MouseAdapter WindowAdapter)
     (java.awt Event)
     (java.io File)
     (net.miginfocom.swing MigLayout)))


(defn load-settings []
  (let [
    prefs (.node (java.util.prefs.Preferences/userRoot) "/org/vi-server/jscfi")
    known-hosts (.get prefs "known_hosts" "")
    log-viewer (.get prefs "log_viewer" "")
    source-directory (.get prefs "source_directory" "")

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
   {:known-hosts known-hosts2, :log-viewer log-viewer, :source-directory source-directory}   
  )
 )

(swap! settings (fn[_] (load-settings)))

(defn extract-source-code [target-directory]
    (let [tdu (str (.toURI (java.io.File. target-directory)))]
        (defn get-target-file [name] 
         "Get path of name in target directory. 
         'org/vi_server' -> file:/c:/jscfi-source/////org/vi_server -> File C:\\jscfi-source\\org\\vi_server"
         (java.io.File. (java.net.URI. (str tdu "/////" name))))
        (defn mkdir [name] (.mkdirs (get-target-file name)))
        (defn read-resource [name] (slurp (ClassLoader/getSystemResourceAsStream name)))
        (defn extract-file [name] 
         (let [
          f (get-target-file name)
          _ (println (str "Extracting " (str f)))
          _ (.mkdirs (.getParentFile f))
          w (java.io.FileWriter. f)
          ]
          (.write w (read-resource name))
          (.close w)
          ))
        
        (doall (map #(extract-file %)
         (seq (.split #"\n" (read-resource "jscfi-file-list.txt")))))
    )
)

(defn create-settings-window []
 (let [
  panel (JPanel. (MigLayout. "", "[][grow][pref]", "[grow]2[grow][grow]"))
  frame (JFrame.)
  prefs (.node (java.util.prefs.Preferences/userRoot) "/org/vi-server/jscfi")
  hostsfile-field (JTextField. (:known-hosts @settings))
  log-viewer-field (JTextField. (:log-viewer @settings))
  source-directory-field (JTextField. (:source-directory @settings))
  action-save (create-action "Save" (fn [_] 
      (let [
        known-hosts (.getText hostsfile-field),
        log-viewer (.getText log-viewer-field)
        source-directory (.getText source-directory-field)
        ]
       (.put prefs "known_hosts" known-hosts)
       (.put prefs "log_viewer" log-viewer)
       (.put prefs "source_directory" source-directory)
       (when (not (empty? source-directory))
        (let [sample-jscfi-file (str source-directory "/org/vi_server/jscfi/jscfi.clj")]
         (when (not (.canRead (File. sample-jscfi-file)))
          (msgbox (str "Note that " sample-jscfi-file " appears to be unavailable.\n"
           "Source loading will not work probably.")))))
       (swap! settings (fn[_]{:known-hosts known-hosts, :log-viewer log-viewer, :source-directory source-directory}))) 
      ) {})
  action-extract-source (create-action "Extract source code" (fn [_] 
    (let [source-directory (.getText source-directory-field)]
     (extract-source-code source-directory))
    ) {})
  ]
  (doto frame 
   (.setSize 430 155)
   (.setContentPane panel)
   (.setTitle "Jscfi settings")
  )
  (doto panel
   (.add (JLabel. "Known hosts:"))
   (.add hostsfile-field "growx")
   (.add (create-file-chooser-button hostsfile-field :open) "wrap")
   
   (.add (JLabel. "Log viewer:"))
   (.add log-viewer-field "growx")
   (.add (create-file-chooser-button log-viewer-field :open) "wrap")
   
   (.add (JLabel. "Jscfi source dir:"))
   (.add source-directory-field "growx")
   (.add (create-file-chooser-button source-directory-field :opendir) "wrap")

   (.add (JButton. action-save))
   (.add (JButton. action-extract-source))

   (.revalidate))
  (.setLocationRelativeTo frame nil)

  frame))

