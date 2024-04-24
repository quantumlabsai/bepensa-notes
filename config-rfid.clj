(ns main
   (:require
    ;[constants :as C]
    ;[env.constants :as EC]
    [clojure.java.io :as io]
    [clojure.tools.logging :as log]
    ;[clojure.core.match :refer [match]]
    [clojure.pprint :as pp]
    [caudal.streams.common :refer [defsink deflistener wire]]
    [caudal.io.rest-server :refer [web]]
    [caudal.streams.stateful :refer [reduce-with changed counter]]
    [caudal.streams.stateless :refer [by pprinte where split smap time-stampit ->INFO ->WARN ->ERROR reinject join unfold]]
    [caudal.io.telegram :refer [send-photo send-text]]
    [cheshire.core :refer [parse-string generate-string]]
    ;[send-events :as SE]
    ;[img-util :as IU]
    )
   (:import
    (java.util Random UUID)
    (java.util Base64)
    (java.net InetAddress)))
 
 ;; Listener
 (deflistener rfid [{:type 'caudal.io.rfid-server
                     :parameters {:controler-name "controladorai-01"
                                  :controler "192.168.1.11"
                                  :RfMode 1002
                                  :cleanup-delta 1000
                                  :chan-buf-size 10
                                  :fastId false
                                  :d-id-re "E2004.*"
                                  :antennas [1 2]}}])
 
 (defn print-it [{:keys [AntennaPortNumber PeakRssiInDbm d-id event] :as e}]
   (log/info (format "%-5s %-10s %-20s %s" AntennaPortNumber PeakRssiInDbm event d-id)))
 
 (defsink example 1 ;; backpressure
   ;; streamer
   (counter
    [:state-counter :event-counter]
    (smap [print-it])
    #_(->INFO [:all])))
 
 ;; Wires our listener with the streamers
 (wire [rfid] [example])
 
 ;(config-view [example] {:doughnut {:state-counter {:value-fn :n :tooltip [:n]}}})
 
 (web {:http-port 9910
       :publish-sinks [example]})