(ns nervestaple.mimeo.ollama.core
  (:require
   [jsonista.core :as json])
  (:import
   [io.github.ollama4j Ollama]
   [io.github.ollama4j.models.generate OllamaGenerateRequest]))

(defn connect [ollama-url]
  (Ollama. ollama-url))

(defn json->map [json-data]
  (json/read-value json-data json/keyword-keys-object-mapper))

(defn model->map [model]
  (json/read-value model json/keyword-keys-object-mapper))

(defn models [connection]
  (mapv #(model->map (str %))
        (.listModels connection)))

(defn name->model [connection model-name]
  (->> (models connection)
      (filter #(= (:name %) model-name))
      (first)))

(defn family->model [connection model-family-name]
  (->> (models connection)
       (filterv #(= (:modelName %) model-family-name))))

(defn request [model prompt]
  (let [model-this (if (map? model) (:name model) model)
        builder (OllamaGenerateRequest/builder)]
    (doto builder
      (.withModel model-this)
      (.withPrompt prompt)
      (.build))))

(defn prompt-one-shot [connection model prompt]
  (let [in (request model prompt)
        out (.generate connection in nil)]
    (json->map (str out))))
