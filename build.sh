#!/usr/bin/env bash
set -e

if [ "$1" = "clean" ]; then
    echo "Cleaning out directory..."
    rm -rf out
    echo "Clean complete."
    exit 0
fi

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
