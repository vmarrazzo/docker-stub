As described in section "Preparing the IBM MQ Driver jar" of article

https://www.blazemeter.com/blog/testing-docked-ibmmq-with-jmeter-learn-how

to obtain ibmmq required jar use command

docker cp \
    <container_id>:/opt/mqm/java/lib/com.ibm.mq.allclient.jar \
    com.ibm.mq.allclient-9.0.4.0.jar

where <container_id> is a running ibmmq container into your machine.

Fecthed jar must be placed into this folder.

Vincenzo