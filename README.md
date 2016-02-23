# om-datascript-sente-datomic-playground
Personal integration for Om.next + Datascript + Sente + Datomic.

# How to fire up?

## Launch your REPL.
```shell
lein repl
```

Basic setup located in `user.clj`, so it will be loaded automatically.

## Make it run.
```clojure
(go)

;; (reset) will recreate a new memory datomic.
```

## Open in browser.
```
http://localhost:3449
```

## What is still missing now?
Pushing data from server-side.
