# Introduction
This is the yaml file used for deploying mongodb on kubernetes as a pod on a persistent volume. Read instructions below to see how that is done. The idea with deploying mongodb on cloud is to have scalability and high availability as we prefer running 24/7 on cloud instead of on our private computer, which make mongodb unavailable when the computer is shutdown. Also this make things easier  since hybrid enviroment(partially on cloud and partially outside make certain thing like networking a bit more complicated). 

# How to deploy
Note: this deployment requires the following to be done
* kubectl 
* gcloud

First create a minimum-recommended persistent volume called mongo-disk 
* gcloud compute disks create --size=200GB --zone=$ZONE mongo-disk

The cost for maintain this is about 9$/month according to the google cost calculator. Zone can be found by the command "gcloud init" to get your current project's zone. Next is to apply the yaml 

* kubectl create -f mongodb.yaml

To access mongodb, first we need to get the name of the pod

* kubectl get pods 

It should look like "mongo-controller-XXXXX", then access just like docker and the usual mongo

* kubectl exec -it mongo-controller-vx9vn bash 
* mongo 

Note expose mongodb as the usual "localhost:27017" . use port-forwarding 
* kubectl port-forward mongo-controller-4vw9v 27017:27017

Note that if you already have something running at port 27017, use 27018:27017 and access to "localhost:27018" instead.


