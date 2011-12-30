(ns org.vi-server.jscfi.main
    "GUI for Jscfi"
    (:use org.vi-server.jscfi.gui-main-window)
    (:use org.vi-server.jscfi.engine)
    (:gen-class))

(defn -main [& args] 
 (gui-main (get-jscfi-engine)))

