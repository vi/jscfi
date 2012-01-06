(ns org.vi-server.jscfi.main2
 "GUI for Jscfi"
 (:use
  [clojure.tools.logging :only [debug error warn info trace]]
  [org.vi-server.jscfi.engine :only [get-jscfi-engine scripts-path]]
  [org.vi-server.jscfi.gui-common :only [settings]]
  [org.vi-server.jscfi.gui-main-window :only [gui-main]])
 (:require org.vi-server.jscfi.main)
 (:refer-clojure :exclude (add-classpath))
 (:use [cemerick.pomegranate :only [add-classpath]])
 (:gen-class))

(defn load-all-clojure-files [fname]
 (trace "load-all-clojure-files" fname)
 (let [f (java.io.File. fname)]
  (if (.isDirectory f)
   (doall (map #(load-all-clojure-files (str fname "/" %)) (seq (.list f))))
   (when (re-find #"\.clj$" fname)
    (debug "Loading  " fname)
    (try (load-file fname) (catch Throwable e (error e)))))))


(defn load-jscfi-from-directory [source-path]
 "Load clojure files and use scripts from source-directory, then start GUI.
 Expects the class loader to be already set to the proper thing"
 
 (add-classpath (java.net.URL. (str "file://" source-path)))
 (let [cl (-> (Thread/currentThread) (.getContextClassLoader))]
  (info "Class loader: " cl)
  (doall (map #(info "URL: " %) (seq (.getURLs cl)))))

 (try
  (info "Loading Clojure sources from " source-path)
  (load-all-clojure-files source-path)
  (info "All found clj files loaded")
  (catch Throwable e (error e)))
 (let [new-jscfi-scripts (str source-path "/org/vi_server/jscfi/scripts")]
  (swap! scripts-path (fn[_] new-jscfi-scripts)))
 (org.vi-server.jscfi.main/-main))



(defn -main [& args] 
 (let [jscfi-path (-> (fn[]) (class) (.getClassLoader) (.getResource "jscfi-file-list.txt"))]
  (info "Started Jscfi main from " jscfi-path))
 ;; jscfi-path is like jar:file:/home/vi/code/jscfi/jscfi-1.4-SNAPSHOT-standalone.jar!/jscfi-file-list.txt

 (let [override-source-path (:source-directory @settings)]
  (if (empty? override-source-path)
   (do
    (info "Starting Jscfi normally")
    (apply org.vi-server.jscfi.main/-main args))
   (do 
    (info "Using user-specified source directory")
    ;; Hacky tricks here: 
    ;; 1. Add our user-specified source directory to classpath (using new instance of class loader)
    ;; 2. Call clojure.main -e "(some (source code))" in that class loader
    ;;      "some source code":
    ;;      a. Scan the source code directory for *.clj files and load them all
    ;;      b. Override directory for scipts
    ;;      c. Call org.vi-server.jscfi.main that will load everything more properly
    ;; Just adding class path alone is not enough (we have old class files loaded).
    (try
     (load-jscfi-from-directory override-source-path)
     (catch Exception e (error "exll" e) (org.vi-server.jscfi.main/-main)))))))
