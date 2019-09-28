(ns yedata.core
  (:require [yedata.util :refer [slurp-from-classpath
                                 create-root-var]]
            [yedata.queryfile-parser :refer [parse-tagged-queries]]
            [yedata.data]))

(defn- gen-var [this]
  (let [f (fn [param conn]
            (yedata.data/execute (:statement this)
                                 param
                                 conn))]
    (create-root-var (:name this)
                     (with-meta f
                       {:name (:name this)
                        :arglist "Not supported yet."
                        ::source (str (:statement this))
                        :doc (or (:docstring this) "No doc.")}))))

(defn defqueries
  "Defines several query functions, as defined in the given SQL file.
  Each query in the file must begin with a `-- name: <function-name>` marker,
  followed by optional comment lines (which form the docstring), followed by
  the query itself."
  ([filename]
   (defqueries filename {}))
  ([filename options]
   (doall (->> filename
               slurp-from-classpath
               parse-tagged-queries
               (map #(gen-var %))))))


;; https://github.com/krisajenkins/yesql/pull/149
(defmacro require-sql
  "Require-like behavior for yesql, to prevent namespace pollution.
   Parameter is a list of [sql-source-file-name [:as alias] [:refer [var1 var2]]]
   At least one of :as or :refer is required
   Usage: (require-sql [\"sql/foo.sql\" :as foo-sql :refer [some-query-fn])"
  [[sql-file & {:keys [as refer]} :as require-args]]
  (when-not (or as refer)
    (throw (Exception. "Missing an :as or a :refer")))
  (let [current-ns (ns-name *ns*)
        ;; Keep this .sql file's defqueries in a predictable place:
        target-ns (symbol
                   (clojure.string/replace (str "yesquire/" sql-file)
                                           "/" "."))]
    `(do
       (ns-unalias *ns* '~as)
       (create-ns '~target-ns)
       (in-ns '~target-ns)
       (clojure.core/require '[yedata.core])
       (yedata.core/defqueries ~sql-file)
       (clojure.core/in-ns '~current-ns)
       ~(when as
          `(clojure.core/alias '~as '~target-ns))
       ~(when refer
          `(clojure.core/refer '~target-ns :only '~refer)))))
