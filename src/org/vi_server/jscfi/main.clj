(ns org.vi-server.jscfi.main
    "GUI for Jscfi"
    (:use org.vi-server.jscfi.gui)
    (:use org.vi-server.jscfi.engine)
    )
(gui-main (get-jscfi-engine))
