(ns toyokumo.commons.io
  (:require
   [clojure.java.io :as io]
   [schema.core :as s])
  (:import
   (java.io
    Reader)
   (org.apache.commons.io.input
    BOMInputStream)))

(s/defn excluding-bom-reader :- Reader
  "Make a reader that detect and exclude a UTF-8 BOM

  in   - InputStream or something to make InputStream from
  opts - same options as clojure.java.io/IOFactory

  General usage:
    (excluding-bom-reader file :encoding :utf-8)"
  [in & opts]
  (let [opts (when opts (apply hash-map opts))]
    (-> in
        (io/make-input-stream opts)
        (BOMInputStream.)
        (io/make-reader opts))))
