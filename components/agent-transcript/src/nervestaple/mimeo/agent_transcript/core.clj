(ns nervestaple.mimeo.agent-transcript.core)

(defn transcript-middleware [handler]
  (fn
    ([request]
     (let [transcript (or (get-in request [:session :transcript]) [])]
       (handler (assoc-in request [:session :transcript]
                          (conj transcript {:type :request
                                            :message (or (request :message-original)
                                                         (request :message))})))))
    ([request response]
     (let [transcript (or (get-in response [:session :transcript]) [])
           response-out (handler request response)
           message (merge {:type :response
                           :message (or (response :response-original)
                                        (response :response))}
                          (when (response :tool-call)
                            {:tool-call (response :tool-call)}))]
       (assoc-in response-out [:session :transcript]
                 (conj transcript message))))))

(defn transcript-item->header [item]
  (cond (item :tool-call)
        "YOU (to yourself)"

        (= :response (item :type))
        "YOU"

        :else "THEM"))

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
