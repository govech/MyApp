#!/usr/bin/env sh

##############################################################################
## Gradle start up script for UN*X
##############################################################################

# Set default Gradle JVM options
default_jvm_opts="-Xmx64m -Xms64m"

# Locate the wrapper jar
WRAPPER_JAR="$(dirname "$0")/gradle/wrapper/gradle-wrapper.jar"

if [ ! -f "$WRAPPER_JAR" ]; then
  echo "Could not find Gradle Wrapper jar. Please run 'gradle wrapper' to generate it."
  exit 1
fi

# Execute the wrapper jar
exec java $default_jvm_opts -jar "$WRAPPER_JAR" "$@"