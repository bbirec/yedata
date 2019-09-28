(ns yedata.data
  (:require [clojure.data.json :as json])
  (:import [com.amazonaws.services.rdsdata
            AWSRDSDataClient
            AWSRDSData]
           [com.amazonaws.services.rdsdata.model
            ExecuteStatementRequest
            ExecuteStatementResult
            Field
            SqlParameter]
           [com.amazonaws.auth
            AWSStaticCredentialsProvider
            BasicAWSCredentials]
           [com.amazonaws.regions Regions]))

(defn create-conn
  [access-key secret-key resource-arn secret-arn db-name]
  {:client (-> (doto (AWSRDSDataClient/builder)
                 (.withCredentials
                  (AWSStaticCredentialsProvider.
                   (BasicAWSCredentials.
                    access-key
                    secret-key)))
                 (.withRegion Regions/US_EAST_1))
               (.build))
   :conn {:resource-arn resource-arn
          :secret-arn secret-arn
          :db-name db-name}})

(defn- v->f [v]
  (cond
    (nil? v)
    (-> (Field.) (.withIsNull true))
    
    (string? v)
    (-> (Field.) (.withStringValue v))

    (instance? Boolean v)
    (-> (Field.) (.withBooleanValue v))

    (integer? v)
    (-> (Field.) (.withLongValue v))

    (number? v)
    (-> (Field.) (.withDoubleValue v))

    (instance? java.sql.Timestamp v)
    (-> (Field.) (.withStringValue (.toString v)))

    (vector? v)
    (-> (Field.) (.withStringValue (json/write-str v)))

    (map? v)
    (-> (Field.) (.withStringValue (json/write-str v)))

    :else
    (throw (Exception. "Unsupported type"))))

(defn- f->v [f]
  (cond
    (.isNull f)
    nil

    :else
    (or
     (.getBooleanValue f)
     (.getStringValue f)
     (.getLongValue f)
     (.getDoubleValue f))))

(defn- gen-params [param]
  (for [[k v] param]
    (let [f (v->f v)]
      (-> (SqlParameter.)
          (.withName (name k))
          (.withValue f)))))

(defn- new-req [conn]
  (-> (ExecuteStatementRequest.)
      (.withResourceArn (:resource-arn conn))
      (.withSecretArn (:secret-arn conn))
      (.withDatabase (:db-name conn))))

(defn- col-info [metas]
  (for [cm metas]
    {:name (.getName cm)
     :type (.getTypeName cm)}))

(defn- read-col
  "col-type에 따라 v를"
  [col-type v]
  (if (nil? v)
    v
    (condp = col-type
      "timestamp" (java.sql.Timestamp/valueOf v)
      "json" (json/read-str v :key-fn keyword)
      "jsonb" (json/read-str v :key-fn keyword)
      v)))

(defn execute [sql param conn]
  (let [req (-> (new-req (:conn conn))
                (.withSql sql)
                (.withIncludeResultMetadata true)
                (.withParameters (gen-params param)))
        result (.executeStatement (:client conn) req)
        cols (col-info (.getColumnMetadata result))
        records (.getRecords result)]
    (when records
      (for [row records]
        (into {} (map
                  (fn [[{name :name
                         t :type} v]]
                    [(keyword name) (read-col t v)])
                  (zipmap cols (for [f row]
                                 (f->v f)))))))))


