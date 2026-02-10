(ns nervestaple.mimeo.uuid.interface
  (:require
   [nervestaple.mimeo.uuid.core :as core]))

(defn generate-sequential
  "Returns a new, sequential `UUID` (UUID version 7). This function used the
  Jackson project's generator."
  []
  (core/generate-sequential))
