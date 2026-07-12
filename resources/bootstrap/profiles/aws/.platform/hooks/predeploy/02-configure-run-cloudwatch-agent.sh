#!/usr/bin/env bash

set -eu -o pipefail

CWAGENT_CONFIG_FILE='/opt/aws/amazon-cloudwatch-agent/etc/biotz-config.json'

# Ensure cwagent file exists and has the right_properties
if [[ ! -f "${CWAGENT_CONFIG_FILE}" ]]; then
    cat /dev/null >"${CWAGENT_CONFIG_FILE}"
fi
chmod 600 "${CWAGENT_CONFIG_FILE}"
chown root:root "${CWAGENT_CONFIG_FILE}"

PROJECT_NAME="$(/opt/elasticbeanstalk/bin/get-config environment -k PROJECT_NAME)"
PROJECT_ENV="$(/opt/elasticbeanstalk/bin/get-config environment -k ENVIRONMENT)"

cat >"${CWAGENT_CONFIG_FILE}" <<EOF
{
  "agent": {
    "metrics_collection_interval": 60,
    "run_as_user": "root"
  },
  "metrics": {
    "metrics_collected": {
      "cpu": {
        "totalcpu": true,
        "measurement": [
          "cpu_usage_active",
          "cpu_usage_idle",
          "cpu_usage_iowait"
        ],
        "append_dimensions": {
          "ElasticBeanstalkEnvironment": "${PROJECT_NAME}-${PROJECT_ENV}"
        }
      },
      "mem": {
        "measurement": [
          "mem_total",
          "mem_used",
          "mem_used_percent",
          "mem_available",
          "mem_available_percent"
        ],
        "append_dimensions": {
          "ElasticBeanstalkEnvironment": "${PROJECT_NAME}-${PROJECT_ENV}"
        }
      },
      "disk": {
        "measurement": [
          "disk_total",
          "disk_used",
          "disk_used_percent"
        ],
        "resources": [
          "/"
        ],
        "append_dimensions": {
          "ElasticBeanstalkEnvironment": "${PROJECT_NAME}-${PROJECT_ENV}"
        }
      }
    }
  }
}
EOF

# Apply the new configuration and restart the agent.
/opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl \
    -a fetch-config \
    -m ec2 \
    -s \
    -c "file:${CWAGENT_CONFIG_FILE}"
