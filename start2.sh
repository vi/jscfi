#!/bin/sh
JP=/usr/share/java
DEPS=$JP/clojure.jar:$JP/clojure-contrib.jar:$JP/clj-yaml-0.3.0.jar:$JP/snakeyaml-1.5.jar:$JP/jsch.jar:$JP/miglayout.jar
AWT_TOOLKIT=MToolkit java -cp .:$DEPS clojure.main org/vi_server/jscfi/main.clj
