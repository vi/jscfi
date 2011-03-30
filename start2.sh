#!/bin/sh
AWT_TOOLKIT=MToolkit java -cp .:/usr/share/java/clojure.jar:/usr/share/java/jsch.jar:/usr/share/java/miglayout.jar clojure.main org/vi_server/jscfi/main.clj
