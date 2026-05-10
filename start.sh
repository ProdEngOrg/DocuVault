#!/bin/bash
set -x

mkdir -p ./workspaces/jenkins_config
export JENKINS_CONFIG_ROOT=./workspaces/jenkins_config
docker compose --profile mongo --profile prod-eng-service up -d
