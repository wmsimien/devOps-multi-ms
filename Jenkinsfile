pipeline {
	agent any

    tools {
		jdk 'jdk17'
        maven 'maven'
    }

    stages {

		stage('Build & Tag Docker Image') {
			steps {
				script {
					withDockerRegistry(credentialsId: 'docker-cred', toolName: 'docker') {
						sh "docker build -t wmsimien/delivery-ms:latest ."
                    }
               }
            }
        }

        stage('Push Docker Image') {
			steps {
				script {
					withDockerRegistry(credentialsId: 'docker-cred', toolName: 'docker') {
						sh "docker push wmsimien/delivery-ms:latest"
                    }
               }
            }
        }
    }
}
