(ns org.vi-server.jscfi.main2
    "GUI for Jscfi"
    (:use org.vi-server.jscfi.gui-main-window)
    (:use org.vi-server.jscfi.gui-common)
    (:use org.vi-server.jscfi.engine)
    (:gen-class))

(defn -main [& args] 
 (let [jscfi-path (-> (fn[]) (class) (.getClassLoader) (.getResource "jscfi-file-list.txt"))]
  (println "Started Jscfi main from " (str jscfi-path)))
 ;; jscfi-path is like jar:file:/home/vi/code/jscfi/jscfi-1.4-SNAPSHOT-standalone.jar!/jscfi-file-list.txt

 (let [override-source-path (:source-directory @settings)]
  (if (empty? override-source-path)
   (println "Starting Jscfi normally")
   (do 
    (println "Loading Clojure sources from " override-source-path)
    (defn load-all-clojure-files [fname]
     (let [f (java.io.File. fname)]
      (if (.isDirectory f)
       (doall (map #(load-all-clojure-files 
                     (str (java.io.File. (java.net.URI. (str (.toURI f) "/" %))))) (seq (.list f))))
       (when (re-find #"\.clj$" fname)
        (println "Loading " fname)
        (load-file fname)))))

    (try

     (load-all-clojure-files override-source-path)
     (println "All found clj files loaded")

     (let [new-jscfi-scripts (str override-source-path "/org/vi_server/jscfi/scripts")]
      (swap! scripts-path (fn[_] new-jscfi-scripts)))

     (catch Exception e (println e))))))

 (let
  [scripts-path-env (System/getenv "JSCFI_SCRIPTS")]
  (when-not (empty? scripts-path-env)
   (swap! scripts-path (fn[_] scripts-path-env))))

 (gui-main (get-jscfi-engine)))
