(ns strava-summary.core.db
  (:require [clojure.java.jdbc :as jdbc]
            [clj-time.core :as t :exclude [extend second]]
            [clj-time.coerce :as c]
            [clj-time.format :as f]
            [clojure.tools.logging :as log]
            ))

(def db-spec
  {:classname "org.sqlite.JDBC"
   :subprotocol "sqlite"
   :subname "db/database.db"})

(defn create-db []
  (try (jdbc/db-do-commands db-spec
                       (jdbc/create-table-ddl :activities
                                         [:athlete_id :int]
                                         [:activity_id :int]
                                         [:activity_type :text]
                                         [:start_date_epoch :int]
                                         [:distance :double]
                                         [:duration :real]
                                         [:elevation :real]
                                         [:kudos_count :int]))))

(defn athlete-first-last-activity [athlete-id]
  (let [rows (jdbc/query db-spec ["SELECT athlete_id, MIN(start_date_epoch) as first, MAX(start_date_epoch) as last FROM activities WHERE athlete_id = ? LIMIT 1" athlete-id])
        ]
    (if (nil? (:athlete_id (first rows)))
      nil
      {:athelete-id (:athlete-id (first rows))
       :first-activity-epoch (:first (first rows))
       :last-activity-epoch (:last (first rows))
       })
    ))

(defn get-summary-activities [athlete-id from to]
  (let [rows (jdbc/query db-spec ["SELECT SUM(distance) as distance, SUM(duration) as duration, SUM(elevation) as elevation, SUM(kudos_count) as kudos_count, activity_type FROM activities WHERE athlete_id = ? AND start_date_epoch >= ? AND start_date_epoch <= ? GROUP BY activity_type" athlete-id from to])
        ]
    ;; [{:activity_type "Ride" :distance xx : duration yy ... }
    ;; {:activity_type "Swim" :distance xx  } ]
   
  rows
    )
  )


(defn activity-to-row [activity]
  {:athlete_id (:id (:athlete activity))
   :activity_id (:id activity)
   :activity_type (:type activity)
   :start_date_epoch (:start_date_epoch activity)
   :distance (:distance activity)
   :duration (:elapsed_time activity)
   :elevation (:total_elevation_gain activity)
   :kudos_count (:kudos_count activity)
   }
  )
(defn insert-activities [activities]
  (log/info (map activity-to-row activities))
  (doseq [activity (map activity-to-row activities)]
    (jdbc/insert! db-spec
                  :activities
                  activity)))


(defn remove-activities-for-athlete [athlete-id]
  (log/info (str "removing activities for athlete " athlete-id))
  (jdbc/delete! db-spec :activities ["athlete_id = ?" athlete-id]))
  
  
           
           
           

