# Repairnator in Kubernetes

This page documents how to deploy the Repairnator components in K8.

## Prerequisites

* kubectl
* a working k8s cluster (preferable high cpu capacity)
* gcloud (for creating the cluster mentoned above and creating mongodb if you don't have them already)

The starting folder for these deployment below is `repairnator/kubernetes-support`.

## Run

Setup activeMQ

```
cd repairnator/kubernetes-support
kubectl create -f queue-for-buildids/activemq.yaml
```

BuildRainer: build build-rainer jar and run it.

```
cd repairnator/build-rainer
mvn install -DskipTest
java -jar target/build-rainer-1.0-SNAPSHOT-jar-with-dependencies.jar
```

build rainer should now be running and submitting builds to `pipeline` queue on ActiveMQ.

Worker: to deploy a pipeline worker which pulls build ids from ActiveMQ, run repair attempts and creates PRs if patches are found:

Note that you may define some of the env values in `repairnator-pipeline.yaml` before running the following command.  

```
kubectl create -f ./repairnator/kubernetes-support/repairnator-deployment-yamlfiles/repairnator-pipeline.yaml
```


## Troubleshooting

Proxy activemq server 

```
kubectl get pods
kubectl port-forward activemq-XXXXXXX-XXXXX 1099:1099 8161:8161 61613:61613 61616:61616
```

Send a build id to queue 

```
# 566070885 is a failed travis build from this [repo](https://github.com/Tailp/travisplay)
python /queue-for-buildids/publisher.py -d /queue/pipeline 566070885
```

Regarding the format of queue messages, repairnator pipeline supports the following ones:

- a plain text with a build id only  
- a plain text with a JSON string  
- a bytes message with a build id only  
- a bytes message with a JSON string

```json
{"buildId":"648902893","CI":"travis-ci.org"}
```

Check the pipeline output by

```
kubectl get pods 
kubectl logs -f repairnator-pipeline-XXXXXXXXX-XXXXX
```

Upgrade the pipeline to use the latest Docker image: we could simply delete the pods. K8s will pull the latest image and recreate new pipeline pods.

```
kubectl delete repairnator-pipeline-XXXXXXXXX-XXXXX
```
