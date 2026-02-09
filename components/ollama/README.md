# Ollama

This component provides functions that make it easy to work with an Ollama server. Under-the-hood we're using [Ollama4J][ollama4j] to communicate with the Ollama instance.

## Getting Started

You will want to include the interface for this component so that you can call it's methods.

```clojure
(require [nervestaple.mimeo.ollama.interface :as ollama])
```

You will next want to to connect to your Ollama server. Most often this is a service running on your local machine.

```clojure
(def connection (ollama/connect "http://localhost:11434"))
```

You can now fetch a list of available models for your instance. Here we simply pick the first model in that list.

```clojure
(def model (first (ollama/models connection)))
```

You will have a map will all of the features of the model. Here's an example from my laptop.

```edn
{:modified_at 1.77005164898113E9,
 :expires_at nil,
 :name "gemma3:12b",
 :digest "f4031aab637d1ffa37b42570452ae0e4fad0314754d17ded67322e4b95836f8a",
 :modelName "gemma3",
 :size 8149190253,
 :details
 {:format "gguf",
  :family "gemma3",
  :parameter_size "12.2B",
  :quantization_level "Q4_K_M",
  :families ["gemma3"]},
 :modelVersion "12b",
 :model "gemma3:12b"}
 ```

 You can also fetch one model by name or a list of them by family. 😉 With a connection and model in hand we can now prompt the model for output!

 ```clojure
(ollama/prompt connection model "Hello!")
 ```

The result is a map with the output from the model. 🎉

```edn
{:response "Hello to you too! 😊 \n\nHow can I help you today?",
 :evalCount 16,
 :done true,
 :totalDuration 5574777542,
 :promptEvalDuration 256387709,
 :doneReason "stop",
 :thinking "",
 :createdAt "2026-02-08T20:16:02.443301Z",
 :loadDuration 4174948708,
 :responseTime 5760,
 :context
 [105
  2364
  107
  9259
  236888
  106
  107
  105
  4368
  107
  9259
  531
  611
  2311
  236888
  103453
  236743
  108
  3910
  740
  564
  1601
  611
  3124
  236881],
 :evalDuration 1121931376,
 :httpStatusCode 200,
 :promptEvalCount 11,
 :model "gemma3:12b"}
 ```

Pretty exciting! Check out the
[`interface.clj`](src/nervestaple/mimeo/ollama/interface.clj) file for all the
deets.

----
[ollama4j]: https://ollama4j.github.io/ollama4j/
