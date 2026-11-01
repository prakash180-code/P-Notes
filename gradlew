#!/bin/sh
#
# Gradle wrapper script for P-Notes
# Uses pre-downloaded Gradle distribution in gradlew-temp/
#

set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
GRADLE_DIST_DIR="$PROJECT_DIR/gradlew-temp"

# Use the pre-downloaded distribution
if [ -d "$GRADLE_DIST_DIR/gradle-8.14.3" ]; then
    GRADLE_HOME="$GRADLE_DIST_DIR/gradle-8.14.3"
else
    echo "No Gradle distribution found in $GRADLE_DIST_DIR"
    exit 1
fi

exec "$GRADLE_HOME/bin/gradle" "$@"