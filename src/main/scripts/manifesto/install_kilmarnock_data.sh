#!/bin/sh
mvn -q install:install-file -Dfile=/home/secure/maven_repo/uk/ac/standrews/cs/data-kilmarnock/1.0/data-kilmarnock-1.0-jar-with-dependencies.jar -DgroupId=uk.ac.standrews.cs -DartifactId=data-kilmarnock -Dversion=1.0 -Dpackaging=jar
