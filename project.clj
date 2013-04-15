(defproject 
   jscfi
   "1.8" 
   :description "GUI for uploading MPI programs into PBS-controlled servers" 
   :dependencies [[org.clojure/clojure "1.5.1"] 
                  [org.clojure/tools.logging "0.2.3"]
		  [com/miglayout/miglayout "3.7.2"]
		  [com/jcraft/jsch "0.1.44-1"]
          [log4j "1.2.15" :exclusions [javax.mail/mail javax.jms/jms com.sun.jdmk/jmxtools com.sun.jmx/jmxri]]
		  ]
    :dev-dependencies [[slamhound "1.2.0"]]
    :main org.vi-server.jscfi.main2
    :java-source-path "src"
    :aot :all
)
