pipeline {
    agent any
    tools {
        maven 'maven'
    }
    stages {
        stage('Checkout') {
            steps {
                git branch: 'main',
                    url: 'https://github.com/Aziz0112/abonnement.git'
            }
        }
        stage('Build & Test') {
            steps {
                sh 'mvn clean verify -B'
            }
        }
        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('sonarqube') {
                    sh '''mvn sonar:sonar \
                        -Dsonar.projectKey=minolingo-abonnement \
                        -Dsonar.projectName=minolingo-abonnement \
                        -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml'''
                }
            }
        }
        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }
        stage('Docker Build') {
            steps {
                sh 'docker build -t nsiriaziz/minolingo-abonnement:latest .'
            }
        }
        stage('Docker Push') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-credentials',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    sh 'echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin'
                    sh 'docker push nsiriaziz/minolingo-abonnement:latest'
                }
            }
        }
        stage('Trigger CD') {
            steps {
                build job: 'minolingo-cd-pipeline',
                      wait: true
            }
        }
    }
    post {
        always {
            jacoco(
                execPattern: 'target/jacoco.exec',
                classPattern: 'target/classes',
                sourcePattern: 'src/main/java',
                exclusionPattern: 'src/test*'
            )
        }
        success { echo 'CI Pipeline completed successfully!' }
        failure { echo 'CI Pipeline failed!' }
    }
}
