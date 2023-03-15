(ns excel-to-strings.core
  (:use [excel-to-strings.utils.util :as util]
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
  "Android strings xml 파일 포멧 데이터를 생성한다.
  kvs like [{:key 'a' :ko 'a' :en 'a'}..]
  column is value key. like :E, :C..
  prefix is that you want to add some value in prefix. like en1, en2, en3, prefix will be 'en'."
  [kvs ^clojure.lang.Keyword column ^String prefix]
  (indent-str
   (xml/sexp-as-element
    [:resources
     (map (fn [obj] ;; {:keys [key (symbol vk)]}
            (let [key (:key obj)
                  value (column obj)]
              [:string
               {:name (trim key)}
               (-> (str prefix (trim value))
                   (clojure.string/replace "'" "\\'")
                   (clojure.string/replace "%d" "%s")
                   (replace-aos-format))])

            ;; android string-array를 위한 처리
            ;; (if (nil? (clojure.string/index-of value "||"))
            ;;   [:string-array {:name (trim key)}
            ;;    (for [d (clojure.string/split value #"\|\|")]
            ;;      [:item d])]
            ;;   )
            ) 
          kvs)])))

(defn generate-ios-strings
  "iOS용 strings 포멧 데이터를 생성한다."
  [kvs ^clojure.lang.Keyword column ^String prefix]
  (clojure.string/join
   (map (fn [obj]
          (let [key (:key obj)
                value (column obj)]
            (format "\"%s\" = \"%s%s\";\n" (trim key) prefix (-> (trim value)
                                                                 (clojure.string/replace "%d" "%@")
                                                                 (clojure.string/replace "%s" "%@")))
            ))
        kvs)))

(defn generate-json
  "WEB용 JSON 포멧 데이터를 생성한다."
  [kvs ^clojure.lang.Keyword column ^String prefix]
  (apply merge (map
                (fn [obj]
                  (let [key (:key obj)
                        value (column obj)]
                    {(keyword (trim key)) (str prefix (trim value))}))
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
   ["-f" "--file File" "파일 경로 ex) /home/user/guest/lang.xlsx"
    :default "lang.xlsx"
    :validate [#(not (nil? (clojure.string/index-of % ".xlsx"))) "Excel 파일형식만 지원합니다."]]

   ;; 시트명
   ["-s" "--sheet Sheet" "시트명"
    :default "key 정의"]

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
        "버전: 0.10.0"
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
      ; help => exit OK with usage summary
      (:help options) {:exit-message (usage summary) :ok? true}

      ; errors => exit with description of errors
      errors {:exit-message (error-msg errors)}

      ; failed custom validation => exit with usage summary
      :else {:options options}
      )))

(defn exit
  [status msg]
  (println msg)
  (System/exit status))

(defn convert-to-file
  [kvs out-files generate-fn {:keys [purpose writer]}]
  (loop [fs out-files]
    (when (seq fs)
      (let [{:keys [code prefix file]} (first fs)
            column (keyword code)]
        (if writer
          (writer file (generate-fn kvs column prefix))
          (write-file! file (generate-fn kvs column prefix)))
        (println (format "%s용 [%s] %s 파일이 생성되었습니다." purpose file code))
        (recur (rest fs))))))

(defn print-error-msg [cfg msg error]
  (println "")
  (println "+----------------------------------------------------------+")
  (println "|" msg)
  (println "+----------------------------------------------------------+")

  (when error
    (println "| Error:")
    (if (> (count error) 55)
      (loop [l (split-at 55 error)]
        (when (seq l)
          (println "| " (clojure.string/join (first l)))
          (recur (rest l))))
      (println "| " error))
    (println "+----------------------------------------------------------+"))

  (let [filename (:file_name cfg)
        sheetname (:sheet_name cfg)
        key (:key cfg)
        columns (:columns cfg)
        data_index (:data_index cfg)]
    (println "")
    (println "+----------------------------------------------------------+")
    (println "| 👨‍🏭 환경 설정")
    (println "+----------------------------------------------------------+")
    (println "| - 📁 파일:" filename)
    (println "| - 📝 시트:" sheetname)
    (println "| - 🔑 키 컬럼:" key)
    (println "| - 📓 데이터 컬럼:" columns)
    (println "| - 📇 데이터 인덱스:" data_index)
    (println "+----------------------------------------------------------+")
    (println "")
    ))

(defn -main
  "export from excel to multi-platform string files."
  [& args]
  (time
   (let [{:keys [options exit-message ok?]} (validate-args args)]
     (if exit-message
       (exit (if ok? 0 1) exit-message)
       (let [cfg (util/get-config)
             arg_file (:file options) ; 지정된 파일 경로
             arg_sheet (:sheet options)  ; 지정된 시트명
             file_name (if arg_file
                         arg_file
                         (if-let [file_name (:file_name cfg)]
                           file_name
                           cfg/select-excel-file))

             sheet_name (if arg_sheet
                          arg_sheet
                          (if-let [sheet_name (:sheet_name cfg)] ; 지정된 시트 이름
                            sheet_name
                            cfg/select-sheet-name))

             ;; key (if-let [key (:key cfg)] ; 엑셀에서 다국어 key 열
             ;;       {key :key}
             ;;       {:C :key})

             ;; values (if-let [values (:values cfg)] ; 엑셀에서 값이 되는 열
             ;;          values
             ;;          {:D :ko :E :en})

             columns (if-let [columns (:columns cfg)]
                       {:C :key
                        :D :ko
                        :E :en})
             data_index (if-let [di (:data_index cfg)]
                          di
                          cfg/select-start-row)]
         (try
           (if-let [ed (load-excel file_name sheet_name columns)]

             ;; 데이터를 정상적으로 불러왔을 경우...
             (let [kvs (sel-list ed data_index)
                   duplicated_kvs (->> kvs
                                       (group-by :key)
                                       (map (fn [[k v]]
                                              {:key k :count (count v)}))
                                       (filter (fn [x]
                                                 (> (:count x) 1))))]
               ;; 폴더 생성
               (let [folders (concat
                              (map #(:file %) cfg/output-ios-files)
                              (map #(:file %) cfg/output-android-files)
                              (map #(:file %) cfg/output-web-files))]
                 (util/make-folders folders))

               ;; 왜 'for'은 동작하지 않지?
               ;; (for [output_path ["output"
               ;;                    "output/en.lproj"
               ;;                    "output/ko.lproj"]]
               ;;   (let [output_file (java.io.File. output_path)]
               ;;     (println output_file)
               ;;     (when (not (. output_file exists))
               ;;       (println (.mkdir output_file)))
               ;;     ))

               (if (empty? duplicated_kvs)
                 (do
                   (convert-to-file kvs cfg/output-ios-files generate-ios-strings {:purpose "iOS"
                                                                                   :writer nil})
                   (convert-to-file kvs cfg/output-android-files generate-android-strings-xml {:purpose "Android"
                                                                                               :writer nil})
                   (convert-to-file kvs cfg/output-web-files generate-json {:purpose "WEB"
                                                                            :writer write-file-stream!}))
                 ;; 중복 데이터 존재 시 출력
                 (loop [l duplicated_kvs]
                   (when (seq l)
                     (let [d (first l)
                           key (:key d)
                           count (:count d)]
                       (println (format "'%s'값이 %d개 존재합니다." key count))
                       (recur (rest l)))))))

             ;; 데이터를 정상적으로 불러오지 못했을 경우
             (print-error-msg (merge cfg
                                     (when arg_file
                                       {:file_name arg_file})
                                     (when arg_sheet
                                       {:sheet_name arg_sheet}))
                              "💬 엑셀 데이터를 불러오지 못했습니다."
                              nil
                              ))

           (catch Exception e
             (print-error-msg (merge cfg
                                     (when arg_file
                                       {:file_name arg_file})
                                     (when arg_sheet
                                       {:sheet_name arg_sheet}))

                              "💬 엑셀 데이터를 불러오는 중 문제가 발생하였습니다."
                              (. e getMessage)))))))))

(comment
  (def select-excel-file "/Users/hojunbaek/Downloads/lang_en.xlsx")
  (def select-sheet-name "Key 정의")
  (def select-columns {:C :key
                       :D :ko
                       :E :en})
  ;; TEST CODE
  ;;
  ;; Load excel data.
  ;;
  ;;
  (-> (sel-list (load-excel select-excel-file select-sheet-name cfg/select-columns) 3)
      prn )
  
  ;;
  ;; generate android string file.
  ;;
  (let [strs (sel-list (load-excel select-excel-file select-sheet-name select-columns) 3)
        xml (generate-android-strings-xml strs :ko nil)]
    xml
    ;; (write-file! cfg/output-android-file xml)
    )
  ;;
  ;; generate ios string file.
  ;;
  ;;
  (let [list (sel-list (load-excel "/Users/hojunbaek/Downloads/lang_en.xlsx" "Key 정의" select-columns) 3)]
    (generate-ios-strings list)
    )


  (write-file-stream! cfg/output-web-file (generate-json (sel-list (load-excel cfg/select-excel-file cfg/select-sheet-name cfg/select-columns) 3)))
  )
