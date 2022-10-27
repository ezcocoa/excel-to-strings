(ns excel-to-strings.core
  (:use [excel-to-strings.utils.util]
        [clojure.data.xml :as xml])
  (:require [clojure.tools.cli :refer [parse-opts]]
            [excel-to-strings.config :as cfg]
            [cheshire.core :refer :all]
            [camel-snake-kebab.core :as csk])
  (:gen-class))

;;
;; Created by hojun rooney baek
;; [iOS Localization][https://developer.apple.com/library/archive/documentation/MacOSX/Conceptual/BPInternational/LocalizingYourApp/LocalizingYourApp.html]
;;

(defn replace-aos-format
  "AOS용 포멧 스트링으로 변환한다."
  [s]
  (let [ll (clojure.string/split (str s " ") #"%s")]
    (loop [l ll
           idx 1
           r []]
      (if (seq l)
        (recur (rest l) (inc idx) (conj r (first l) (str "%" idx "$s")))
        (trim (clojure.string/join (drop-last r)))
        ))))

(defn generate-android-strings-xml
  "Android strings xml 파일 포멧 데이터를 생성한다."
  [strs]
  (indent-str
   (xml/sexp-as-element
    [:resources
     (map (fn [{:keys [key value]}]
            [:string
             {:name (trim key)}
             (-> (trim value)
                 (clojure.string/replace "%d" "%s")
                 (replace-aos-format))]
            ;; android string-array를 위한 처리
            ;; (if (nil? (clojure.string/index-of value "||"))
            ;;   [:string-array {:name (trim key)}
            ;;    (for [d (clojure.string/split value #"\|\|")]
            ;;      [:item d])]
            ;;   )
            ) 
          strs)])))

(defn generate-ios-strings
  "iOS용 strings 포멧 데이터를 생성한다."
  [kvs prefix]
  (clojure.string/join
   (map (fn [{:keys [key value]}]
          (format "\"%s\" = \"%s%s\";\n" (trim key) prefix (-> (trim value)
                                                               (clojure.string/replace "%d" "%@")
                                                               (clojure.string/replace "%s" "%@"))))
        kvs)))

(defn generate-json
  "WEB용 JSON 포멧 데이터를 생성한다."
  [kvs]
  (apply merge (map (fn [{:keys [key value]}]
                     {(keyword (trim key)) (trim value)}) 
                   kvs)))

(defn write-xml!
  "XML(Android용) 파일을 생성한다."
  [output_file xml]
  (with-open [out-file (java.io.FileWriter. output_file)]
    (xml/emit xml out-file)))

(defn write-file!
  "JSON 데이터를 생성한다."
  [output_file data]
  (with-open [w (clojure.java.io/writer output_file :append false)]
    (.write w data)))

(defn write-strings!
  "strings(iOS용) 파일을 생성한다."
  [output_file data]
  (write-file! output_file data))

(defn write-file-stream!
  [output_file map_data]
  (generate-stream map_data
                   (clojure.java.io/writer output_file)
                   {:pretty true}))

(def cli-options
  [; sheet file 지정 옵션 
   ["-f" "--file File" "파일명"
    :default "lang.xlsx"
    :validate [#(not (nil? (clojure.string/index-of % ".xlsx"))) "Excel 파일형식만 지원합니다."]]
   ; 도움말 옵션
   ["-h" "--help"]])

(defn usage
  [options-summary]
  (->> ["다국어 처리를 위한 Excel 파일을 리소스 파일로 변환할 수 있습니다."
        "(현재 iOS/Android/WEB 플랫폼 형식을 지원)"
        ""
        "사용법: program-name [옵션] action"
        ""
        "옵션:"
        options-summary
        ""
        "버전: 0.9.0"
        "더 많은 정보는 mr.hjbaek@gmail.com로 문의주세요."]
       (clojure.string/join \newline)))

(defn error-msg
  [errors]
  (str "문법 분석 중 오류가 발생되었습니다.\n\n"
       (clojure.string/join \newline errors)))

(defn validate-args
  [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond
      (:help options) ; help => exit OK with usage summary
      {:exit-message (usage summary) :ok? true}
      errors ; errors => exit with description of errors
      {:exit-message (error-msg errors)}
      (= 1 (count options))
      {:options options}
      :else ; failed custom validation => exit with usage summary
      {:exit-message (usage summary)})))

(defn exit
  [status msg]
  (println msg)
  (System/exit status))

(defn -main
  "export from excel to multi-platform string files."
  [& args]
  (let [{:keys [options exit-message ok?]} (validate-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (do
        ;;; 폴더 생성
        (loop [output_paths ["output"
                             "output/en.lproj"
                             "output/ko.lproj"]]
          (when (seq output_paths)
            (let [output_file (java.io.File. (first output_paths))]
              (when (not (. output_file exists))
                (.mkdir output_file))
              (recur (rest output_paths)))))

        ;; 왜 'for'은 동작하지 않지?
        ;; (for [output_path ["output"
        ;;                    "output/en.lproj"
        ;;                    "output/ko.lproj"]]
        ;;   (let [output_file (java.io.File. output_path)]
        ;;     (println output_file)
        ;;     (when (not (. output_file exists))
        ;;       (println (.mkdir output_file)))
        ;;     ))

        ;;; 변환
        (let [kvs (sel-list (load-excel (:file options) cfg/select-sheet-name cfg/select-columns) cfg/select-start-row)
              duplicated_kvs (->> kvs
                                  (group-by :key)
                                  (map (fn [[k v]] 
                                         {:key k :count (count v)}))
                                  (filter (fn [x]
                                            (> (:count x) 1))))]
          (if (empty? duplicated_kvs)
            (let [aos_data (generate-android-strings-xml kvs)
                  ios_ko_data (generate-ios-strings kvs "")
                  ios_en_data (generate-ios-strings kvs "🇺🇸")
                  web_data (generate-json kvs)]

              (let [file cfg/output-ios-en-file]
                (write-file! file ios_en_data)
                (println (format "iOS용 [%s] en 파일이 생성되었습니다." file)))

              (let [file cfg/output-ios-ko-file]
                (write-file! file ios_ko_data)
                (println (format "iOS용 [%s] ko 파일이 생성되었습니다." file)))

              (let [file cfg/output-android-file]
                (write-file! file aos_data)
                (println (format "Android용 [%s] 파일이 생성되었습니다." file)))

              (let [file cfg/output-web-file]
                (write-file-stream! file web_data)
                (println (format "WEB용 [%s] 파일이 생성되었습니다." file))))

            (loop [l duplicated_kvs]
              (when (seq l)
                (let [d (first l)
                      key (:key d)
                      count (:count d)]
                  (println (format "'%s'값이 %d개 존재합니다." key count))
                  (recur (rest l)))))))))))

(comment
  ;; TEST CODE
  ;;
  ;; Load excel data.
  ;;
  (-> (sel-list (load-excel cfg/select-excel-file cfg/select-sheet-name cfg/select-columns) 3)
      prn )
  
  ;;
  ;; generate android string file.
  ;;
  (let [strs (sel-list (load-excel cfg/select-excel-file cfg/select-sheet-name cfg/select-columns) 3)
        xml (generate-android-strings-xml strs)]
    (write-file! cfg/output-android-file xml))
  ;;
  ;; generate ios string file.
  ;;
  (generate-ios-strings (sel-list (load-excel cfg/select-excel-file cfg/select-sheet-name cfg/select-columns) 3))

  (write-file-stream! cfg/output-web-file (generate-json (sel-list (load-excel cfg/select-excel-file cfg/select-sheet-name cfg/select-columns) 3)))
  )
