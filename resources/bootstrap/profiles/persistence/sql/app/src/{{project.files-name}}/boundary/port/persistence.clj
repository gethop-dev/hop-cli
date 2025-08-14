;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

{{=<< >>=}}
(ns <<project.name>>.boundary.port.persistence)

(defprotocol Health
  (get-status [spec opts]))

(defprotocol Transactionable
  (with-transaction
    [db-spec config body-fn body-fn-argv]
    [db-spec config body-fn body-fn-argv rollback-check-fn]))
