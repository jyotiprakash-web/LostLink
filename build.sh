#!/usr/bin/env bash
set -e

mkdir -p out

echo "Compiling Java sources..."
javac -Xlint:all -d out $(find src test -name "*.java")

echo "Compilation succeeded."
if [ "$1" = "test" ]; then
    echo "Running TestRunner..."
    java -cp out TestRunner
elif [ "$1" = "run" ]; then
    shift
    echo "Running Main..."
    java -cp out Main "$@"
fi
