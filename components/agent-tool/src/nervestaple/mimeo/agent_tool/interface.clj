(ns nervestaple.mimeo.agent-tool.interface
  (:require
   [nervestaple.mimeo.agent-tool.core :as core]))

(defmacro fn->str
  [fn-this]
  `(core/fn->str ~fn-this))

(defn tool-middleware
  [handler tool-spec]
  (core/tool-middleware handler tool-spec))

(defn call-tool-middleware
  [handler]
  (core/call-tool-middleware handler))
