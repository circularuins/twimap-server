(ns twimap-server.mongodb
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [monger.operators :refer :all]
            [clj-time.core :as t]
            [clj-time.format :as f])
  (:import [com.mongodb MongoOptions ServerAddress]
           [org.bson.types ObjectId]))

(def db (mg/get-db (mg/connect) "twi-map"))

(defn now-time []
  ;; JSTで現在時刻を取得
  (t/to-time-zone (t/now) (t/time-zone-for-offset +9)))

(defn search-tweet [word]
  (let [coll "tweet"]
    ;; 正規表現を利用した、キーワードの部分一致検索
    ;; screenName userName text のどれかにマッチするものを返す
    (mc/find-maps db coll { $or [{:screenName {$regex (str ".*" word ".*")}}
                                 {:userName {$regex (str ".*" word ".*")}}
                                 {:text {$regex (str ".*" word ".*")}}]})))

(defn search-tweet-by-keyword [id]
  (let [coll "tweet"
        result (mc/find-maps db coll {:keyId id})]
    (for [re result]
      (zipmap [:tweetId
             :userId
             :userName
             :screenName
             :location
             :imageUrl
             :followers
             :text
             :latitude
             :longitude
             :date
             :tweetUrl]
            [(re :tweetId)
             (re :userId)
             (re :userName)
             (re :screenName)
             (re :location)
             (re :imageUrl)
             (re :followers)
             (re :text)
             (re :latitude)
             (re :longitude)
             (re :date)
             (re :tweetUrl)]))))

(defn add-tweet [data key-id]
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
                :tweetUrl (data :tweetUrl)
                :keyId (.toString key-id)}
               {:upsert true})))

(defn search-keyword [word]
  (let [coll "keyword"]
    (mc/find-maps db coll {:keyword {$regex (str ".*" word ".*")}})))

(defn add-keyword [word]
  (let [coll "keyword"]
    (mc/insert db coll {:_id (ObjectId.)
                        :keyword word
                        :created_on (.toString (now-time))
                        :updated_on (.toString (now-time))
                        })))

(defn modi-keyword [id word]
  (let [coll "keyword"]
    (mc/update-by-id db coll id { $set {:keyword word
                                        :updated_on (.toString (now-time))}})))
