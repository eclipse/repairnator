pipeline {
  agent {
    kubernetes {
      label 'my-agent-pod'
      yaml """
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: maven
    image: andre15silva/repairnator-ci:latest
    command:
    - cat
    tty: true
    resources:
      limits:
        memory: "4Gi"
        cpu: "2"
      requests:
        memory: "4Gi"
        cpu: "2"
"""
    }
  }
    stages {
        stage('repairnator-core'){
            environment {
                TEST_PATH="src/repairnator-core/"
            }
            steps {
                container('maven') {
                  sh 'bash ./.ci/ci-run.sh'
                }
            }
        }
        stage('repairnator-realtime'){
            environment {
                TEST_PATH="src/repairnator-realtime/"
                TEST_LIST="**"
            }
            steps {
                container('maven') {
                  sh 'bash ./.ci/ci-run-with-core.sh'
                }
            }
        }
        stage('repairnator-jenkins-plugin'){
            environment {
                TEST_PATH="src/repairnator-jenkins-plugin/"
            }
            steps {
                container('maven') {
                  sh 'bash ./.ci/ci-run.sh'
                }
            }
        }
        stage('repairnator-maven-repair'){
            environment {
                TEST_PATH="src/maven-repair/"
                TEST_LIST="**"
            }
            steps {
                container('maven') {
                  sh 'bash ./.ci/ci-maven-repair.sh'
                }
            }
        }
    }
}
