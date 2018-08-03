$DO_TOKEN="your-digitalocean-token"
$MACHINE_NAME="my-machine"

$accessKey="AKIAIOSFODNN7EXAMPLE"
$secretKey="wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"

docker-machine create `
	--driver=digitalocean `
	--digitalocean-access-token=$DO_TOKEN `
	--digitalocean-size=1gb `
	--digitalocean-region=ams3 `
	--digitalocean-private-networking=true `
	--digitalocean-image=ubuntu-16-04-x64 `
    $MACHINE_NAME

$MACHINE_IP = ((docker-machine ip $MACHINE_NAME) | Out-String).trim()

& docker-machine env $MACHINE_NAME --shell powershell | Invoke-Expression

$SUB_NET="172.18.0.0/16"
$STUB_NET=mydummynet

docker network create `
	--subnet=$SUB_NET $STUB_NET

docker volume create s3data
docker volume create s3conf

docker run `
  -p 9000:9000 `
  --detach `
  --name minio `
  --env "MINIO_ACCESS_KEY=$accessKey" `
  --env "MINIO_SECRET_KEY=$secretKey" `
  --net $STUB_NET `
  -v s3data:/data `
  -v s3conf:/root/.minio `
  minio/minio server /data
  
# this command provides necessary jar for Minio
scp -o StrictHostKeyChecking=accept-new `
    -i "${env:HOMEPATH}/.docker/machine/machines/$MACHINE_NAME/id_rsa" `
    ../../minio-4.0.2-all.jar root@${MACHINE_IP}:~

scp -o StrictHostKeyChecking=accept-new `
    -i "${env:HOMEPATH}/.docker/machine/machines/$MACHINE_NAME/id_rsa" `
    ./script.jmx root@${MACHINE_IP}:~	
 
docker run `
  --detach `
  --name jmeter `
  --env "JMETER_USER_CLASSPATH=/root/" `
  --env "MINIO_ACCESS_KEY=$accessKey" `
  --env "MINIO_SECRET_KEY=$secretKey" `
  --volume /root/:/root/ `
  --net $STUB_NET `
  vmarrazzo/jmeter `
  -n -t /root/script.jmx
