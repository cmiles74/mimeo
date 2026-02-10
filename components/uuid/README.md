# UUID

This component contains functions to make using UUIDs easier. It's thing wrapper around the [Jackson project's UUID generator][java-uuid].

## Getting Started

You will want to include the interface for this component so that you can call it's methods.

```clojure
(require [nervestaple.mimeo.uuid.interface :as uuid])
```

Now you can generate version 7 UUIDs. 

```clojure
(uuid/generate-sequential) 

#uuid "019c48ed-2de0-7577-b1cc-af2094268f43"
```

Check out the
[`interface.clj`](src/nervestaple/mimeo/uuid/interface.clj) file for more details.

----
[java-uuid]: https://github.com/cowtowncoder/java-uuid-generator
