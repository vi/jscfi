(ns org.vi-server.jscfi.gui-settings-window
  "GUI for Jscfi (settings window)"
  (:use
    [clojure.tools.logging :only [debug info warn error]]
    [org.vi-server.jscfi.gui-common
     :only
     [create-action create-file-chooser-button msgbox settings]])
  (:import
    (java.io File)
    (javax.swing Action JButton JFrame JLabel JPanel JTextField)
    (net.miginfocom.swing MigLayout)))

(def settings-window-size {:width 430, :height 170})

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
          _ (debug "Extracting " (str f))
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
  panel (JPanel. (MigLayout. "", "[][grow][pref][pref]", "[grow]3[grow][grow]"))
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
      ) {Action/SHORT_DESCRIPTION  "Save setings (does not close the window)"})
  action-extract-source (create-action "Extract source code" (fn [_] 
    (let [source-directory (.getText source-directory-field)]
     (extract-source-code source-directory))
    ) {Action/SHORT_DESCRIPTION  "Extract the source code of this Jscfi into 'Jscfi source dir' directory'"})
  ]
  (doto frame 
   (.setSize (:width settings-window-size) (:height settings-window-size))
   (.setContentPane panel)
   (.setTitle "Jscfi settings")
  )
  (doto panel
   (.add (doto (JLabel. "Known hosts:") (.setToolTipText "File to store the host key of server. Used for security.")))
   (.add hostsfile-field "growx")
   (.add (create-file-chooser-button hostsfile-field :open) "wrap")
   
   (.add (doto (JLabel. "Log viewer:") (.setToolTipText
    (str "<html>Executable jar file to load external log viewer.<br/>"
     "In addition to usual 'public static void main(String[])' it must <br/>"
     "have 'public static void main(InputStream)' and read everything <br/>"
     "from that InputStream without long delays (otherwise the whole Jscfi will fail).<br/>"
     "See the example of such jar and library to parse that input at http://vi-server.org/pub/diststatanalyse-1.3.jar"
     "<br/><br/>If empty, jscfi will dump stat data to System.out instead.</html>"))))
   (.add log-viewer-field "growx")
   (.add (create-file-chooser-button log-viewer-field :open) "wrap")
   
   (.add (doto (JLabel. "Jscfi source dir:") (.setToolTipText
    (str "<html>Source directory to run updated Jscfi from.<br/>"
     "If set, jscfi will start from that source code instead of one embedded in jscfi-...-standalone.jar.</html>"))))
   (.add source-directory-field "growx")
   (.add (create-file-chooser-button source-directory-field :opendir) "wrap")

   (.add (JButton. action-save))
   (.add (JButton. action-extract-source))

   (.revalidate))
  (.setLocationRelativeTo frame nil)

  frame))

