$DO_TOKEN="your-digitalocean-token"
$MACHINE_NAME="my-machine"
$QM_VOLUME_NAME="qm1data"

$ks_file="my-cert.pfx"
$ks_pass="changeit"

docker-machine create `
	--driver=digitalocean `
	--digitalocean-access-token=$DO_TOKEN `
	--digitalocean-size=1gb `
	--digitalocean-region=ams3 `
	--digitalocean-private-networking=true `
	--digitalocean-image=ubuntu-16-04-x64 `
    $MACHINE_NAME

$MACHINE_IP = ((docker-machine ip $MACHINE_NAME) | Out-String).trim()

scp -o StrictHostKeyChecking=accept-new `
    -i "${env:HOMEPATH}/.docker/machine/machines/$MACHINE_NAME/id_rsa" `
    ./my-cert.pfx root@${MACHINE_IP}:~

scp -o StrictHostKeyChecking=accept-new `
    -i "${env:HOMEPATH}/.docker/machine/machines/$MACHINE_NAME/id_rsa" `
    ./my-cert.jks root@${MACHINE_IP}:~

scp -o StrictHostKeyChecking=accept-new `
    -i "${env:HOMEPATH}/.docker/machine/machines/$MACHINE_NAME/id_rsa" `
    -r ./java-stub root@${MACHINE_IP}:~

& docker-machine env $MACHINE_NAME --shell powershell | Invoke-Expression

$SUB_NET="172.18.0.0/16"
$STUB_NET="mydummynet"

docker network create `
	--subnet=$SUB_NET $STUB_NET

docker volume create $QM_VOLUME_NAME

docker run `
  --env MQ_TLS_KEYSTORE=/var/ks/$ks_file `
  --env MQ_TLS_PASSPHRASE=$ks_pass `
  --publish 1414:1414 `
  --publish 1415:1415 `
  --publish 9443:9443 `
  --name wmq `
  --detach `
  --net $STUB_NET `
  --volume ${QM_VOLUME_NAME}:/mnt/mqm `
  --volume /root/:/var/ks `
  vmarrazzo/wmq

# this command provides jar for IBM MQ
scp -o StrictHostKeyChecking=accept-new `
    -i "${env:HOMEPATH}/.docker/machine/machines/$MACHINE_NAME/id_rsa" `
    ../../com.ibm.mq.allclient-9.0.4.0.jar root@${MACHINE_IP}:~/java-stub/localrepository/com/ibm/com.ibm.mq.allclient/9.0.4.0

docker volume create --name maven-repo

docker run `
  -it --rm `
  --net $STUB_NET `
  -v /root/:/opt/maven `
  -v maven-repo:/root/.m2 `
  -w /opt/maven `
  maven:3.5-jdk-10 `
  mvn clean compile exec:java -f /opt/maven/java-stub/pom.xml -DIBMMQ_HOST=wmq

