# Overview
This script tests the performance of an OpenShift environment by running multiple workspaces simultaneously. It evaluates the system under test by checking the average results across all pods and identifying any failures that occur during the testing process.

## Prerequisites
To run these tests, you will need:
- The `kubectl` client installed
- An OpenShift cluster with running OpenShift DevSpaces
- A test user logged into the DevSpaces Dashboard (this ensures that user namespaces are created)

## Running load tests
Follow these steps to run the load tests:
1. Log in to the OpenShift cluster with OpenShift DevSpaces or Eclipse Che deployed from the terminal.
2. Start the `load-test.sh` script from `test/e2e/performance/load-tests`. Set the number of workspaces to start using the `-c` parameter (e.g., `./load-test.sh -c 5`). Set the timeout for waiting for workspaces to start using the `-t` parameter in seconds (e.g., `./load-test.sh -t 240`).
3. This script uses the local `example.yaml` file to start the workspaces.
4. Alternatively, you can provide a link to the test devworkspace YAML file using the `-l` argument (e.g., `./load-test.sh -l https://gist.githubusercontent.com/SkorikSergey/1856af20514ecce6c0dbb71f44fc0bcb/raw/3f6a38f0f6adf017dcecf6486ffe507ebe6cfc31/load-test-devworkspace.yaml`).
5. If you want to start workspaces in separate namespaces (one workspace per namespace), use the `--start-separately` argument (e.g., `./load-test.sh --start-separately`).
6. The script will provide the average time for workspace starting and the number of failed workspaces.

## Results and logs
If a workspace fails to start, the logs will be saved in the `logs` directory.
