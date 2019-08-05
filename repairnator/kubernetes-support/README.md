# Deploy Repairnator in Kubernetes

## Prerequisites

* kubectl
* a working k8s cluster (preferable high cpu capacity)
* gcloud (for creating the cluster mentoned above and creating mongodb if you don't have them already)

The starting folder for these deployment below is "repairnator/kubernetes-support".
## Run
Setup activeMQ 
* cd repairnator/kubernetes-support
* kubectl create -f queue-for-buildids/activemq.yaml
* kubectl create -f queue-for-buildids/repairnator-pipeline.yaml

Send build ids to pipeline, first proxy activemq server 
* kubectl get pods
* kubectl port-forward activemq-XXXXXXX-XXXXX 8161:8161 61613:61613
* python /queue-for-buildids/publisher.py -d /queue/pipeline 566070885

566070885 is a failed travis build from this [repo](https://github.com/Tailp/travisplay). Check the pipeline output by

* kubectl get pods 
* kubectl logs -f repairnator-pipeline-XXXXXXXXX-XXXXX
