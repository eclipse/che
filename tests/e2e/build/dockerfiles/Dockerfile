FROM selenium/standalone-chrome:117.0

ENV DISPLAY=':20'

USER root

RUN apt-get update && apt-get install && \
    apt-get install -y ftp x11vnc ffmpeg libvpx6 && \
    curl -fsSLo- https://raw.githubusercontent.com/nvm-sh/nvm/master/install.sh | bash && \
    export NVM_DIR="$HOME/.nvm" && \
    [ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh" && \
    [ -s "$NVM_DIR/bash_completion" ] && \. "$NVM_DIR/bash_completion" && \
    nvm install stable && nvm alias default stable && nvm use default && nvm list && \
    node -v

COPY --chown=0:0 build/dockerfiles/entrypoint.sh /tmp/

RUN mkdir /tmp/e2e && \
    chmod -R 777 /tmp/e2e

RUN sed -i "s/nodaemon=true/nodaemon=false/" /etc/supervisord.conf

COPY package.json package-lock.json /tmp/e2e/

RUN cd /tmp/e2e && \
    export NVM_DIR="$HOME/.nvm" && \
    [ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh" && \
    [ -s "$NVM_DIR/bash_completion" ] && \. "$NVM_DIR/bash_completion" && \
    nvm use default && \
    npm ci

COPY . /tmp/e2e

WORKDIR /tmp/e2e

EXPOSE 5920

RUN chgrp -R 0 /tmp && \
    chmod -R g+rwX /tmp

ENTRYPOINT [ "/tmp/entrypoint.sh" ]
