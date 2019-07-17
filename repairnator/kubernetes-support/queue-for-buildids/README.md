# Introduction
This is for deploying ActiveMQ on K8s cloud.

This require kubectl to work. To create 
* kubectl create -f activemq.yaml

To access 
* kubectl get pods 
It should look like activemq-XXXXXXX-XXXXX. Then 
* kubectl exec -it activemq-XXXXXXX-XXXXX bash
To expose to localhost for webinterface access and also publishing(sending message to queue) outside the cluster. 
* kubectl port-forward activemq-XXXXXXX-XXXXX 8161:8161 61613:61613
Access webinterface with "localhost:8161" in your browser. The default username is "admin" and password is also "admin".

To send message to queue use the publisher.py script. Syntax
* python publisher.py -d queue-name message-1 message-2 message3 ... message-N

queue-name is according to the format /queue/name. For instance /queue/scanner will send message to the scanner queue.
