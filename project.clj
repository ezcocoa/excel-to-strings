(defproject excel-to-strings "0.1.7"
  :description "다국어 변환 툴입니다. (excel to multi-platform language file.)"
  :url "https://ezcocoa.com"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [dk.ative/docjure "1.17.0"]
                 [org.clojure/data.xml "0.0.8"]
                 [org.clojure/tools.cli "1.0.206"]
                 [cheshire "5.11.0"]
                 [camel-snake-kebab "0.4.3"]]
  :main excel-to-strings.core
  :aot :all
  :repl-options {:init-ns excel-to-strings.core})
