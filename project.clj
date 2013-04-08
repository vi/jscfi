(defproject 
   jscfi
   "1.7" 
   :description "GUI for uploading MPI programs into PBS-controlled servers" 
   :dependencies [[org.clojure/clojure "1.3.0-alpha4"] 
                 #_ "Alpha version because of http://dev.clojure.org/jira/browse/CLJ-855"
                  [org.clojure/tools.logging "0.2.3"]
		  [com/miglayout/miglayout "3.7.2"]
		  [com/jcraft/jsch "0.1.44-1"]
          [log4j "1.2.15" :exclusions [javax.mail/mail javax.jms/jms com.sun.jdmk/jmxtools com.sun.jmx/jmxri]]
		  ]
    :dev-dependencies [[slamhound "1.2.0"]]
    :main org.vi-server.jscfi.main2
    :java-source-path "src"
)
