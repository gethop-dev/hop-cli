#!/usr/bin/env bash
#shellcheck disable=SC2091

set -eu

AWS_ACCOUNT_NUMBER=$(aws sts get-caller-identity --query Account --output text |
    sed "s/$(printf '\r')\$//")

aws ecr get-login-password --region "${AWS_DEFAULT_REGION}" |
    sed "s/$(printf '\r')\$//" |
    docker login --username AWS --password-stdin "${AWS_ACCOUNT_NUMBER}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com"
