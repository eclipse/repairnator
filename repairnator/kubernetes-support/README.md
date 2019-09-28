# Deploy Repairnator in Kubernetes

## Prerequisites

* kubectl
* a working k8s cluster (preferable high cpu capacity)
* gcloud (for creating the cluster mentoned above and creating mongodb if you don't have them already)

The starting folder for these deployment below is `repairnator/kubernetes-support`.

## Run
Setup activeMQ and pipeline

```
cd repairnator/kubernetes-support
kubectl create -f queue-for-buildids/activemq.yaml
kubectl create -f queue-for-buildids/repairnator-pipeline.yaml
```

Send build ids to pipeline, first proxy activemq server 

```
kubectl get pods
kubectl port-forward activemq-XXXXXXX-XXXXX 1099:1099 8161:8161 61613:61613 61616:61616

# 566070885 is a failed travis build from this [repo](https://github.com/Tailp/travisplay)
python /queue-for-buildids/publisher.py -d /queue/pipeline 566070885
```

Check the pipeline output by

```
kubectl get pods 
kubectl logs -f repairnator-pipeline-XXXXXXXXX-XXXXX
```

Setup BuildRainer

Clone [tdurieux/travis-listener](https://github.com/tdurieux/travis-listener)
```
git clone git@github.com:tdurieux/travis-listener.git
cd script
npm install
npm run-script build
npm run-script server
```

Then for repairnator git clone , build build-rainer jar and run it.

```
cd repairnator/build-rainer
mvn install -DskipTest
java -jar target/build-rainer-1.0-SNAPSHOT-jar-with-dependencies.jar
```

build rainer should now be running and submitting builds to `pipeline` queue on ActiveMQ.
