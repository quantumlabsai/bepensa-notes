(ns main
  (:require
   [constants :as C]
   [env.constants :as EC]
   [send-events :as SE]
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

(log/info "Starting streamer")
(log/info (pr-str {:CAUDAL_HOME C/CAUDAL_HOME}))

(def origin (-> (InetAddress/getLocalHost)
                (.getHostName)))

(def plantId "Beppemsa1")

(defn create-uuid [] (str (UUID/randomUUID)))

(defn set-defaults [defaults event]
  (merge defaults event))

(defn e-counter [{:keys [n last] :or {n 0 last -1}} e]
  (when EC/WITH-E-COUNTER
    (let [now (mod (System/currentTimeMillis) 10000)]
      (if (>= now last)
        {:n (inc n) :last now}
        (let [rt (Runtime/getRuntime)
              nf (java.text.NumberFormat/getNumberInstance)
              maxM (.maxMemory rt)
              freeM (.freeMemory rt)
              totM (.totalMemory rt)
              usedM (- totM freeM)
              doGC false ;(< freeM (* totM 0.50))
              info [:max (.format nf maxM) :tot (.format nf totM) :free (.format nf  freeM) :used (.format nf usedM) :GC doGC]]
          (when doGC (.gc rt))
          (log/info (str "evts/s: " (/ n 10.0) " --> " info))
          {:n 1 :last now})))))

(defn print-it [{:keys [controler-name AntennaPortNumber PeakRssiInDbm d-id event rfid-ts] :as e}]
  (log/info (format "%-10s %-5s %-10s %-20s %s %s" controler-name AntennaPortNumber PeakRssiInDbm event d-id rfid-ts)))

; EVENTE ES ON_TAG_READ | ON_TAG_REMOVED | ON_TAG_ERROR
(defn tag-reducer [{:keys [last-d-id last-entry-ts]} {:keys [d-id event entry-ts] :as e}]
  (if (= event :ON_TAG_READ)
    (if (or (not= d-id last-d-id) (> entry-ts (+ last-entry-ts C/DELTA-REPEAT-TAG)))
      (assoc e :last-d-id d-id :last-entry-ts entry-ts :send-tag true :uuid (create-uuid))
      (assoc e :last-d-id d-id :last-entry-ts last-entry-ts))
    (assoc e :last-d-id last-d-id :last-entry-ts last-entry-ts)))

(defn create-chanel-id [controler-name antena]
  (cond (= controler-name "salida") (str controler-name antena)
        (= controler-name "entrada1") "entrada1"
        (= controler-name "entrada2") "entrada2"
        :else (log/error "no puedo crear nombre con " controler-name antena)))

(defsink example 1 ;; backpressure
  ;; streamer
  (smap
   [set-defaults {:plantId plantId
                  :origin origin}]
   (time-stampit
    [:entry-ts]
    ;(reduce-with [:counter e-counter])
    ;(smap [#(log/info (pr-str [:antes-tag-reducer  %]))])
    (smap
     [(fn [{:keys [controler-name AntennaPortNumber] :as e}]
        (let [id (create-chanel-id controler-name AntennaPortNumber)]
          (assoc e :channel-id id :lane id)))]
     (by
      [:channel-id]
      (reduce-with
       [:tag-reducer tag-reducer]
     ;(smap [#(log/info (pr-str [:tag-reducer %]))])
       (where
        [:send-tag]
        (smap
         [SE/send-events]
         (smap [print-it])))))))))


; OJO debemos permitir algun tipo de manejo de las regex por planta mañana lo defino hoy es: (2026-06-03)
;
;

(defn get-prefix []
  (-> (slurp "C:/quantumlabs/cauda-rfid/config/rfid-tag.regex")
      clojure.string/split-lines
      first
      clojure.string/trim))

;; Listener
(deflistener rfid-salida [{:type 'caudal.io.rfid-server
                           :parameters {:controler-info {:name "salida"
                                                         :plant 43}
                                        :controler "10.180.10.132"
                                        :inactivity 900000
                                        :RfMode 1002
                                        :cleanup-delta 120000
                                        :chan-buf-size 1
                                        :fastId false
                                        :d-id-re (get-prefix) ;"AABB.*"
                                        :keepalive-ms 60000
                                        :antennas [[1 24 -70] [2 24 -70]]
                                        :tag-policy {:type :last
                                                     :delta 3000
                                                     :directrion :approaching}}}])
; en antennas va por cada antena un vector con (id, tx power,rx sendibility) [id nil|true|real nil|true|int-dbm]

(deflistener rfid-entrada1 [{:type 'caudal.io.rfid-server
                             :parameters {:controler-info {:name "entrada1"
                                                           :plant 43}
                                          :controler "10.180.10.131"
                                          :inactivity 900000
                                          :RfMode 1002
                                          :cleanup-delta 120000
                                          :chan-buf-size 1
                                          :fastId false
                                          :d-id-re (get-prefix) ;"AABB.*"
                                          :keepalive-ms 60000
                                          :antennas [[1 28 -80] [2 28 -80]]
                                          :tag-policy {:type :last
                                                       :delta 3100}}}])

(deflistener rfid-entrada2 [{:type 'caudal.io.rfid-server
                             :parameters {:controler-info {:name "entrada2"
                                                           :plant 43}
                                          :controler "10.180.10.133"
                                          :inactivity 900000
                                          :RfMode 1002
                                          :cleanup-delta 120000
                                          :chan-buf-size 1
                                          :fastId false
                                          :d-id-re (get-prefix) ;"AABB.*"
                                          :keepalive-ms 60000
                                          :antennas [[1 28 -80] [2 28 -80]]
                                          :tag-policy {:type :last
                                                       :delta 3200}}}])


;;Wires our listener with the streamers
(wire [rfid-entrada1 rfid-entrada2 rfid-salida] [example])
#_(wire [rfid-entrada1 rfid-salida] [example])
#_(wire [rfid-salida] [example])

 ;(config-view [example] {:doughnut {:state-counter {:value-fn :n :tooltip [:n]}}})

(web {:http-port 9910
      :publish-sinks [example]})
