#!/bin/bash
set -e

if [[ $TRAVIS_PULL_REQUEST == 'false' && ! -z "$TRAVIS_TAG" && $TRAVIS_REPO_SLUG == 'bluesliverx/grails-external-config-reload' ]]; then
  echo "Publishing plugin grails-external-config-reload version $TRAVIS_TAG"

  # Publish plugin
  ./gradlew bintrayUpload
else
  ./gradlew check
fi
