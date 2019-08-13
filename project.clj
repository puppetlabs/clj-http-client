(defproject puppetlabs/http-client "1.1.1"
  :description "HTTP client wrapper"
  :license {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0.html"}

  :min-lein-version "2.9.1"

  :parent-project {:coords [puppetlabs/clj-parent "4.0.0"]
                   :inherit [:managed-dependencies]}

  ;; Abort when version ranges or version conflicts are detected in
  ;; dependencies. Also supports :warn to simply emit warnings.
  ;; requires lein 2.2.0+.
  :pedantic? :abort

  :dependencies [[org.clojure/clojure]

                 [org.apache.httpcomponents/httpasyncclient "4.1.2"]
                 [prismatic/schema]
                 [commons-io]
                 [io.dropwizard.metrics/metrics-core]

                 [puppetlabs/ssl-utils]
                 [puppetlabs/i18n]]

  :source-paths ["src/clj"]
  :java-source-paths ["src/java"]
  :jar-exclusions [#".*\.java$"]

  ;; By declaring a classifier here and a corresponding profile below we'll get an additional jar
  ;; during `lein jar` that has all the source code (including the java source). Downstream projects can then
  ;; depend on this source jar using a :classifier in their :dependencies.
  :classifiers [["sources" :sources-jar]]

  :profiles {:dev {:dependencies [[cheshire]
                                  [puppetlabs/kitchensink :classifier "test"]
                                  [puppetlabs/trapperkeeper]
                                  [puppetlabs/trapperkeeper :classifier "test"]
                                  [puppetlabs/trapperkeeper-webserver-jetty9]
                                  [puppetlabs/trapperkeeper-webserver-jetty9 :classifier "test"]
                                  [puppetlabs/ring-middleware]
                                  [org.bouncycastle/bcpkix-jdk15on]]}
             :fips {:dependencies [[cheshire]
                                   [puppetlabs/kitchensink :classifier "test"]
                                   [puppetlabs/trapperkeeper]
                                   [puppetlabs/trapperkeeper :classifier "test"]
                                   [puppetlabs/trapperkeeper-webserver-jetty9]
                                   [puppetlabs/trapperkeeper-webserver-jetty9 :classifier "test"]
                                   [puppetlabs/ring-middleware]
                                   [org.bouncycastle/bcpkix-fips]
                                   [org.bouncycastle/bc-fips]]}
             :sources-jar {:java-source-paths ^:replace []
                           :jar-exclusions ^:replace []
                           :source-paths ^:replace ["src/clj" "src/java"]}}

  :deploy-repositories [["releases" {:url "https://clojars.org/repo"
                                     :username :env/clojars_jenkins_username
                                     :password :env/clojars_jenkins_password
                                     :sign-releases false}]]

  :lein-release {:scm :git
                 :deploy-via :lein-deploy}

  :plugins [[lein-parent "0.3.7"]
            [puppetlabs/i18n "0.8.0"]]

  :repositories [["puppet-releases" "https://artifactory.delivery.puppetlabs.net/artifactory/list/clojure-releases__local/"]
                 ["puppet-snapshots" "https://artifactory.delivery.puppetlabs.net/artifactory/list/clojure-snapshots__local/"]])
