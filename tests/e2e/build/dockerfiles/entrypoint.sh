#!/usr/bin/env bash

EXIT_CODE=0
export NVM_DIR="$HOME/.nvm"
[ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh"
[ -s "$NVM_DIR/bash_completion" ] && \. "$NVM_DIR/bash_completion"
nvm use default

kill_ffmpeg(){
  echo "Killing ffmpeg with PID=$ffmpeg_pid"
  kill -2 "$ffmpeg_pid"
  wait "$ffmpeg_pid"
  mkdir -p /tmp/e2e/report/
  cp /tmp/ffmpeg_report/* /tmp/e2e/report/
}

set -x
# Validate selenium base URL
if [ -z "$TS_SELENIUM_BASE_URL" ]; then
    echo "The \"TS_SELENIUM_BASE_URL\" is not set!";
    echo "Please, set the \"TS_SELENIUM_BASE_URL\" environment variable."
    exit 1
fi

# Set testing suite
if [ -z "$TEST_SUITE" ]; then
    TEST_SUITE=test
fi

# Launch display mode and VNC server
export DISPLAY=':20'
Xvfb :20 -screen 0 1920x1080x24 > /dev/null 2>&1 &
x11vnc -display :20 -N -forever > /dev/null 2>&1 &
echo ''
echo '#######################'
echo ''
echo 'For remote debug connect to the VNC server 0.0.0.0:5920'
echo ''
echo '#######################'
echo ''

# Launch selenium server
/usr/bin/supervisord --configuration /etc/supervisord.conf & \
export TS_SELENIUM_REMOTE_DRIVER_URL=http://localhost:4444/wd/hub

# Check selenium server launching
expectedStatus=200
currentTry=1
maximumAttempts=5

while [ $(curl -s -o /dev/null -w "%{http_code}" --fail http://localhost:4444/wd/hub/status) != $expectedStatus ];
do
  if (( currentTry > maximumAttempts ));
  then
    status=$(curl -s -o /dev/null -w "%{http_code}" --fail http://localhost:4444/wd/hub/status)
    echo "Exceeded the maximum number of checking attempts,"
    echo "selenium server status is '$status' and it is different from '$expectedStatus'";
    exit 1;
  fi;

  echo "Wait selenium server availability ..."

  curentTry=$((curentTry + 1))
  sleep 1
done

# Print information about launching tests
if mount | grep 'e2e' && ! mount | grep 'e2e/report'; then
	echo "The local code is mounted. Executing local code."
	cd /tmp/e2e || exit
	npm ci
else
	echo "Executing e2e tests from an image."
	cd /tmp/e2e || exit
fi

# Launch tests
if [ $TS_LOAD_TESTS ]; then
  timestamp=$(date +%s)
  user_folder="$TS_SELENIUM_OCP_USERNAME-$timestamp"
  export TS_SELENIUM_REPORT_FOLDER="./$user_folder/report"
  export TS_SELENIUM_LOAD_TEST_REPORT_FOLDER="./$user_folder/load-test-folder"
  CONSOLE_LOGS="./$user_folder/console-log.txt"
  mkdir $user_folder
  touch $CONSOLE_LOGS

  npm run $TEST_SUITE 2>&1 | tee $CONSOLE_LOGS

  echo "Tarring files and sending them via FTP..."
  tar -cf $user_folder.tar ./$user_folder

  ftp -vn load-tests-ftp-service << End_script
  user user pass1234
  binary
  put $user_folder.tar
  quit
End_script

  echo "Files sent to load-tests-ftp-service."
else
  SCREEN_RECORDING=${VIDEO_RECORDING:-true}
  if [ "${SCREEN_RECORDING}" == "true" ]; then
    echo "Starting ffmpeg recording..."
    mkdir -p /tmp/ffmpeg_report
    nohup ffmpeg -y -video_size 1920x1080 -framerate 30 -f x11grab -i :20.0 -c:a libvpx /tmp/ffmpeg_report/output.webm 2> /tmp/ffmpeg_report/ffmpeg_err.txt > /tmp/ffmpeg_report/ffmpeg_std.txt &
    ffmpeg_pid=$!
    trap kill_ffmpeg 2 15
  fi

  echo "Running TEST_SUITE: $TEST_SUITE with user: $TS_SELENIUM_OCP_USERNAME"
  npm run $TEST_SUITE
  EXIT_CODE=$?
  if [ "${SCREEN_RECORDING}" == "true" ]; then
    kill_ffmpeg
  fi
  exit $EXIT_CODE
fi
