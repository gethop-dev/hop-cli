#!/usr/bin/env bash
#shellcheck disable=SC2034

set -eu -o pipefail

SCRIPT_DIR=$(dirname "$(realpath "$0")")
SCRIPT_PARENT_DIR=$(dirname "${SCRIPT_DIR}")
# shellcheck disable=SC1090,SC1091
source "${SCRIPT_PARENT_DIR}/common-vars.sh"

# The AWS account-id is obtained from the credentials
AWS_ACCOUNT_NUMBER=$(aws sts get-caller-identity --query Account --output text |
                         sed "s/$(printf '\r')\$//")

# S3 Bucket where the bundle for the ElasticBeanstalk deployment is stored
EB_S3_BUCKET="elasticbeanstalk-${AWS_DEFAULT_REGION}-${AWS_ACCOUNT_NUMBER}"

# List of file for the Beanstalk Task definition bundle.
# It should contain all the directories and
# files that are used as volumes by the differente containers of the
# app (nginx, etc.). Directories are added recursively.
SOURCE_BUNDLE_FILES=({{#project.deploy-files}}{{&.}}{{/project.deploy-files}})

# Name for the zip file where we bundle the files specified in
# SOURCE_BUNDLE_FILES. IMPORTANT: the name must be a valid S3 key, as
# we are storing the file in S3 using this name as the key
KEY="${TAG}.zip"

# Name of the Beanstalk application for the builds
APPLICATION_NAME="{{project.profiles.ci.continuous-deployment.aws.eb-application-name}}"

# Name of the Beanstalk environment where we deploy the builds to
TEST_ENV_NAME="{{project.profiles.ci.continuous-deployment.aws.environment.test.eb-env-name}}"

PROD_ENV_NAME="{{project.profiles.ci.continuous-deployment.aws.environment.prod.eb-env-name}}"
