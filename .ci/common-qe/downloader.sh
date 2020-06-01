#!/bin/bash

# Copyright (c) 2012-2020 Red Hat, Inc.
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# Contributors:
#   Red Hat, Inc. - initial API and implementation

COMMON_FOLDER_URL="https://raw.githubusercontent.com/eclipse/che/iokhrime-common-centos/.ci/common-qe"

COMMON_FOLDER_NAME="common-qe"
CERT_GENERATION_SCRIPT_NAME="che-cert-generation.sh"
CHE_UTIL_SCRIPT_NAME="che-util.sh"
COMMON_UTIL_SCRIPT_NAME="common-util.sh"
INSTALLATION_UTIL_SCRIPT_NAME="installation-util.sh"

function downloadAndSetPermissions(){
    local filename="$1"
    local fileUrl="$COMMON_FOLDER_URL/$filename"

    curl "$fileUrl" -o "$COMMON_FOLDER_NAME/$filename"

    chmod u+x "$COMMON_FOLDER_NAME/$filename"
}


set -e

rm -rf "$COMMON_FOLDER_NAME"
mkdir "$COMMON_FOLDER_NAME"

downloadAndSetPermissions $CERT_GENERATION_SCRIPT_NAME
downloadAndSetPermissions $CHE_UTIL_SCRIPT_NAME
downloadAndSetPermissions $COMMON_UTIL_SCRIPT_NAME
downloadAndSetPermissions $INSTALLATION_UTIL_SCRIPT_NAME