pipeline {
    agent any

    environment {
        AWS_REGION       = 'ap-northeast-2'
        ECR_REGISTRY     = credentials('ecr-registry-url')       // e.g. 123456789012.dkr.ecr.ap-northeast-2.amazonaws.com
        ECR_REPOSITORY   = 'locker-room/resource-service'
        IMAGE_TAG        = "${env.BUILD_NUMBER}-${env.GIT_COMMIT?.take(7) ?: 'unknown'}"
        DEV_SERVER_HOST  = credentials('dev-server-host')         // dev 서버 IP 또는 호스트
        DEV_SERVER_USER  = credentials('dev-server-user')         // SSH 사용자
        COMPOSE_DIR      = '/opt/locker-room'                     // dev 서버의 docker-compose 경로
    }

    stages {

        // ── 1. Git Pull ──
        stage('Git Pull') {
            steps {
                checkout scm
            }
        }

        // ── 2. Build ──
        stage('Build') {
            steps {
                script {
                    dockerImage = docker.build(
                        "${ECR_REGISTRY}/${ECR_REPOSITORY}:${IMAGE_TAG}",
                        '--no-cache .'
                    )
                }
            }
        }

        // ── 3. JUnit Test ──
        stage('JUnit Test') {
            agent {
                docker {
                    image 'maven:3.9-eclipse-temurin-17'
                    reuseNode true
                }
            }
            steps {
                sh 'mvn test -B'
            }
            post {
                always {
                    junit testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true
                }
            }
        }

        // ── 4. Docker Image Upload (ECR) ──
        stage('Docker Image Upload') {
            steps {
                script {
                    sh "aws ecr get-login-password --region ${AWS_REGION} | docker login --username AWS --password-stdin ${ECR_REGISTRY}"
                    dockerImage.push(IMAGE_TAG)
                    dockerImage.push('latest')
                }
            }
        }

        // ── 5. Dev Deploy ──
        stage('Dev Deploy') {
            steps {
                sshagent(credentials: ['dev-server-ssh-key']) {
                    sh """
                        ssh -o StrictHostKeyChecking=no ${DEV_SERVER_USER}@${DEV_SERVER_HOST} '
                            aws ecr get-login-password --region ${AWS_REGION} | docker login --username AWS --password-stdin ${ECR_REGISTRY}
                            cd ${COMPOSE_DIR}
                            export RESOURCE_SERVICE_IMAGE=${ECR_REGISTRY}/${ECR_REPOSITORY}:${IMAGE_TAG}
                            docker compose pull resource-service
                            docker compose up -d resource-service
                        '
                    """
                }
            }
        }
    }

    post {
        success {
            echo "Pipeline succeeded — image: ${ECR_REGISTRY}/${ECR_REPOSITORY}:${IMAGE_TAG}"
        }
        failure {
            echo 'Pipeline failed'
        }
    }
}
