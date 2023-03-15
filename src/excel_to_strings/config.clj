(ns excel-to-strings.config)

;; config.cdn 파일을 참조하여 사용.
;; 데이터가 존재하지 않을 시 아래의 값들이 기본값으로 설정됨.
(defonce select-excel-file "lang.xlsx")
(defonce select-sheet-name "key 정의") ; config 파일에서 불러오도록 수정
(defonce select-columns {:C :key :D :ko :E :en}) ; config 파일에서 불러오도록 수정
(defonce select-start-row 2) ; config 파일에서 불러오도록 수정

(defonce output-android-files [{:code "en"
                                :prefix ""
                                :file "output/android/values/strings.xml"}
                               {:code "ko"
                                :prefix ""
                                :file "output/android/values-ko/strings.xml"}
                               ])

(defonce output-ios-files [{:code "en"
                           :prefix ""
                           :file "output/ios/en.lproj/Localizable.strings"}
                          {:code "ko"
                           :prefix ""
                           :file "output/ios/ko.lproj/Localizable.strings"}])

(defonce output-web-files [{:code "en"
                            :prefix ""
                            :file "output/web/strings-en.json"}
                           {:code "ko"
                            :prefix ""
                            :file "output/web/strings-ko.json"}
                           ])
