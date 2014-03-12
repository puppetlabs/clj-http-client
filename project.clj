(defproject puppetlabs/http-client "0.1.0"
  :description "HTTP client wrapper"
  :license {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0.html"}

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [http-kit "2.1.16"]
                 [puppetlabs/kitchensink "0.5.2"]]

  :deploy-repositories [["releases" {:url "https://clojars.org/repo"
                                     :username :env/clojars_jenkins_username
                                     :password :env/clojars_jenkins_password
                                     :sign-releases false}]]

  :lein-release {:scm :git
                 :deploy-via :lein-deploy}

  :plugins [[lein-release "1.0.5"]])
