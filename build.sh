#!/bin/sh

set -e

# Remove a previously created custom runtime
file="runtime.zip"
rm -f "$file"

# Build the custom Java runtime from the Dockerfile
docker build -f Dockerfile --progress=plain -t lambda-custom-runtime-clojure .

# Extract the runtime.zip from the Docker environment and store it locally
docker run --rm --entrypoint cat lambda-custom-runtime-clojure $file > $file

