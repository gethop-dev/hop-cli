(ns hop-cli.bootstrap.profile.core)

(defn profile
  [_settings]
  {:files [{:files [{:src "resources/bootstrap/project/core"
                     :dst "new-project"}]
            :copy-if (constantly true)}]})
