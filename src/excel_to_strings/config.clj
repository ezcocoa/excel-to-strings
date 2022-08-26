(ns excel-to-strings.config)
;; config
;;
(defonce select-excel-file "lang.xlsx")
(defonce select-sheet-name "key 정의")
(defonce select-columns {:C :key :E :value})
(defonce select-start-row 3)
(defonce output-android-file "output/strings.xml")
(defonce output-ios-file "output/Localizable.strings")
(defonce output-web-file "output/strings.json")
