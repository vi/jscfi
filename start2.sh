#!/bin/sh
JP=/usr/share/java
DEPS=$JP/clojure.jar:$JP/clojure-contrib.jar:$JP/jsch.jar:$JP/miglayout.jar
cd src
AWT_TOOLKIT=MToolkit java -cp .:$DEPS clojure.main org/vi_server/jscfi/main.clj
