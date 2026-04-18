(ns nervestaple.mimeo.agent-transcript.core)

(defn transcript-tool-call
  [response]
  (let [request (merge {:type :tool-request
                        :message (response :tool-call)})
        message (merge {:type :tool-response
                        :continue true
                        :message (or (response :response-original)
                                     (response :response))})]
    [request message]))

(defn transcript-response
  [response]
  [{:type :response
    :continue false
    :message (or (response :response-original)
                 (response :response))}])

(defn transcript-middleware [handler]
  (fn
    ([request]
     (let [transcript (or (get-in request [:session :transcript]) [])]
       (handler (if (= :continue (request :message-original))
                  request
                  (assoc-in request [:session :transcript]
                            (conj transcript {:type :request
                                              :message (or (request :message-original)
                                                           (request :message))}))))))
    ([request response]
     (let [transcript (or (get-in response [:session :transcript]) [])
           response-out (handler request response)
           messages-out (if (response :tool-call)
                          (transcript-tool-call response)
                          (transcript-response response))]
       (assoc-in response-out [:session :transcript]
                 (into transcript messages-out))))))

(defn parse-request [request]
  (let [message (or (request :message-original) (request :message))]
    (merge {:message message}
           (select-keys request [:type]))))

(defn parse-response [response]
  (let [response-message (or (response :response-original) (response :response))]
    (merge {:response response-message}
           (select-keys response [::type :evalCount :done :totalDuration
                                  :promptEvalDuration :doneReason :createdAt
                                  :loadDuration :responseTime]))))

(defn transcript-item->header [item]
  (cond (= :tool-request (item :type))
        "TOOL_USE"

        (= :tool-response (item :type))
        "TOOL_RESULT"

        (= :response (item :type))
        "ASSISTANT"

        :else "USER"))

(defn transcript->text [transcript]
  (if (and transcript (< 1 (count transcript)))
    (let [out (str "Attached below is a transcript of the conversation so far.\n\n"
                   "```\n"
                   (apply str
                          (for [item transcript]
                            (str (transcript-item->header item) ": "
                                 (item :message)
                                 "\n\n")))
                   "```\n\n")]
      out)
    ""))

(defn fill-transcript-middleware [handler]
  (fn
    ([request]
     (handler
      (merge request {:message-original (request :message)
                      :message (str (transcript->text
                                     (get-in request [:session :transcript]))
                                    (request :message))})))
    ([request response]
     (handler (-> request
                  (merge {:message (:message-original request)})
                  (dissoc :message-original))
              response))))
