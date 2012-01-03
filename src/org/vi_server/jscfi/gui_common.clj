(ns org.vi-server.jscfi.gui-common
    "Common things for Jscfi's GUI"
    (:use [clojure.tools.logging :only [info warn error debug]])
    (:import (javax.swing AbstractAction Action JButton JFileChooser SwingUtilities JComboBox)))

(def jscfi-version "1.5")

(def settings (atom nil)) ;; initialized in gui_settings_window.clj

(defn create-action "Creates an implementation of AbstractAction." [name behavior options]
 (let [
  action (proxy [AbstractAction] [name]
      (actionPerformed [event] (behavior event)))]
  (if options
   (doseq [key (keys options)] (.putValue action key (options key))))
  action))

(defn create-file-chooser-button [textfield mode] "Create button which causes open/save dialog writing filename to the specified text field. mode is :open or :save"
 (JButton. (create-action "..." (fn[_]
  (let [fc (JFileChooser.)]
   (when (contains? #{:opendir :savedir} mode)
    (.setFileSelectionMode fc JFileChooser/DIRECTORIES_ONLY))
   (when (= (if 
             (contains? #{:open :opendir} mode) 
             (.showOpenDialog fc textfield) 
             (.showSaveDialog fc textfield)) JFileChooser/APPROVE_OPTION)
    (.setText textfield (str (.getSelectedFile fc)))))) {})))

(defn msgbox [text] (SwingUtilities/invokeLater (fn[](javax.swing.JOptionPane/showMessageDialog nil 
			   text "jscfi" javax.swing.JOptionPane/INFORMATION_MESSAGE))))


(defn combobox-create [the-set] (try
 {
 :combobox (JComboBox. (into-array the-set)),
 :keyword-to-index (zipmap the-set (take (count the-set) (iterate inc 0))),
 :set the-set
 } (catch Throwable e (error "pln3" e))))
(defn combobox-set [cb kw] (try
 (if (find (:keyword-to-index cb) kw)
  (.setSelectedIndex (:combobox cb) (get (:keyword-to-index cb) kw))
  (.setSelectedIndex (:combobox cb) 0)) (catch Throwable e (error e))))
(defn combobox-get [cb]
 (get (:set cb) (.getSelectedIndex (:combobox cb))))
(defn combobox-field [cb] (:combobox cb))
 

;; Does not work from leiningen:
;; (defn is-running-from-repl [] (println "#'*1 is " #'*1 " bound:" (bound? #'*1)) (bound? #'*1))


(defn exit-if-needed [] 
 (when-not (System/getenv "DONT_EXIT") (System/exit 0)))


(defn get-monitoring-output []
    "Start external monitoring tool and return pipe for writing into it. Returns System.out in case of failure."
    (let [
     log-viewer (:log-viewer @settings)
     output (if 
         (empty? log-viewer) 
         System/out
         (try
          (let [
            pipein (java.io.PipedInputStream.)
            pipeout (java.io.PipedOutputStream. pipein)
            
            classPathUrls (into-array java.net.URL [(java.net.URL. (str "file://" log-viewer))])
            classLoader (java.net.URLClassLoader. classPathUrls)
            _ (info "MM Loaded " log-viewer)
            mainClassName (-> 
                (java.util.jar.JarFile. log-viewer) 
                (.getManifest) 
                (.getMainAttributes) 
                (.getValue "Main-Class") )
            _ (debug "MM Main class is " mainClassName)
            mainClass (.loadClass classLoader mainClassName)
            ourMainMethod (.getMethod mainClass "main" (into-array Class [java.io.InputStream]))
            _ (debug "MM Found static main(InputStream)")
           ]
           (.start (Thread. (fn[]
             (info "MM Started external monitoring")
             (try
                (.invoke ourMainMethod nil (into-array Object [pipein]))
              (catch Exception e (error "MM " e)))
             (info "MM External monitoring exited")
             (.close pipein)
             )))
           pipeout
           )
          (catch Exception e (error "MM " e) System/out))
         )
     ]
     output
    ))
