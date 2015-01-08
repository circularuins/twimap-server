(ns twimap-server.mongodb
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [monger.operators :refer :all])
  (:import [com.mongodb MongoOptions ServerAddress]))

(def db (mg/get-db (mg/connect) "twi-map"))

(defn add-tweet [data]
  (let [coll "tweet"]
    ;; Tweetのidが存在していたらupdate、存在していなかったらinsart
    (mc/update db coll {:tweetId (data :tweetId)}
               {:tweetId (data :tweetId)
                :userId (data :userId)
                :userName (data :userName)
                :screenName (data :screenName)
                :location (data :location)
                :imageUrl (data :imageUrl)
                :followers (data :followers)
                :text (data :text)
                :latitude (data :latitude)
                :longitude (data :longitude)
                :date (data :date)
                :tweetUrl (data :tweetUrl)}
               {:upsert true})))

(defn search-tweet [word]
  (let [coll "tweet"]
    ;; 正規表現を利用した、キーワードの部分一致検索
    ;; screenName userName text のどれかにマッチするものを返す
    (mc/find-maps db coll { $or [{:screenName {$regex (str ".*" word ".*")}}
                                 {:userName {$regex (str ".*" word ".*")}}
                                 {:text {$regex (str ".*" word ".*")}}]})))
