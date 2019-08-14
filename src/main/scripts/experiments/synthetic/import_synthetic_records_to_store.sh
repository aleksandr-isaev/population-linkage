#!/bin/sh

if [[ -n "$1" ]];
then
    export MAVEN_OPTS="-Xmx"$1"G"
    echo Setting heap size: $1GB
fi


mvn exec:java -q -Dexec.cleanupDaemonThreads=false -Dexec.mainClass="uk.ac.standrews.cs.population_linkage.data.synthetic.ImportSyntheticScotlandRecordsToStore" -e -Dexec.args="$2 $3 $4"
