(defproject 
   jscfi
   "1.5" 
   :description "GUI for uploading MPI programs into PBS-controlled servers" 
   :dependencies [[org.clojure/clojure "1.3.0-alpha4"] 
                 #_ "Alpha version because of http://dev.clojure.org/jira/browse/CLJ-855"
                  [org.clojure/clojure-contrib "1.2.0"]
                  [org.clojure/tools.logging "0.2.3"]
		  [com/miglayout/miglayout "3.7.2"]
		  [com/jcraft/jsch "0.1.44-1"]
		  ]
    :main org.vi-server.jscfi.main2
    :java-source-path "src"
)
