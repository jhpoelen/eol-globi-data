#!/bin/bash
if [ -n "$TRAVIS_TAG" ]; then
  mvn -s .travis.maven.settings.xml --debug -DskipTests clean deploy
fi
