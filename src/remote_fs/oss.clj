(ns remote-fs.oss
  ^{:author "Jingcheng Yang<yjcyxky@163.com>"
    :description "Clojure Wrapper around oss-java client"}
  (:require [java-time :as t]
            [clojure.data.json :as json]
            [clojure.java.io :as io])
  (:import [com.aliyun.oss OSSClientBuilder OSSClient OSSException ClientException HttpMethod ClientBuilderConfiguration]
           [com.aliyun.oss.model ListObjectsRequest GeneratePresignedUrlRequest GetObjectRequest]
           [java.io InputStreamReader BufferedReader FileInputStream]
           [java.util Date]))

(defn connect
  [^String endpoint ^String access-key ^String secret-key]
  (let [conf (doto (new ClientBuilderConfiguration)
               (.setSupportCname false))]
    (. (new OSSClientBuilder) build endpoint access-key secret-key conf)))

(defn make-bucket
  "Creates a bucket with a name. Does nothing if one exists. Returns nil
   https://help.aliyun.com/document_detail/32012.html
  "
  [^OSSClient conn ^String name]
  (try
    (.createBucket conn name)
    name
    (catch OSSException _ nil)
    (catch ClientException _ nil)))

(defn list-buckets
  "Returns maps "
  [^OSSClient conn]
  (let [buckets (. conn listBuckets)]
    (map (fn [bucket] {"CreationDate" (str (.getCreationDate bucket))
                       "Name" (.getName bucket)}) buckets)))

(defn UUID []
  (java.util.UUID/randomUUID))

(defn now []
  (t/format "yMMd-HHmm"  (t/local-date-time)))

(defn put-object
  "Uploads a file object to the bucket. 
   Returns a map of bucket name and file name
  "
  ([conn ^String bucket ^String file-name]
   (let [upload-name (str  (now) "_" (UUID) "_" file-name)]
     (put-object conn bucket upload-name file-name)
     {:bucket bucket
      :name upload-name}))
  ([conn ^String bucket ^String upload-name ^String source-file-name]
   (.putObject conn bucket upload-name (new FileInputStream source-file-name))
   {:bucket bucket
    :name upload-name}))

(defn get-object
  "Takes connection and a map of [bucket name] keys as returned by (put-object) or explicit arguments 
   returns java.io.BufferedReader.
   Use clojure.java.io/copy to stream the bucket data files, or HTTP responses
  "
  ([conn {:keys [bucket name]}]
   (get-object conn bucket name))
  ([conn bucket name]
   (->> (.getObjectContent (.getObject conn bucket name))
        (new InputStreamReader))))

(defn download-object
  "Download object to a local path.
   See Docs: https://www.alibabacloud.com/help/zh/doc-detail/84824.htm
  "
  [conn bucket name localpath]
  (.getObject conn (new GetObjectRequest bucket name) (io/file localpath)))

(defn- objectStat->map
  "Helper function for datatype conversion"
  [stat]
  {:created-time (.getLastModified stat)
   :length (.getContentLength stat)
   :etag (.getETag stat)
   :content-type (.getContentType stat)
   :encryption-key (.getServerSideEncryption stat)
   ;; TODO: Any method to get the http-headers?
   :http-headers {}})

(defn get-object-meta
  "Returns object metadata as clojure hash-map"
  ([conn bucket name]
   (-> (.getObjectMetadata conn bucket name)
       objectStat->map
       (assoc :key name :name name :bucket bucket)))
  ([conn {:keys [bucket name]}]
   (get-object-meta conn bucket name)))

(defn- item->map
  [item]
  (if (= (type item) String)
    {:etag nil
     :key item
     :last-modified nil
     :ower nil
     :size 0
     :storage-class nil}
    {:etag (.getETag item)
     :key (.getKey item)
     :last-modified (.getLastModified item)
     :ower (.getOwner item)
     :size (.getSize item)
     :storage-class (.getStorageClass item)}))

(defn list-objects
  "
  "
  ([conn bucket]
   (map item->map (.getObjectSummaries (.listObjects conn bucket))))
  ([conn bucket filter]
   (map item->map (.getObjectSummaries
                   (.listObjects conn (doto (new ListObjectsRequest bucket)
                                        (.withPrefix filter))))))
  ([conn bucket filter recursive]
   (let [objects (.listObjects conn (doto (new ListObjectsRequest bucket)
                                      (.withPrefix filter)
                                      (.setDelimiter (if (some? recursive) "/" ""))))]
     (map item->map (concat (.getObjectSummaries objects) (.getCommonPrefixes objects))))))

(defn remove-bucket!
  "Removes the bucket form the storage"
  [conn bucket-name]
  (.deleteBucket conn bucket-name))

(defn remove-object! [conn bucket object]
  (.deleteObject conn bucket object))

(defn get-upload-url
  "Returns presigned and named upload url for direct upload from the client 
   See docs: https://help.aliyun.com/document_detail/32016.html
   You need to set Content-Type when you upload a file by js
  "
  ([conn bucket name]
   (let [timeout (Date. (+ (.getTime (Date.)) (* 1000 3600 24)))]
     (get-upload-url conn bucket name timeout)))
  ([conn bucket name timeout]
   (str (.generatePresignedUrl
         conn
         (doto (new GeneratePresignedUrlRequest bucket name (. HttpMethod PUT))
           (.setExpiration timeout)
           (.setContentType "application/octet-stream"))))))

(defn get-download-url
  "Returns a temporary download url for this object with 7day expiration
   See docs: https://help.aliyun.com/document_detail/32016.html
  "
  ([conn bucket name]
   (let [timeout (Date. (+ (.getTime (Date.)) (* 1000 3600 24)))]
     (get-download-url conn bucket name timeout)))
  ([conn bucket name timeout]
   (str (.generatePresignedUrl conn bucket name timeout))))

(defn set-bucket-policy
  "sets bucket policy map, takes Clojure persistant map, serializes it to json
   TODO: Any docs?
  "
  [conn ^clojure.lang.IPersistentMap policy]
  (.setBucketPolicy conn ((json/write-str policy))))

(defn init
  []
  {:make-bucket      make-bucket
   :connect          connect
   :list-buckets     list-buckets
   :put-object       put-object
   :get-object       get-object
   :download-object  download-object
   :list-objects     list-objects
   :remove-bucket    remove-bucket!
   :remove-object    remove-object!
   :get-upload-url   get-upload-url
   :get-download-url get-download-url
   :get-object-meta  get-object-meta})
