#!/bin/sh
# Compiles the project and starts the application. Run it from this folder.
set -e
mkdir -p out
javac -encoding UTF-8 -d out $(find src -name '*.java')
java -cp out ems.Main
