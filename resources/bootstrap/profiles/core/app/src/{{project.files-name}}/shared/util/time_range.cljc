;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

{{=<< >>=}}
(ns <<project.name>>.shared.util.time-range
  (:require [cljc.java-time.day-of-week :as jt.day-of-week]
            [cljc.java-time.duration :as jt.duration]
            [cljc.java-time.instant :as jt.instant]
            [cljc.java-time.local-date :as jt.local-date]
            [cljc.java-time.local-date-time :as jt.local-date-time]
            [cljc.java-time.local-time :as jt.local-time]
            [cljc.java-time.temporal.temporal-adjusters :as jt.temporal-adjusters]
            [cljc.java-time.zone-id :as jt.zone-id]
            [cljc.java-time.zoned-date-time :as jt.zoned-date-time]))

(defn- local-date-time->instant
  [local-date-time zone-id]
  (-> local-date-time
      (jt.local-date-time/at-zone zone-id)
      (jt.zoned-date-time/to-instant)))

(defmulti calculate-relative-local-date-time
  (fn [range _opts]
    (second range)))

(defmethod calculate-relative-local-date-time :minutes
  [[amount _ when] {:keys [zone-id]}]
  (let [time (-> (jt.local-date-time/now zone-id)
                 (jt.local-date-time/plus-minutes amount))]
    (if (= :start when)
      (-> time
          (jt.local-date-time/with-second 0)
          (jt.local-date-time/with-nano 0))
      (-> time
          (jt.local-date-time/with-second 59)
          (jt.local-date-time/with-nano 0)))))

(defmethod calculate-relative-local-date-time :hours
  [[amount _ when] {:keys [zone-id] :or {zone-id (jt.zone-id/system-default)}}]
  (let [time (-> (jt.local-date-time/now zone-id)
                 (jt.local-date-time/plus-hours amount))]
    (if (= :start when)
      (-> time
          (jt.local-date-time/with-minute 0)
          (jt.local-date-time/with-second 0)
          (jt.local-date-time/with-nano 0))
      (-> time
          (jt.local-date-time/with-minute 59)
          (jt.local-date-time/with-second 59)
          (jt.local-date-time/with-nano 0)))))

(defmethod calculate-relative-local-date-time :days
  [[amount _ when] {:keys [zone-id] :or {zone-id (jt.zone-id/system-default)}}]
  (let [day (-> (jt.local-date/now zone-id)
                (jt.local-date/plus-days amount))]
    (if (= :start when)
      (jt.local-date/at-start-of-day day)
      (jt.local-date/at-time day jt.local-time/max))))

(defmethod calculate-relative-local-date-time :weeks
  [[amount _ when] {:keys [zone-id first-week-day last-week-day]
                    :or {first-week-day jt.day-of-week/monday
                         last-week-day jt.day-of-week/sunday}}]
  (let [day (-> (jt.local-date/now zone-id)
                (jt.local-date/plus-days (* 7 amount)))]
    (if (= :start when)
      (-> day
          (jt.local-date/with (jt.temporal-adjusters/previous-or-same first-week-day))
          (jt.local-date/at-time jt.local-time/min))
      (-> day
          (jt.local-date/with (jt.temporal-adjusters/next-or-same last-week-day))
          (jt.local-date/at-time jt.local-time/max)))))

(defmethod calculate-relative-local-date-time :months
  [[amount _ when] {:keys [zone-id]}]
  (let [day (-> (jt.local-date/now zone-id)
                (jt.local-date/plus-months amount))]
    (if (= :start when)
      (-> day
          (jt.local-date/with (jt.temporal-adjusters/first-day-of-month))
          (jt.local-date/at-time jt.local-time/min))
      (-> day
          (jt.local-date/with (jt.temporal-adjusters/last-day-of-month))
          (jt.local-date/at-time jt.local-time/max)))))

(defmethod calculate-relative-local-date-time :years
  [[amount _ when] {:keys [zone-id]}]
  (let [day (-> (jt.local-date/now zone-id)
                (jt.local-date/plus-years amount))]
    (if (= :start when)
      (-> day
          (jt.local-date/with (jt.temporal-adjusters/first-day-of-year))
          (jt.local-date/at-time jt.local-time/min))
      (-> day
          (jt.local-date/with (jt.temporal-adjusters/last-day-of-year))
          (jt.local-date/at-time jt.local-time/max)))))

(defn- calculate-time-range-extreme
  [definition {:keys [zone-id] :as opts}]
  (if (= :now definition)
    (jt.instant/now)
    (-> (calculate-relative-local-date-time definition opts)
        (local-date-time->instant zone-id))))

(defn calculate-time-range
  [{:keys [from to]} opts]
  {:from (calculate-time-range-extreme from opts)
   :to (calculate-time-range-extreme to opts)})

(defn calculate-time-range-interval-in-seconds
  [{:keys [from to] :as _time-range}]
  (-> (jt.duration/between from to)
      (jt.duration/get-seconds)))
