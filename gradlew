#!/usr/bin/env sh
APP_BASE_NAME=`basename "$0"`
DIRNAME=`dirname "$0"`
[ -z "$JAVA_HOME" ] && JAVA_HOME=`type -p java | xargs readlink -f | xargs dirname | xargs dirname`
if [ -z "$JAVA_HOME" ]; then
  echo "Error: JAVA_HOME is not set." >&2
  exit 1
fi
CLASSPATH=$DIRNAME/gradle/wrapper/gradle-wrapper.jar
exec "$JAVA_HOME/bin/java" -Xmx64m "-Dorg.gradle.appname=$APP_BASE_NAME" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
