(defproject com.github.yjcyxky/remote-fs "0.2.3"
  :description "File system utilities for object store in clojure."
  :url "https://github.com/yjcyxky/remote-fs.git"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [com.aliyun.oss/aliyun-sdk-oss "3.10.2"]
                 [clojure.java-time "0.3.2"]
                 [clj-time "0.15.2"]
                 [org.clojure/data.json "1.0.0"]
                 [io.minio/minio "7.1.0"]
                 [metosin/spec-tools "0.10.5"]
                 [metosin/ring-http-response "0.9.1"]
                 [org.clojure/tools.logging "1.1.0"]]
  :plugins [[lein-cloverage "1.0.13"]
            [lein-shell "0.5.0"]
            [lein-ancient "0.6.15"]
            [lein-changelog "0.3.2"]]
  :repl-options {:init-ns remote-fs.core}
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.10.0"]]}
             :uberjar {:auto-clean    true
                       :aot           :all
                       :omit-source   true
                       :javac-options ["-target" "1.8", "-source" "1.8"]
                       :target-path   "target/%s"
                       :resource-paths ["resources"]
                       :uberjar-name  "remote-fs.tservice-plugin.jar"}}

  :repositories [["central" "https://maven.aliyun.com/repository/central"]
                 ["jcenter" "https://maven.aliyun.com/repository/jcenter"]
                 ["clojars" "https://mirrors.tuna.tsinghua.edu.cn/clojars/"]]

  :plugin-repositories [["central" "https://maven.aliyun.com/repository/central"]
                        ["jcenter" "https://maven.aliyun.com/repository/jcenter"]
                        ["clojars" "https://mirrors.tuna.tsinghua.edu.cn/clojars/"]]

  :deploy-repositories [["clojars" {:url "https://clojars.org/repo"
                                    :username :gpg
                                    :password :gpg}]]

  :aliases {"update-readme-version" ["shell" "sed" "-i" "" "s/\\\\[com\\.github\\.yjcyxky\\\\/remote-fs \"[0-9.]*\"\\\\]/[com\\.github\\.yjcyxky\\\\/remote-fs \"${:version}\"]/" "README.md"]}
  :release-tasks [["shell" "git" "diff" "--exit-code"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["changelog" "release"]
                  ["update-readme-version"]
                  ["vcs" "commit"]
                  ["vcs" "tag"]
                  ["deploy"]
                  ["vcs" "push"]])
