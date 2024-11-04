#!/usr/bin/env bash


IMAGE="na.artifactory.swg-devops.com/hyc-roja-platform-engineering-team-docker-local/concert/ibm-roja-pipeline:v1.0.2.1-347-20241010.192530-v1.0.2.x"

## mount the source json and jsonata expression files
VOL_MNT="-v ${PWD}/src:/src"

## convenient script under ./src to invoke jsonata python cli
### this script takes the expression file and json source file as arguments
E_PT="--entrypoint=/src/transform.sh"

## run the transform script inside the container as the current user (root is not required)
## operating on the json files in the ./src directory passed as an arg to the entrypoint script

## tip: export PODMAN_ARCH="--arch amd64" to run on an Apple Silicon based Mac.

podman run ${PODMAN_ARCH} --user $(id -u) --rm -it ${VOL_MNT} ${E_PT} ${IMAGE} /src/convert.jsonata /src/example-01.json
