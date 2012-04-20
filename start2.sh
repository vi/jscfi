#!/bin/sh
JP=/usr/share/java
DEPS=$JP/clojure.jar:$JP/tools.logging-0.2.3.jar:$JP/log4j-1.2.15.jar:$JP/jsch.jar:$JP/miglayout.jar
cd src
AWT_TOOLKIT=MToolkit java -cp .:$DEPS clojure.main -e '(load-file "org/vi_server/jscfi/main.clj") (org.vi-server.jscfi.main/-main)'
