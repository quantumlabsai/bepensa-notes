(ns query-rx-sensitivity
  (:gen-class)
  (:import (com.impinj.octane ImpinjReader
                              TagReportListener
                              KeepaliveListener
                              ConnectionLostListener
                              GpoMode)))

(defn -main [& args]
  (when (empty? args)
    (println "Uso: clj -M -m query-rx-sensitivity <ip-lectora>")
    (System/exit 1))

  (let [ip (first args)
        reader (ImpinjReader.)]

    (try
      (.connect reader ip)

      (println "Conectado a" ip)

      (let [features (.queryFeatureSet reader)
            values (.getRxSensitivities features)]

        (println "Rx Sensitivity soportados:")

        (doseq [v values]
          (println " " v "dBm")))

      (.disconnect reader)

      (catch Exception e
        (println (.getMessage e))

        (try
          (.disconnect reader)
          (catch Exception _))))))