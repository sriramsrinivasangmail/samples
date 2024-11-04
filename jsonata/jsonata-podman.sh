#!/usr/bin/env bash


IMAGE="na.artifactory.swg-devops.com/hyc-roja-platform-engineering-team-docker-local/concert/ibm-roja-pipeline:v1.0.2.1-347-20241010.192530-v1.0.2.x"

## mount the source json and jsonata expression files
VOL_MNT="-v ${PWD}/src:/src"

## convenient script under ./src to invoke jsonata python cli
### this script takes the expression file and json source file as arguments
E_PT="--entrypoint=/src/transform.sh"

## run the transfrom inside the container, operating on the json files in the ./src directory
podman run --rm -it ${VOL_MNT} ${E_PT} ${IMAGE} /src/convert.jsonata /src/example-01.json
