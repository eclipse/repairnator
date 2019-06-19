# Deploy a high avaliable Repairnator in Kubernetes

*On going pull request*

In order to make Repairnator highly available, it might be good to have configurations and tools to deploy Repairnator in Kubernetes, a preliminary idea here:

- A queue system like ActiveMQ to save build ids
- A K8s cron job running repairnator scaner to get latest build ids and add them into the queue
- A set of pods running repairnator pipeline which fetches build id from the queue and does the analysis

# How to set up repairnator on K8s

## Setup mongodb and ActiveMQ
First set up mongodb on kubernetes with a persistent memory volume(This mean that even if you erase the deployment and redeploy nothing will be lost, the database would look the same)

So first create a persistent volume(ZONE can be found with gcloud init for your current project)
* gcloud compute disks create --size=200GB --zone=$ZONE mongo-disk

Then create the mongodb deployemnt using mongodb.yaml in the k8s-mongodb folder .

* kubectl create -f k8s-mongodb/mongodb.yaml

Try it out
* kubectl get pods 
Our mongodb pod should be named like "mongo-controller-XXXXX" then we can get into the pod 
* kubectl exec -it mongo-controller-XXXXX bash
* mongo

Now deploy ActiveMQ for effective queue managements of jobs to the pipeline and scanner. First apply the yaml file inside the queue-for-buildids folder
* kubectl create -f /queue-for-buildids/activemq.yaml
To access to the web interface
* kubectl get pods 
It should look like activemq-XXXXXXX-XXXXX. Then 
* kubectl exec -it activemq-XXXXXXX-XXXXX bash
To expose to localhost for webinterface access and also publishing(sending message to queue) outside the cluster. 
* kubectl port-forward activemq-XXXXXXX-XXXXX 8161:8161 61613:61613
Access webinterface with "http://localhost:8161/admin/" in your browser. The default username is "admin" and password is also "admin".

To send message to queue use the publisher.py script in queue-for-buildids folder. Syntax
* python publisher.py -d queue-name message-1 message-2 message3 ... message-N

queue-name is according to the format /queue/name. For instance /queue/scanner will send message to the scanner queue.
You can use this later to manually input build id to the pipeline queue or project to the scanner queue

## Setup scanners and pipelines

To deploy the scanner first go into folder "scanner-dockerimage" then fill in the enviroment variables(env variable=value) in the Dockerfile. Then you can build the image and push it to your registry. With docker 
* docker build -t repairnator-scanner:tagname .
* docker tag repairnator-scanner:tagname YOUR_DOCKERHUB_NAME/repairnator-scanner
* docker push YOUR_DOCKERHUB_NAME/repairnator-scanner:tagname
Then go to "repairnator-deployment-yamlfiles" folder. Open repairnator-scanner.yaml then replace the image 
´´´
...
	containers:
      - name: repairnator-scanner
        image: YOUR_DOCKERHUB_NAME/repairnator-scanner:tagname
...
´´´
Now create the scanner with 
* kubectl create -f repairnator-scanner.yaml
Then go to "http://localhost:8161/admin/queues.jsp" and you should see a scanner queue with one consumer.

Same goes for the pipeline, first go to "pipeline-dockerimage" folder to modify the Dockerfile and then docker build and push it and modify the yaml file then create with kubectl and you should also see on the same page a pipeline queue with one consumer.

## Example run
In the folder "queue-for-buildids" use publisher.py to send messages to the queue. If you have a slug (like "surli/failingProject") you can send it to the scanner queue for scanning. The scanner will auto submit the interesting BuildIds to the pipeline queue for generating patches. So sending by
* python publisher.py /queue/scanner surli/failingProject
This will give some patches to the pipeline and you can confirm that by checking at the webpage for message enqueued. Then you can also see that in the pipeline pod. 
* kubectl get pods
You should see something like repairnator-scanner-XXXXXXXXX-XXXXX . Watch the output of the scanner by(in realtime with flag -f)
* kubectl logs repairnator-scanner-XXXXXXXXX-XXXXX
Also for the pipeline if there are some interesting build gotten from the scanner you will be able to see it with 
* kubectl logs repairnator-pipeline-XXXXXXXXX-XXXXX
The output of the pipeline such as patches, hardwareinfo .. is submitted to the cloud mongodb just like the non-cloud repairnator.

To scale this either you can change the "replicas" value in the yaml file and reapply the yaml or scale it with kubectl
* kubectl scale --replicas=3 -f repairnator-pipeline.yaml
This will create 2 more pipelines and you should be able to see it "http://localhost:8161/admin/queues.jsp" . Same thing with the scanner if you need more than one like for instance if there are too many jobs in the queue.


## Delete deployment
Provided with every yaml file mentioned in this readme, call 
* kubectl delete -f "yamlfile" 
To remove each of them. For instance kubectl delete -f repairnator-scanner.yaml to remove the scanners. 


























