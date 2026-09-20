#!/bin/sh
set -e

cd "$(dirname "$0")"

if ! command -v javac >/dev/null 2>&1; then
  if [ -x /usr/libexec/java_home ]; then
    JAVA_HOME="$(/usr/libexec/java_home)"
    export JAVA_HOME
    PATH="$JAVA_HOME/bin:$PATH"
    export PATH
  fi
fi

mkdir -p out
find src -name "*.java" > sources.txt
javac -d out @sources.txt
java -cp out skillswap.Main
