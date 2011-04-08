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

(defn is-running-from-repl []
 (bound? #'*1))
