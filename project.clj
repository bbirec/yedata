(defproject yedata "0.1.0-SNAPSHOT"
  :description "Aurora Serverless Data API (feat. yesql)"
  :url "https://github.com/bbirec/yedata"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/data.json "0.2.6"]
                 [com.amazonaws/aws-java-sdk-rdsdata "1.11.638"]
                 [instaparse "1.4.10" :exclusions [org.clojure/clojure]]])
