(def ks-version "1.2.0")
(def tk-version "1.1.1")
(def tk-jetty-version "1.5.0")

(defproject puppetlabs/http-client "0.5.1-SNAPSHOT"
  :description "HTTP client wrapper"
  :license {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0.html"}

  ;; Abort when version ranges or version conflicts are detected in
  ;; dependencies. Also supports :warn to simply emit warnings.
  ;; requires lein 2.2.0+.
  :pedantic? :abort

  :dependencies [[org.clojure/clojure "1.7.0"]

                 [org.apache.httpcomponents/httpasyncclient "4.1.1"]
                 [prismatic/schema "1.0.4"]
                 [org.slf4j/slf4j-api "1.7.13"]
                 [commons-io "2.4"]
                 [io.dropwizard.metrics/metrics-core "3.1.2"]

                 [puppetlabs/ssl-utils "0.8.1"]]

  :source-paths ["src/clj"]
  :java-source-paths ["src/java"]
  :jar-exclusions [#".*\.java$"]

  ;; By declaring a classifier here and a corresponding profile below we'll get an additional jar
  ;; during `lein jar` that has all the source code (including the java source). Downstream projects can then
  ;; depend on this source jar using a :classifier in their :dependencies.
  :classifiers [["sources" :sources-jar]]

  :profiles {:dev {:dependencies [[puppetlabs/kitchensink ~ks-version :classifier "test"]
                                  [puppetlabs/trapperkeeper ~tk-version]
                                  [puppetlabs/trapperkeeper ~tk-version :classifier "test"]
                                  [puppetlabs/trapperkeeper-webserver-jetty9 ~tk-jetty-version]
                                  [puppetlabs/trapperkeeper-webserver-jetty9 ~tk-jetty-version :classifier "test"]]
                   ;; TK-143, enable SSLv3 for unit tests that exercise SSLv3
                   :jvm-opts ["-Djava.security.properties=./dev-resources/java.security"]}
             :sources-jar {:java-source-paths ^:replace []
                           :jar-exclusions ^:replace []
                           :source-paths ^:replace ["src/clj" "src/java"]}}

  :deploy-repositories [["releases" {:url "https://clojars.org/repo"
                                     :username :env/clojars_jenkins_username
                                     :password :env/clojars_jenkins_password
                                     :sign-releases false}]]

  :lein-release {:scm :git
                 :deploy-via :lein-deploy}

  :plugins [[lein-release "1.0.5" :exclusions [org.clojure/clojure]]])
