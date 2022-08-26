(ns excel-to-strings.utils.util
  (:use [dk.ative.docjure spreadsheet]
        [clojure.data.xml :as xml]))
;;
;; utils
;;
(defn trim [v]
  (if v
    (clojure.string/trim v)
    ""))

(defn sel-list [l sr]
  (if (<= sr 1)
    l
    (sel-list (rest l) (dec sr))))

(defn load-excel
  "엑셀 파일에 지정된 sheet 정보를 읽어온다."
  [filename sheet columns]
  (->> (load-workbook filename)
       (select-sheet sheet)
       (select-columns columns)
       (remove nil?)
       (remove #(nil? (:key %)) )))
