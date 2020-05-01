(ns toyokumo.commons.csv
  (:require
   [clojure.java.io :as io]
   [schema.core :as s])
  (:import
   (java.io
    Reader)
   (org.apache.commons.csv
    CSVFormat
    CSVParser
    CSVPrinter)))

(def ^:private format-schema
  (s/enum :default
          :excel
          :informix-unload
          :informix-unload-csv
          :mongodb-csv
          :mongodb-tsv
          :mysql
          :oracle
          :postgresql-csv
          :postgresql-text
          :rfc4180
          :tdf))

(s/defn csv-format :- CSVFormat
  [fmt :- format-schema]
  (case fmt
    :excel CSVFormat/EXCEL
    :informix-unload CSVFormat/INFORMIX_UNLOAD
    :informix-unload-csv CSVFormat/INFORMIX_UNLOAD_CSV
    :mongodb-csv CSVFormat/MONGODB_CSV
    :mongodb-tsv CSVFormat/MONGODB_TSV
    :mysql CSVFormat/MYSQL
    :oracle CSVFormat/ORACLE
    :postgresql-csv CSVFormat/POSTGRESQL_CSV
    :postgresql-text CSVFormat/POSTGRESQL_TEXT
    :rfc4180 CSVFormat/RFC4180
    :tdf CSVFormat/TDF
    CSVFormat/DEFAULT))

(s/defn csv-parser :- CSVParser
  "Make org.apache.commons.csv.CSVParser

  reader - java.io.Reader
  opts   - Options that determine CSV format
    :format - Use to make org.apache.commons.csv.CSVFormat
              See https://commons.apache.org/proper/commons-csv/user-guide.html for more detail"
  ([reader :- Reader]
   (csv-parser reader {:format :default}))
  ([reader :- Reader
    opts :- {:format format-schema}]
   (CSVParser/parse reader (csv-format (:format opts)))))

(defmulti read-all
  "Read all CSV records as a vector of vectors of string,
  like: [[\"foo\" \"\bar\"] [\"hoge\" \"fuga\"]]

  General usage:
    (with-open [reader (-> (clojure.java.io/file \"/your/file/path.csv\")
                           (clojure.java.io/reader :encoding \"utf-8\"))
                parser (csv-parser reader {:format :rfc4180})]
      (read-all parser))"
  class)

(defmethod read-all CSVParser [parser]
  (->> (.getRecords parser)
       (mapv vec)))

(defmethod read-all Reader [reader]
  (with-open [parser (csv-parser reader)]
    (read-all parser)))

(defmethod read-all :default [in]
  (with-open [parser (csv-parser (io/reader in))]
    (read-all parser)))

(s/defn csv-printer :- CSVPrinter
  "Make org.apache.commons.csv.CSVPrinter

  out    - java.lang.Appendable
  opts   - Options that determine CSV format
    :format - Use to make org.apache.commons.csv.CSVFormat
              See https://commons.apache.org/proper/commons-csv/user-guide.html for more detail"
  ([out :- Appendable]
   (csv-printer out {:format :default}))
  ([out :- Appendable
    opts :- {:format format-schema}]
   (CSVPrinter. out (csv-format (:format opts)))))

(defmulti write-all
  "Write all CSV records into out.
  valeus should be a vector like: [[\"foo\" \"\bar\"] [\"hoge\" \"fuga\"]]

  General usage:
    ;; Write on memory
    (let [values [[\"foo\" \"bar\"] [\"hoge\" \"fuga\"]]
         sb (StringBuilder.)
         printer (csv-printer sb {:format :rfc4180})]
      (write-all printer values)
      (str sb))

    ;; Write to a file
    (let [values [[\"あいう\" \"えお\"] [\"foo\" \"bar\"]]]
      (with-open [out (clojure.java.io/writer (io/file \"/your/file/path.csv\") :encoding \"utf-8\")]
        (let [printer (csv-printer out {:format :rfc4180})]
          (write-all printer values))))"
  (fn [out values]
    (class out)))

(defmethod write-all CSVPrinter [printer values]
  (.printRecords printer values))

(defmethod write-all :default [out values]
  (write-all (csv-printer out) values))
