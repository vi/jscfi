(ns org.vi-server.jscfi.gui-common
    "Common things for Jscfi's GUI"
    (:import (javax.swing AbstractAction Action)))

(defn create-action "Creates an implementation of AbstractAction." [name behavior options]
 (let [
  action (proxy [AbstractAction] [name]
      (actionPerformed [event] (behavior event)))]
  (if options
   (doseq [key (keys options)] (.putValue action key (options key))))
  action))

;; Does not work from leiningen:
;; (defn is-running-from-repl [] (println "#'*1 is " #'*1 " bound:" (bound? #'*1)) (bound? #'*1))


(defn exit-if-needed [] 
 (when-not (System/getenv "DONT_EXIT") (System/exit 0)))
