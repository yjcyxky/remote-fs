(ns remote-fs.util)

(defn in-coll?
  [key coll]
  (>= (.indexOf coll key) 0))

(defn filter-buckets
  [coll filter-list mode]
  (if (= mode "white")
    (filter (fn [item] (in-coll? (get item "Name") filter-list)) coll)
    (filter (fn [item] (not (in-coll? (get item "Name") filter-list))) coll)))

(defn filter-by-whitelist
  "Save all items in whitelist."
  [coll whitelist]
  (filter-buckets coll whitelist "white"))

(defn filter-by-blacklist
  "Remove all items in blacklist."
  [coll blacklist]
  (filter-buckets coll blacklist "black"))
