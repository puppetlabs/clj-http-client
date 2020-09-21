(defproject puppetlabs/http-client "1.2.0"
  :description "HTTP client wrapper"
  :license {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0.html"}

  :min-lein-version "2.9.1"

  :parent-project {:coords [puppetlabs/clj-parent "4.2.10"]
                   :inherit [:managed-dependencies]}

  ;; Abort when version ranges or version conflicts are detected in
  ;; dependencies. Also supports :warn to simply emit warnings.
  ;; requires lein 2.2.0+.
  :pedantic? :abort

  :dependencies [[org.clojure/clojure]

                 [org.apache.httpcomponents/httpasyncclient]
                 [prismatic/schema]
                 [commons-io]
                 [io.dropwizard.metrics/metrics-core]

                 [puppetlabs/ssl-utils]
                 [puppetlabs/i18n]

                 [org.slf4j/jul-to-slf4j]]

  :source-paths ["src/clj"]
  :java-source-paths ["src/java"]
  :jar-exclusions [#".*\.java$"]

  ;; By declaring a classifier here and a corresponding profile below we'll get an additional jar
  ;; during `lein jar` that has all the source code (including the java source). Downstream projects can then
  ;; depend on this source jar using a :classifier in their :dependencies.
  :classifiers [["sources" :sources-jar]]

  :profiles {:defaults {:dependencies [[cheshire]
                                       [puppetlabs/kitchensink :classifier "test"]
                                       [puppetlabs/trapperkeeper]
                                       [puppetlabs/trapperkeeper :classifier "test"]
                                       [puppetlabs/trapperkeeper-webserver-jetty9]
                                       [puppetlabs/trapperkeeper-webserver-jetty9 :classifier "test"]
                                       [puppetlabs/ring-middleware]]
                        :resource-paths ["dev-resources"]
                        :jvm-opts ["-Djava.util.logging.config.file=dev-resources/logging.properties"]}
             :dev [:defaults
                   {:dependencies [[org.bouncycastle/bcpkix-jdk15on]]}]
             :fips [:defaults
                    {:dependencies [[org.bouncycastle/bcpkix-fips]
                                    [org.bouncycastle/bc-fips]
                                    [org.bouncycastle/bctls-fips]]
                     ;; this only ensures that we run with the proper profiles
                     ;; during testing. This JVM opt will be set in the puppet module
                     ;; that sets up the JVM classpaths during installation.
                     :jvm-opts ~(let [version (System/getProperty "java.version")
                                      [major minor _] (clojure.string/split version #"\.")
                                      unsupported-ex (ex-info "Unsupported major Java version. Expects 8 or 11."
                                                       {:major major
                                                        :minor minor})]
                                  (condp = (java.lang.Integer/parseInt major)
                                    1 (if (= 8 (java.lang.Integer/parseInt minor))
                                        ["-Djava.security.properties==dev-resources/jdk8-fips-security"]
                                        (throw unsupported-ex))
                                    11 ["-Djava.security.properties==dev-resources/jdk11-fips-security"]
                                    (throw unsupported-ex)))}]
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
