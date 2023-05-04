pipeline {
  agent {
    kubernetes {
      inheritFrom 'my-agent-pod'
      yaml """
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: maven
    image: repairnator/ci-env:latest
    command:
    - cat
    tty: true
    env:
      - name: "MAVEN_OPTS"
        value: "-Duser.home=/home/jenkins"
    volumeMounts:
      - name: settings-xml
        mountPath: /home/jenkins/.m2/settings.xml
        subPath: settings.xml
        readOnly: true
      - name: m2-repo
        mountPath: /home/jenkins/.m2/repository
    resources:
      limits:
        memory: "8Gi"
        cpu: "2"
      requests:
        memory: "8Gi"
        cpu: "2"
  volumes:
    - name: settings-xml
      secret:
        secretName: m2-secret-dir
        items:
        - key: settings.xml
          path: settings.xml
    - name: m2-repo
      emptyDir: {}
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
      options {
          timeout(time: 15, unit: "MINUTES")
      }
    }
    stage('repairnator-realtime'){
      environment {
        TEST_PATH="src/repairnator-realtime/"
        TEST_LIST="**"
        GITHUB_OAUTH=credentials('github-bot-token')
      }
      steps {
        container('maven') {
          sh 'bash ./.ci/ci-run-with-core.sh'
        }
      }
      options {
        timeout(time: 15, unit: "MINUTES")
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
      options {
        timeout(time: 15, unit: "MINUTES")
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
      options {
        timeout(time: 30, unit: "MINUTES")
      }
    }
    stage('repairnator-pipeline'){
      environment {
        TEST_PATH="src/repairnator-pipeline/"
        TEST_LIST="**"
      }
      steps {
        container('maven') {
          sh 'bash ./.ci/ci-run-with-core.sh'
        }
      }
      options {
        timeout(time: 2, unit: "HOURS")
      }
    }
  }
  post {
    always {
      junit testResults: '**/target/surefire-reports/*.xml'
    }
  }
}
