# Copyright (c) 2012-2016 Codenvy, S.A.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#
# Contributors:
#   Codenvy, S.A. - initial API and implementation

# Set to "<proto>://<user>:<pass>@<host>:<port>"
$http_proxy  = ""
$https_proxy = ""
$che_version = "nightly"
$ip          = "192.168.28.111"

Vagrant.configure(2) do |config|
  config.vm.box = "boxcutter/centos72-docker"
  config.vm.box_download_insecure = true
  config.ssh.insert_key = false
  config.vm.network :private_network, ip: $ip
  config.vm.synced_folder ".", "/home/vagrant/.che"
  config.vm.define "che" do |che|
  end

  config.vm.provider "virtualbox" do |vb|
    vb.memory = "4096"
    vb.name = "eclipse-che-vm"
  end

  $script = <<-SHELL
    HTTP_PROXY=$1
    HTTPS_PROXY=$2
    CHE_VERSION=$3

    if [ -n "$HTTP_PROXY" ] || [ -n "$HTTPS_PROXY" ]; then
	    echo "-------------------------------------"
	    echo "."
	    echo "ECLIPSE CHE: CONFIGURING SYSTEM PROXY"
	    echo "."
	    echo "-------------------------------------"
	    echo 'export HTTP_PROXY="'$HTTP_PROXY'"' >> /home/vagrant/.bashrc
	    echo 'export HTTPS_PROXY="'$HTTPS_PROXY'"' >> /home/vagrant/.bashrc
	    source /home/vagrant/.bashrc
	    echo "HTTP PROXY set to: $HTTP_PROXY"
	    echo "HTTPS PROXY set to: $HTTPS_PROXY"
    fi

    # Add the user in the VM to the docker group
    echo "------------------------------------"
    echo "ECLIPSE CHE: UPGRADING DOCKER ENGINE"
    echo "------------------------------------"
    echo 'y' | sudo yum update docker-engine &>/dev/null &
    PROC_ID=$!
    while kill -0 "$PROC_ID" >/dev/null 2>&1; do
      printf "#"
      sleep 5
    done

 
    # Add the 'vagrant' user to the 'docker' group
    usermod -aG docker vagrant &>/dev/null

    # We need write access to this file to enable Che container to create other containers
    sudo chmod 777 /var/run/docker.sock &>/dev/null

    # Configure Docker daemon with the proxy
    if [ -n "$HTTP_PROXY" ] || [ -n "$HTTPS_PROXY" ]; then
        mkdir /etc/systemd/system/docker.service.d
    fi
    if [ -n "$HTTP_PROXY" ]; then
        printf "[Service]\nEnvironment=\"HTTP_PROXY=${HTTP_PROXY}\"" > /etc/systemd/system/docker.service.d/http-proxy.conf
    fi
    if [ -n "$HTTPS_PROXY" ]; then
        printf "[Service]\nEnvironment=\"HTTPS_PROXY=${HTTPS_PROXY}\"" > /etc/systemd/system/docker.service.d/https-proxy.conf
    fi
    if [ -n "$HTTP_PROXY" ] || [ -n "$HTTPS_PROXY" ]; then
        printf "[Service]\nEnvironment=\"NO_PROXY=localhost,127.0.0.1\"" > /etc/systemd/system/docker.service.d/no-proxy.conf
        systemctl daemon-reload
        systemctl restart docker
    fi

    echo "-------------------------------------------------"
    echo "ECLIPSE CHE: DOWNLOADING ECLIPSE CHE DOCKER IMAGE"
    echo "-------------------------------------------------"
    docker pull codenvy/che:${CHE_VERSION} &>/dev/null &
    PROC_ID=$!
 
    while kill -0 "$PROC_ID" >/dev/null 2>&1; do
      printf "#"
      sleep 5
    done
  SHELL

  config.vm.provision "shell" do |s| 
  	s.inline = $script
  	s.args = [$http_proxy, $https_proxy, $che_version]
  end

  $script2 = <<-SHELL
    CHE_VERSION=$1
    IP=$2

    echo "---------------------------------------"
    echo "ECLIPSE CHE: BOOTING ECLIPSE CHE SERVER"
    echo "---------------------------------------"

    docker run --net=host --name=che --restart=always --detach `
              `-v /var/run/docker.sock:/var/run/docker.sock `
              `-v /home/user/che/lib:/home/user/che/lib-copy `
              `-v /home/user/che/workspaces:/home/user/che/workspaces `
              `-v /home/user/che/storage:/home/user/che/tomcat/temp/local-storage `
              `codenvy/che:${CHE_VERSION} --remote:${IP} run &>/dev/null
            
    while [ true ]; do
      printf "#"
      curl -v http://${IP}:8080/dashboard &>/dev/null
      exitcode=$?
      if [ $exitcode == "0" ]; then
        echo "----------------------------------------"
        echo "ECLIPSE CHE: SERVER BOOTED AND REACHABLE"
        echo "AVAILABLE: http://${IP}:8080  "
        echo "----------------------------------------"
        exit 0
      fi 
      sleep 5

    done

  SHELL

  config.vm.provision "shell", run: "always" do |s|
    s.inline = $script2
    s.args = [$che_version, $ip]
  end

end
