#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
mkdir -p .private/core-test
find android/app/src/main/java/community/leo/obd/core -name '*.java' > .private/core-test/sources
javac -encoding UTF-8 --release 17 -d .private/core-test @.private/core-test/sources tests/CoreChecks.java
java -cp .private/core-test CoreChecks
