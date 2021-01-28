cd ../repairnator-core
mvn install -DskipTests
cd ../repairnator-pipeline
mvn package -DskipTests
cp target/repairnator-realtime-3.3-SNAPSHOT-jar-with-dependencies.jar ../docker-images/pipeline-dockerimage/
cd ../docker-images/pipeline-dockerimage/
docker build . -t repairnator/pipeline:sorald