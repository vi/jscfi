(ns org.vi-server.jscfi.main2
    "GUI for Jscfi"
    (:use org.vi-server.jscfi.gui)
    (:use org.vi-server.jscfi.real)
    (:gen-class))
(defn -main [& args] (gui-main (get-real-jscfi)))
