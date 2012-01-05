(ns org.vi-server.jscfi.main
    "GUI for Jscfi"
    (:use
     [org.vi-server.jscfi.engine :only [get-jscfi-engine scripts-path]]
     [org.vi-server.jscfi.gui-main-window :only [gui-main]])
    (:gen-class))

(defn -main [& args] 
 (let
  [scripts-path-env (System/getenv "JSCFI_SCRIPTS")]
  (when-not (empty? scripts-path-env)
   (swap! scripts-path (fn[_] scripts-path-env))))

 (gui-main (get-jscfi-engine)))

