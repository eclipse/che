# Copyright (c) 2019 Red Hat, Inc.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html

FROM library/centos:centos7

ENV LANG=en_US.utf8 \
    DISPLAY=:99 \
    FABRIC8_USER_NAME=fabric8

ARG CHROME_VERSION=''
RUN echo ${CHROME_VERSION}

ADD https://dl.google.com/linux/direct/google-chrome-stable_current_x86_64.rpm /usr/bin

RUN yum localinstall --assumeyes  /usr/bin/google-chrome-stable_current_x86_64.rpm && \
    yum install --assumeyes epel-release && \
    yum update --assumeyes && \
    yum install --assumeyes \
        xorg-x11-server-Xvfb \
        git \
        unzip \
        centos-release-scl && \
    yum install --assumeyes nodejs && \
    npm install -g typescript && \
    yum install jq --assumeyes && \
    yum install x11vnc --assumeyes && \
    yum clean all --assumeyes && \
    rm -rf /var/cache/yum && \
    yum install nmap --assumeyes && \
    # Get compatible versions of chrome and chromedriver
    chrome_version=${CHROME_VERSION} && \
    chromedriver_version=${CHROME_VERSION} && \
    $(curl -sS -g https://chromedriver.storage.googleapis.com/${chromedriver_version}/chromedriver_linux64.zip > chromedriver_linux64.zip) && \
    unzip chromedriver_linux64.zip && mv chromedriver /usr/bin/chromedriver && chmod +x /usr/bin/chromedriver && rm chromedriver_linux64.zip
    
COPY e2e/package.json e2e/package-lock.json /root/e2e/
RUN cd /root/e2e && \
    npm --silent i
    
COPY e2e /root/e2e
COPY docker-entrypoint.sh /root/

WORKDIR /root/

EXPOSE 9515

ENTRYPOINT ["/root/docker-entrypoint.sh"]
