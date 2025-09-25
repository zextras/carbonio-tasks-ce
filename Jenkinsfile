// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

library(
    identifier: 'jenkins-packages-build-library@1.0.4',
    retriever: modernSCM([
        $class: 'GitSCMSource',
        remote: 'git@github.com:zextras/jenkins-packages-build-library.git',
        credentialsId: 'jenkins-integration-with-github-account'
    ])
)

pipeline {
    agent {
        node {
            label 'zextras-v1'
        }
    }

    environment {
        JAVA_OPTS = '-Dfile.encoding=UTF8'
        LC_ALL = 'C.UTF-8'
        jenkins_build = 'true'
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '25'))
        skipDefaultCheckout()
        timeout(time: 1, unit: 'HOURS')
    }

    parameters {
        booleanParam defaultValue: false,
            description: 'Whether to upload the packages in playground repositories',
            name: 'PLAYGROUND'
    }

    tools {
        jfrog 'jfrog-cli'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    gitMetadata()
                }
            }
        }

        stage('Build jar') {
            steps {
                container('jdk-17') {
                    sh 'mvn -B clean package'
                    sh 'cp boot/target/carbonio-tasks-*-jar-with-dependencies.jar package/carbonio-tasks.jar'
                }
            }
        }

        stage('Unit tests') {
            steps {
                container('jdk-17') {
                    sh 'mvn -B verify -P run-unit-tests'
                }
            }
        }

        stage('Integration tests') {
            steps {
                container('jdk-17') {
                    sh 'mvn -B verify -P run-integration-tests'
                }
            }
        }

        stage('Coverage') {
            steps {
                container('jdk-17') {
                    sh 'mvn -B verify -P generate-jacoco-full-report'
                    recordCoverage(
                        tools: [[parser: 'JACOCO']],
                        sourceCodeRetention: 'MODIFIED'
                    )
                }
            }
        }

        stage('SonarQube analysis') {
            when {
               anyOf {
                   branch 'develop'
                   expression { env.BRANCH_NAME.contains("PR") }
               }
            }
            steps {
                container('jdk-17') {
                    withSonarQubeEnv(credentialsId: 'sonarqube-user-token', installationName: 'SonarQube instance') {
                        sh 'mvn -B sonar:sonar'
                    }
                }
            }
        }

        stage('Build deb/rpm') {
            steps {
                echo 'Building deb/rpm packages'
                buildStage([
                    rockySinglePkg: true,
                    ubuntuSinglePkg: true
                ])
            }
        }

        stage('Upload artifacts')
        {
            steps {
                uploadStage(
                    packages: yapHelper.getPackageNames(),
                    rockySinglePkg: true,
                    ubuntuSinglePkg: true
                )
            }
        }

        stage('Build and Publish Docker Image') {
            when {
                not {
                    anyOf {
                        buildingTag()
                        expression { env.BRANCH_NAME.startsWith("PR-") }
                    }
                }
            }
            steps {
                container('dind') {
                    withDockerRegistry([
                        credentialsId: 'private-registry',
                        url: 'https://registry.dev.zextras.com'
                    ]) {
                        script {
                            String branchTag = env.BRANCH_NAME.replaceAll('/', '-').toLowerCase()
                            Set<String> imageTags = [ branchTag ]

                            if (env.BRANCH_NAME == 'devel') {
                                imageTags.add('latest')
                            } else if (buildingTag() && env.TAG_NAME?.trim()) {
                                imageTags.add(env.TAG_NAME?.startsWith('v') ? env.TAG_NAME.substring(1) : env.TAG_NAME)
                            }

                            dockerHelper.buildImage([
                                imageName: 'registry.dev.zextras.com/dev/carbonio-tasks-ce',
                                imageTags: imageTags,
                                dockerfile: 'docker/minimal/carbonio-tasks/Dockerfile',
                                ocLabels: [
                                    title: 'Carbonio tasks CE',
                                    description: 'Carbonio tasks Community Edition',
                                    version: branchTag
                                ]
                            ])
                        }
                    }
                }
            }
        }
    }
}
