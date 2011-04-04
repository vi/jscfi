#!/bin/sh
JP=/usr/share/java
AWT_TOOLKIT=MToolkit java -cp .:$JP/clojure.jar:$JP/clojure-contrib.jar:$JP/clojure-json-1.2.jar:$JP/jsch.jar:$JP/miglayout.jar clojure.main org/vi_server/jscfi/main.clj
