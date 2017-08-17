#!/bin/bash

# $1 is .zip FileName
# $2 is Service name
echo "Output from Deployment Script"
echo "=============================="
echo "File name : "$1
echo "Service name : "$2
if [ ! -d "/var/serverless/Deployment" ]; then
	mkdir /var/serverless/Deployment
fi
cd /var/serverless/Deployment 	# Deployment directory containing Deploy.sh and the uploaded file
echo "Directory changed to Deployment"
chmod -R 777 $1
echo "Permissions given to file : "$1
unzip $1 -d TempFolder > /tmp/log 2>&1
if [ $? -ne 0 ]; then
	echo "Wrong Upload Format/Wrong Code"
	rm -f $1
	echo "Uploaded file $1 deleted"
	echo FAIL
	exit
fi
echo "File Unzipped to TempFolder : "$1
chmod -R 777 TempFolder
cp /var/serverless/{coreServiceFramework.class,communicateWithMQ.class,DoLogging.class,functionParam.class} TempFolder/src
echo "/var/serverless/{coreServiceFramework.class,communicateWithMQ.class,DoLogging.class,functionParam.class} copied to TempFolder/src for Compilation"
cd TempFolder/src
javac -cp "../lib/*:" *.java #compile
# java -cp "../lib/*:" Dog  #test to execute
if [ $? -eq 0 ]; then
	echo "Service $2 Compiled Succesfully"
	cd ../..
	mkdir -p /var/serverless/Services/$2
	echo "Directory created for service $2"
    cp -avr TempFolder/. /var/serverless/Services/$2 > /dev/null
    echo "Compiled classes and Application files copied from TempFolder/. /var/serverless/Services/$2"
    rm -rf TempFolder
    echo "TempFolder deleted"
    rm -f $1
    echo "Uploaded file $1 deleted"
    echo OK
else
    cd ../..
    echo "Wrong Upload Format/Wrong Code"
    rm -rf TempFolder
    echo "TempFolder deleted"
    rm -f $1
    echo "Uploaded file $1 deleted"
    echo FAIL
fi
