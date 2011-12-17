(defproject 
   jscfi
   "1.3-SNAPSHOT" 
   :description "GUI for uploading MPI programs into PBS-controlled servers" 
   :dependencies [[org.clojure/clojure "1.3.0-alpha4"]
                  [org.clojure/clojure-contrib "1.2.0"]
		  [com/miglayout/miglayout "3.7.2"]
		  [com/jcraft/jsch "0.1.44-1"]
		  ]
    :main org.vi-server.jscfi.main2
    :java-source-path "src"
)
