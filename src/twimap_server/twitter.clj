(ns twimap-server.twitter
  (:gen-class)
  (:require [cheshire.core :as json])
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

(defn search-keyword [word]
  (let [twitter (make-twitter)
        query (new Query word)]
    (. query setCount 100)
    (def tweets (. (. twitter search query) getTweets))
    (array-map :tweets (for [tw tweets]
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
                                       (.getId tw))])))))
