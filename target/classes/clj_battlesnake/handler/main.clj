(do (clojure.core/ns clj-battlesnake.handler.main (:gen-class)) (clojure.core/defn -main [] ((do (clojure.core/require (quote ring.server.leiningen)) (clojure.core/resolve (quote ring.server.leiningen/serve))) (quote {:ring {:handler clj-battlesnake.handler/app, :port 80, :open-browser? false, :stacktraces? false, :auto-reload? false, :auto-refresh? false}}))))
