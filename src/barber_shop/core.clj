(ns barber-shop.core
  (:require [clojure.core.async :as as]))

(def ^:dynamic activities nil)

;; Logging
(defn log [& args]
  (as/>!! activities (apply str (interpose " " args))))

(defn start-activities-logger []
  (as/thread
    (loop []
      (when-let [activity (as/<!! activities)]
        (do
          (println activity)
          (recur))))))

;; Shop
(defn start-barber [seats]
  (as/thread
    (log "Barber arrived")
    (loop []
      (when-let [customer (as/<!! seats)]
        (log "Customer" customer "is getting their hair cut")
        (Thread/sleep (rand-int 5000))
        (log "Customer" customer "has finished getting their hair cut")
        (recur)))))

(defn close-shop [seats barber-finished]
  (log "Closing shop")
  (as/close! seats)
  (as/<!! barber-finished)
  (log "Barber leaves")
  (as/close! activities))

(defn open-shop [seats]
  (as/thread
    (loop [customer 1]
      (Thread/sleep (rand-int 2000))
      (let [[action _] (as/alts!! [[seats customer]]
                                  :default :busy)]
        (case action
          :busy (log "Cusomter" customer "leaves the shop as it is busy")
          false (log "Customer" customer "is turned away - the shop is shutting")
          (log "Customer" customer "enters the shop"))
        (when action (recur (inc customer)))))))

(defn simulate-trading-day []
  (binding [activities (as/chan 10)]
    (start-activities-logger)
    (let [seats (as/chan 3)
          barber-finished (start-barber seats)]
      (open-shop seats)
      (Thread/sleep 10000)
      (close-shop seats barber-finished))))
