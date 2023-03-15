(ns excel-to-strings.utils.util
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.data.xml :as xml])
  (:use [dk.ative.docjure spreadsheet]))
;;
;; utils
;;
(defn trim [v]
  (if v
    (clojure.string/trim v)
    ""))

(defn trim-all [v]
  (if v
    (clojure.string/replace (clojure.string/trim v) #" " "")
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
       (remove #(empty? (trim (:key %))))))

(defn make-folders
  "폴더 존재 여부를 확인하고 없으면 생성한다."
  [folders]
  (loop [output_paths folders]
    (when (seq output_paths)
      (clojure.java.io/make-parents (first output_paths))
      (recur (rest output_paths)))))

(defn get-config
  ([] (get-config "config.edn"))
  ([filename]
   (if (.exists (new java.io.File filename))
     (edn/read-string (slurp filename)) {})))
