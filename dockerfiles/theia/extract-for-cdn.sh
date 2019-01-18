#!/bin/sh -e
#
# Copyright (c) 2019 Red Hat, Inc.
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0

image=$1
destination=$2
container=$(docker create "$image")
if [ -f "$destination" ]; then
  rm -Rf "$destination"
fi
mkdir -p "$destination"
docker cp "$container:/home/theia/lib/cdn.json" "$destination" 2>/dev/null || true
if [ -f "$destination/cdn.json" ]; then
  for file in $(jq --raw-output '.[] | select((has("cdn")) and (has("external")|not)) | .chunk,.resource' "$destination/cdn.json" | grep -v 'null')
  do
    dir=$(dirname "$file")
    mkdir -p "$destination/$dir"
    docker cp "$container:/home/theia/lib/$file" "$destination/$file"
    if [[ "$file" == *.*.js ]]
    then
      docker cp "$container:/home/theia/lib/$file.map" "$destination/$file.map" || true
    fi
  done
fi
docker rm "$container" >/dev/null
