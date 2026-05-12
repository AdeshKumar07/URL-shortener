// ═══════════════════════════════════════════════════════════════════
// INT332 DevOps Project - Jenkins Declarative Pipeline
// Stages: Checkout → Build & Test → Build Docker Image → Push
// ═══════════════════════════════════════════════════════════════════

pipeline {

    agent any

    // ── Environment Variables ────────────────────────────────────
    environment {
        // IMPORTANT: Replace with your actual Docker Hub username,
        // or set DOCKERHUB_USERNAME as a Jenkins credential / env var.
        IMAGE_NAME = "YOUR_DOCKERHUB_USERNAME/url-shortener"
        IMAGE_TAG  = "${env.BUILD_NUMBER}"
    }

    // ── Global Tools (configured in Jenkins → Global Tool Config) ─
    tools {
        jdk   'JDK17'      // Must match name in Jenkins Global Tools
        maven 'Maven3.9'   // Must match name in Jenkins Global Tools
    }

    // ── Pipeline Stages ──────────────────────────────────────────
    stages {

        // Stage 1: Checkout code from GitHub
        stage('Checkout') {
            steps {
                echo '=== Checking out source code ==='
                checkout scm
                echo "Branch: ${env.GIT_BRANCH}"
                echo "Commit: ${env.GIT_COMMIT}"
            }
        }

        // Stage 2: Build and run unit tests with Maven
        stage('Build & Test') {
            steps {
                echo '=== Building and testing with Maven ==='
                sh 'mvn clean package'
                echo 'Maven build successful'
            }
            post {
                always {
                    // Publish JUnit test results in Jenkins UI
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }

        // Stage 3: Build Docker image
        stage('Build Docker Image') {
            steps {
                echo '=== Building Docker image ==='
                sh "docker build -t ${IMAGE_NAME}:${IMAGE_TAG} ."
                sh "docker tag  ${IMAGE_NAME}:${IMAGE_TAG} ${IMAGE_NAME}:latest"
                echo "Image built: ${IMAGE_NAME}:${IMAGE_TAG}"
            }
        }

        // Stage 4: Push image to Docker Hub
        stage('Push to Docker Hub') {
            steps {
                echo '=== Pushing image to Docker Hub ==='
                withCredentials([usernamePassword(
                    credentialsId: 'docker-hub-creds',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    sh 'echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin'
                    sh "docker push ${IMAGE_NAME}:${IMAGE_TAG}"
                    sh "docker push ${IMAGE_NAME}:latest"
                    echo "Image pushed successfully"
                }
            }
        }
    }

    // ── Post Actions ─────────────────────────────────────────────
    post {
        success {
            echo """
            ╔══════════════════════════════════════╗
            ║  Pipeline SUCCEEDED!                 ║
            ║  Image: ${IMAGE_NAME}:${IMAGE_TAG}   ║
            ╚══════════════════════════════════════╝
            """
        }
        failure {
            echo """
            ╔══════════════════════════════════════╗
            ║  Pipeline FAILED                     ║
            ║  Check the stage logs above          ║
            ╚══════════════════════════════════════╝
            """
        }
        // FIX: The original used single quotes: 'Pipeline finished. Build #${env.BUILD_NUMBER}'
        // In Groovy, single-quoted strings do NOT interpolate variables — ${env.BUILD_NUMBER}
        // was printed literally instead of the actual build number.
        // Changed to double quotes so the variable is expanded correctly.
        always {
            echo "Pipeline finished. Build #${env.BUILD_NUMBER}"
        }
    }
}
