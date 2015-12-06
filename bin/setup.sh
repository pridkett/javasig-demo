#!/bin/bash

# Copyright 2015 IBM Corp.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# limitations under the License.

# Helper script to setup all the services necessary for the NY Java Sig
# Watson Demo Application.
#
# Patrick Wagstrom <pwagstro@us.ibm.com>

OUTPUT_FILE="configuration.properties"

if [ ! -z "$1" ]; then
    OUTPUT_FILE=$1
fi

function outputCredentials() {
    PROPERTY_KEY=$1
    PROPERTY_VALUE=$2

    sed '/$PROPERTY_KEY/d' $OUTPUT_FILE > /dev/null 2>&1
    echo "$PROPERTY_KEY=$PROPERTY_VALUE" >> $OUTPUT_FILE
}

function createService() {
    SERVICE=$1
    PLAN=$2
    SERVICE_NAME=$3
    SERVICE_PREFIX=$4

    # Check to see if the service already exists in this space
    cf service $SERVICE_NAME --guid > /dev/null 2>&1

    if [ $? == 1 ]; then
        cf create-service $SERVICE $PLAN $SERVICE_NAME
        cf create-service-key $SERVICE_NAME Credentials-1
    else
        echo "Service $SERVICE_NAME already exists - using existing service"
    fi

    # Check for service credentials
    CREDS=$(cf curl /v2/service_instances/$(cf service $SERVICE_NAME --guid)/service_keys)
    HAS_PW=$(echo $CREDS | grep password)

    # If there are no service credential, then create a set and fetch them
    if [ -z "$HAS_PW" ]; then
        cf create-service-key $SERVICE_NAME Credentials-1
        CREDS=$(cf curl /v2/service_instances/$(cf service $SERVICE_NAME --guid)/service_keys)
    fi
        
    SERVICE_USERNAME=$(echo $CREDS | sed -e 's/.*"username": "\([^"]*\)".*/\1/')
    SERVICE_PASSWORD=$(echo $CREDS | sed -e 's/.*"password": "\([^"]*\)".*/\1/')

    outputCredentials $SERVICE_PREFIX.username $SERVICE_USERNAME
    outputCredentials $SERVICE_PREFIX.password $SERVICE_PASSWORD
}

# Speech to Text
createService speech_to_text standard demo-stt com.ibm.watson.watsondemo.stt

# Text to Speech
createService text_to_speech standard demo-tts com.ibm.watson.watsondemo.tts

# Language Translation
createService language_translation standard demo-lt com.ibm.watson.watsondemo.lt

# Natural Language Classifier
createService natural_language_classifier standard demo-nlc com.ibm.watson.watsondemo.nlclassifier
