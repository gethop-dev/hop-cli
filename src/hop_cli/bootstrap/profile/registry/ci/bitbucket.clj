(ns hop-cli.bootstrap.profile.registry.ci.bitbucket)

(defn profile
  [_settings]
  {:files [{:src "ci/bitbucket"}]})
