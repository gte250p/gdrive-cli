#!/bin/bash

rm -rf gdrive-cli-1.0/
./gradlew distZip
unzip ./build/distributions/gdrive-cli-1.0.zip
