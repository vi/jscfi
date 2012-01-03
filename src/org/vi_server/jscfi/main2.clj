(ns org.vi-server.jscfi.main2
    "GUI for Jscfi"
    (:use org.vi-server.jscfi.gui-main-window)
    (:use org.vi-server.jscfi.gui-common)
    (:use org.vi-server.jscfi.engine)
    (:use [clojure.tools.logging :only [info warn error debug]])
    (:gen-class))

(defn -main [& args] 
 (let [jscfi-path (-> (fn[]) (class) (.getClassLoader) (.getResource "jscfi-file-list.txt"))]
  (info "Started Jscfi main from " jscfi-path))
 ;; jscfi-path is like jar:file:/home/vi/code/jscfi/jscfi-1.4-SNAPSHOT-standalone.jar!/jscfi-file-list.txt

 (let [override-source-path (:source-directory @settings)]
  (if (empty? override-source-path)
   (info "Starting Jscfi normally")
   (do 
    (info "Loading Clojure sources from " override-source-path)
    (defn load-all-clojure-files [fname]
     (let [f (java.io.File. fname)]
      (if (.isDirectory f)
       (doall (map #(load-all-clojure-files 
                     (str (java.io.File. (java.net.URI. (str (.toURI f) "/" %))))) (seq (.list f))))
       (when (re-find #"\.clj$" fname)
        (debug "Loading " fname)
        (load-file fname)))))

    (try

     (load-all-clojure-files override-source-path)
     (info "All found clj files loaded")

     (let [new-jscfi-scripts (str override-source-path "/org/vi_server/jscfi/scripts")]
      (swap! scripts-path (fn[_] new-jscfi-scripts)))

     (catch Exception e (error "exll" e))))))

 (let
  [scripts-path-env (System/getenv "JSCFI_SCRIPTS")]
  (when-not (empty? scripts-path-env)
   (swap! scripts-path (fn[_] scripts-path-env))))

 (gui-main (get-jscfi-engine)))
