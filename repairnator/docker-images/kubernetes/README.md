# Deploy a high avaliable Repairnator in Kubernetes

*On going pull request*

In order to make Repairnator highly available, it might be good to have configurations and tools to deploy Repairnator in Kubernetes, a preliminary idea here:

- A queue system like ActiveMQ to save build ids
- A K8s cron job running repairnator scaner to get latest build ids and add them into the queue
- A set of pods running repairnator pipeline which fetches build id from the queue and does the analysis