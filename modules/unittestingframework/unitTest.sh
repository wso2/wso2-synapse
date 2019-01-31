#!/bin/bash 
JAVA_HOME="/opt/java/jdk1.8.0_144"
CLASSPATH="/home/SynapseUnitTest/target/Synapse-1.0-SNAPSHOT-jar-with-dependencies.jar"
echo 'descriptorFilePath = ' $1
echo 'synapseHost = ' $2
echo 'port = ' $3
java -jar Synapse-1.0-SNAPSHOT-jar-with-dependencies.jar $1 $2 $3

