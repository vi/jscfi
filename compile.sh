#!/bin/sh
java -cp .:/usr/share/java/clojure.jar:/usr/share/java/jsch.jar clojure.main -e '(compile "jscfi")'
