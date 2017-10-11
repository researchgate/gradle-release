#!/usr/bin/env bash

set -ev

# Workaround to using openjdk7 with Gradle due to security issue:
# https://github.com/gradle/gradle/issues/2421
if [ $TRAVIS_JDK_VERSION == "openjdk7" ]; then
    readonly BCPROV_FILENAME=bcprov-ext-jdk15on-158.jar
    wget "https://bouncycastle.org/download/${BCPROV_FILENAME}"
    sudo mv $BCPROV_FILENAME /usr/lib/jvm/java-7-openjdk-amd64/jre/lib/ext
    sudo perl -pi.bak -e 's/^(security\.provider\.)([0-9]+)/$1.($2+1)/ge' /etc/java-7-openjdk/security/java.security
    echo "security.provider.1=org.bouncycastle.jce.provider.BouncyCastleProvider" | sudo tee -a /etc/java-7-openjdk/security/java.security
fi
