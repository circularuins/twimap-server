(ns twimap-server.twitter
  (:gen-class)
  (:require [cheshire.core :as json]
            [twimap-server.mongodb :as mongo])
  (:import [twitter4j conf.ConfigurationBuilder TwitterFactory Twitter Query TwitterException]))

;; consumer-key, consumer-secret, access-token, access-token-secret の読み込み
(def auth-data (read-string (slurp "config/twitter.clj")))

(def date-format
  (doto (java.text.SimpleDateFormat. "yyyy/MM/dd hh:mm")
    (.setTimeZone (java.util.TimeZone/getTimeZone "JST"))))

(defn make-config []
  (let [config (new ConfigurationBuilder)]
    (. config setOAuthConsumerKey (auth-data :consumer-key))
    (. config setOAuthConsumerSecret (auth-data :consumer-secret))
    (. config setOAuthAccessToken (auth-data :access-token))
    (. config setOAuthAccessTokenSecret (auth-data :access-token-secret))
    (. config build)))

(defn make-twitter []
  (let [config (make-config)
        factory (new TwitterFactory config)]
    (. factory getInstance)))

(defn twitter-search
  "Use Twitter-search-API. Return map-array"
    [word]
  (let [twitter (make-twitter)
        query (new Query word)]
    (. query setCount 100)
    (def tweets (. (. twitter search query) getTweets))
    (for [tw tweets]
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
              [(.getId tw)
               (.. tw getUser getId)
               (.. tw getUser getName)
               (.. tw getUser getScreenName)
               (.. tw getUser getLocation)
               (.. tw getUser getProfileImageURL)
               (.. tw getUser getFollowersCount)
               (.getText tw)
               (if (.getGeoLocation tw)
                 (.. tw getGeoLocation getLatitude)
                 "0.0")
               (if (.getGeoLocation tw)
                 (.. tw getGeoLocation getLongitude)
                 "0.0")
               (.format date-format (.getCreatedAt tw))
               (str "https://twitter.com/"
                    (.. tw getUser getScreenName)
                    "/status/"
                    (.getId tw))]))))

(defn search-and-save
  "引数がキーワードDBに含まれるかチェック。ツイートの検索と保存も行う。"
  [word]
  (let [l-word (clojure.string/lower-case word)
        keyword (mongo/search-keyword l-word)
        tweets (twitter-search l-word)]
    (if (empty? keyword)
      ;; キーワードが未登録の場合
      (do
        (mongo/add-keyword l-word)
        (map mongo/add-tweet tweets (repeat ((first (mongo/search-keyword l-word)) :_id)))
        )
      ;; キーワードが既存データにに部分一致する場合
      (do
        (mongo/modi-keyword ((first keyword) :_id) l-word)
        (map mongo/add-tweet tweets (repeat ((first keyword) :_id)))
        ))))

(defn search
  "ツイート検索用API。mapを返す"
  [word]
  (doall (search-and-save word))  
  (if (empty? (mongo/search-keyword (clojure.string/lower-case word)))
    {:tweets nil}
    (let [result (mongo/search-tweet-by-keyword (.toString ((first (mongo/search-keyword (clojure.string/lower-case word))) :_id)))]
      (array-map
       :tweets
       (concat
        (take 100 (filter #(and (= (:latitude %) "0.0") (= (:longitude %) "0.0")) result))
        (filter #(or (not= (:latitude %) "0.0") (not= (:longitude %) "0.0")) result))))))
