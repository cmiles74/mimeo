(ns nervestaple.mimeo.log.core
  (:require
   [taoensso.timbre :as timbre]
   [taoensso.timbre.appenders.core :as appenders]))

(def LOG-LEVELS [:trace :debug :info :warn :error :fatal :report])

(defn add-file
  [file-name]
  (timbre/debug (str "Logging to \"" file-name "\""))
  (timbre/merge-config!
   {:appenders (merge (timbre/*config* :appenders)
                      {:spit (appenders/spit-appender {:fname file-name})})}))

(defn set-ns-log-level
  ([namespace log-level]
   (set-ns-log-level [[namespace log-level]]))
  ([namespace-to-log-levels]
   (timbre/merge-config!
    (assoc timbre/*config*
           :min-level
           (if (keyword? (:min-level timbre/*config*))
             (into namespace-to-log-levels
                   [["*" (:min-level timbre/*config*)]])
             (vec (merge (into {} (:min-level timbre/*config*))
                         (into {} namespace-to-log-levels))))))))

(defn set-min-level
  [level]
  (timbre/set-min-level! level))

(defmacro with-level
  [level & args]
  `(let [config# timbre/*config*]
     (timbre/set-min-level! ~level)
     (let [result# (do ~@args)]
       (timbre/merge-config! config#)
       result#)))

(defmacro with-ns-level
  [ns level & args]
  `(let [config# timbre/*config*]
     (timbre/set-ns-min-level! ~ns ~level)
     (let [result# (do ~@args)]
       (timbre/merge-config! config#)
       result#)))

(defmacro trace
  [& args]
  `(timbre/trace ~@args))

(defmacro debug
  [& args]
  `(timbre/debug ~@args))

(defmacro info
  [& args]
  `(timbre/info ~@args))

(defmacro warn
  [& args]
  `(timbre/warn ~@args))

(defmacro error
  [& args]
  `(timbre/error ~@args))

(defmacro fatal
  [& args]
  `(timbre/fatal ~@args))

(defmacro report
  [& args]
  `(timbre/report ~@args))
