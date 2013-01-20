(ns cvf.core
  (:require [tentacles.core :as tentacles]
            [tentacles.repos :refer (specific-repo contributors)]
            [clojure.walk :refer (postwalk)]))

(defn get-auth []
  (or (System/getenv "GITHUB_AUTH") (.trim (slurp "auth"))))

(def options {:per-page 100
              :all-pages true
              :auth (get-auth)})

(def db (atom {}))

(def repo-re #"(?i)^([a-z0-9]+)/([a-z0-9]+)$")

(defn clean [data]
  (postwalk
    (fn [x]
      (if (map? x)
        (let [ks (filter #(.endsWith (name %) "url") (keys x))]
          (apply dissoc x ks))
        x))
    data))

(defn add-repo! [qualified-name]
  (if-let [[_ user repo] (re-matches repo-re qualified-name)]
    (let [data {:info (specific-repo user repo options)
                :contributors (contributors user repo options)}]
      (if (some #(or (:message %) (:status %)) (vals data))
        :github-error
        (let [id (-> data :info :id)
              data (clean data)]
          (swap! db assoc id data)
          id)))
      :bad-repo-name))

(defn stats []
  (for [{:keys [info contributors]} (vals @db)]
    {:id (:id info)
     :url (:url info)
     :repo (str (-> info :owner :login) "/" (:name info))
     :forks (:forks info)
     :watchers (:watchers_count info)
     :contributors (count contributors)}))

;(add-repo! "clojure/clojure")
;(add-repo! "clojure/clojurescript")
;(add-repo! "brandonbloom/fipp")
;(add-repo! "brandonbloom/factjor")
