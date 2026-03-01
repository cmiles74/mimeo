# AGENTS.md

This document helps AI agents work effectively in the Mimeo codebase.

## Project Overview

Mimeo is an experimental Clojure project for building LLM-powered agents that communicate with local Ollama servers. The goal is to create a simple agent harness enabling multiple agents to talk to each other. Uses `core.async` channels for request/response handling with composable middleware.

## Architecture: Polylith Monorepo

This project uses the [Polylith](https://polylith.gitbook.io/polylith) architecture - a monorepo structure with strict boundaries between components.

### Directory Structure

```
mimeo/
├── bases/           # Deployable entry points (contain -main)
│   └── mimeo/       # Main CLI chat application
├── components/      # Reusable building blocks
│   ├── agent/              # Agent loop and lifecycle
│   ├── agent-session-memory/  # In-memory session storage
│   ├── agent-transcript/      # Conversation transcript middleware
│   ├── log/                # Logging (wraps taoensso/timbre)
│   ├── ollama/             # Ollama API client (ollama4j)
│   └── uuid/               # UUID generation (v7 sequential)
├── projects/        # Deployable artifacts
│   └── mimeo/       # Main uberjar project
├── development/     # REPL development namespace
├── deps.edn         # Root dependencies and aliases
├── workspace.edn    # Polylith workspace config
└── build.clj        # Build script
```

### Key Polylith Conventions

- **Top namespace**: `nervestaple.mimeo`
- **Interface pattern**: Each component has `interface.clj` exposing public API
- **Implementation**: `core.clj` contains private implementation
- **Never import `core` directly** - always use the interface namespace

```clojure
;; CORRECT
(:require [nervestaple.mimeo.agent.interface :as agent])

;; WRONG - never do this
(:require [nervestaple.mimeo.agent.core :as agent])
```

## Essential Commands

### Development REPL

```bash
# Start REPL with all components loaded
clj -A:dev
```

Then work from `development/src/cmiles74.clj` which has examples pre-configured.

### Testing

```bash
# Run all tests (requires xvfb on Linux/CI for headless)
clojure -M:poly test :all

# Check architecture validity
clojure -M:poly check
```

### Building

```bash
# Build uberjar for a project
clj -T:build uberjar :project mimeo

# Run the built jar
java -jar projects/mimeo/target/mimeo.jar
```

### Polylith Commands

```bash
# Check workspace health
clojure -M:poly check

# See what's changed
clojure -M:poly diff

# View project info
clojure -M:poly info
```

## Code Patterns

### Middleware Pattern

Agent handlers use Ring-style middleware composition. Middleware wraps a handler function and can:
1. Transform requests (arity-1 function)
2. Transform responses (arity-2 function)

```clojure
;; Middleware factory returns a wrapper function
(defn my-middleware [handler]
  (fn
    ([request]           ; Pre-process request
     (handler (transform-request request)))
    ([request response]  ; Post-process response
     (handler request (transform-response response)))))

;; Composition with threading
(-> (agent/null-handler)
    (transcript/fill-transcript-middleware)
    (transcript/transcript-middleware)
    (session/session-middleware))
```

### Agent Lifecycle

```clojure
;; 1. Connect to Ollama
(def conn (ollama/connect "http://localhost:11434" 300))

;; 2. Define agent with model and system prompt
(def my-agent (agent/define-agent conn "gemma3:12b" "You are helpful."))

;; 3. Start agent loop (returns channels)
(def session (agent/start-agent my-agent handler))

;; 4. Send/receive via channels
(async/>!! (:request-channel session) {:message "Hello"})
(async/<!! (:response-channel session))

;; 5. Shutdown
(agent/shutdown-agent session)
```

### Session Data Flow

Request/response maps carry session state:

```clojure
{:message "user input"
 :session-id #uuid "..."
 :session {:transcript [{:type :request :message "..."}
                        {:type :response :message "..."}]}}
```

## Dependencies

### Key Libraries

| Library | Purpose |
|---------|---------|
| `io.github.ollama4j/ollama4j` | Ollama API client (Java interop) |
| `org.clojure/core.async` | Async channels for agent communication |
| `metosin/jsonista` | Fast JSON encoding/decoding |
| `taoensso/timbre` | Logging |
| `com.fasterxml.uuid/java-uuid-generator` | UUID v7 generation |

### Adding Dependencies

- Component-level deps go in `components/<name>/deps.edn`
- Root `deps.edn` aggregates components via local deps
- Project `deps.edn` pulls in needed components

## Testing Patterns

- Tests live in `components/<name>/test/` mirroring src structure
- Test namespaces end with `-test` suffix
- Uses `clojure.test`

```clojure
(ns nervestaple.mimeo.log.interface-test
  (:require [clojure.test :refer [deftest is]]
            [nervestaple.mimeo.log.interface :as log]))

(deftest my-test
  (is (= expected actual)))
```

## Configuration

### Ollama Connection

The project expects an Ollama server. Default in examples is `http://friday.nervestaple.com:11434` with a 300s timeout. Modify connection URL as needed:

```clojure
(ollama/connect "http://localhost:11434" 60)
```

### Models

Use `ollama/name->model` to look up model by name, or `ollama/family->model` by family:

```clojure
(ollama/name->model conn "gemma3:12b")
(ollama/family->model conn "deepseek-r1")
```

## Gotchas and Non-Obvious Patterns

1. **Interface files expose macros**: The `log` component wraps Timbre macros. Import the interface, not core, to get proper macro expansion.

2. **Middleware order matters**: Later middleware in the threading chain wraps earlier ones. Session middleware should typically be outermost.

3. **Channel blocking**: `async/>!!` and `async/<!!` are blocking operations. Don't use in go blocks (use `>!` / `<!` instead).

4. **Java interop in ollama**: The Ollama component uses `ollama4j` Java library. JSON responses are converted to Clojure maps via jsonista.

5. **Duplicate function definition**: `components/agent-transcript/core.clj` has `transcript-middleware` defined twice (line 3 and 35). The second definition shadows the first.

6. **Unused imports**: Several files have unused namespace imports (shown as warnings by clj-kondo). These are safe to ignore or clean up.

## CI/CD

GitHub Actions workflow (`.github/workflows/ci.yml`) runs on push to `main` or `release`:

1. Resolves dependencies: `clojure -A:dev:test -P`
2. Runs tests: `xvfb-run clojure -M:poly test :all`
3. Checks architecture: `clojure -M:poly check`

Note: `xvfb-run` is used because some transitive dependencies may require a display.

## File Naming

- Clojure namespaces use hyphens: `agent-transcript`
- Filesystem uses underscores: `agent_transcript/`
- This is standard Clojure convention
