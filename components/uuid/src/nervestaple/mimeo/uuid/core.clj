(ns nervestaple.mimeo.uuid.core
  (:import
   [com.fasterxml.uuid Generators]))

(defonce time-based-epoch-generator (Generators/timeBasedEpochGenerator))

(defn generate-sequential []
  (.generate time-based-epoch-generator))
