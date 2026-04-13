#!/bin/bash
set -e

ANDROID_DIR="$(cd "$(dirname "$0")" && pwd)"
TEST_DIR="$ANDROID_DIR/app/src/test"
CLASSES_DIR="$ANDROID_DIR/build/test-classes"
JUNIT_DIR="$ANDROID_DIR/build/junit"
COVERAGE_DIR="$ANDROID_DIR/build/coverage"

mkdir -p "$CLASSES_DIR" "$JUNIT_DIR" "$COVERAGE_DIR"

if [ ! -f "$JUNIT_DIR/junit-4.13.2.jar" ]; then
    echo "Downloading JUnit..."
    mkdir -p "$JUNIT_DIR"
    curl -sL https://repo1.maven.org/maven2/junit/junit/4.13.2/junit-4.13.2.jar -o "$JUNIT_DIR/junit-4.13.2.jar"
    curl -sL https://repo1.maven.org/maven2/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar -o "$JUNIT_DIR/hamcrest-core-1.3.jar"
fi

JUNIT_CP="$JUNIT_DIR/junit-4.13.2.jar:$JUNIT_DIR/hamcrest-core-1.3.jar"

echo "Compiling test files..."
find "$TEST_DIR" -name "*.java" -type f > "$ANDROID_DIR/build/sources.txt"

javac -source 8 -target 8 -cp "$JUNIT_CP" -d "$CLASSES_DIR" @"$ANDROID_DIR/build/sources.txt"

echo "Running tests..."
java -cp "$CLASSES_DIR:$JUNIT_CP" org.junit.runner.JUnitCore com.nerv.clock.ClockLogicTest

echo "Tests completed."
