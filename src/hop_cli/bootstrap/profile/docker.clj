(ns hop-cli.bootstrap.profile.docker)

(defn profile
  [_settings]
  {:files [{:files [{:src "resources/bootstrap/project/docker"
                     :dst "new-project"}]}]})
