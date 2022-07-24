pipeline {
  agent none
  stages {
    stage('检查maven') {
         agent {
           label 'maven'
         }
         steps {
            sh 'sleep 20s'
            echo 'check maven settings.xml'
            sh 'cat /root/maven/settings.xml'
            echo 'maven版本:'
            sh 'mvn -v'
            echo 'jdk版本'
            sh 'java -version'
         }
    }

    stage('检查docker') {
         agent {
           label 'docker'
         }
         steps {
            sh 'sleep 5s'
            sh 'docker login -u admin -p ZKwin123.. harbor.hyena.ink:82'
            sh 'sleep 5s'
            sh 'docker pull harbor.hyena.ink:82/project/mysql:5.7'
            sh 'docker images | grep mysql'
         }
    }

    stage('检查kubectl') {
         agent {
           label 'kubectl'
         }
         steps {
            sh 'sleep 10s'
            echo '查看版本:'
            sh 'kubectl version'
            echo '查看pod'
            sh 'kubectl get po'
         }
    }
  }
}