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
        GITHUB_TOKEN = credentials('jenkins-integration-with-github-account')
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
        booleanParam(
            name: 'RELEASE_TO_RC',
            defaultValue: false,
            description: 'Check this to prepare a new release (creates pre-release branch and PR)'
        )
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
                    sh 'git fetch --tags --force'

                    env.GIT_COMMIT_MSG = sh(
                        script: 'git log -1 --pretty=%B',
                        returnStdout: true
                    ).trim()
                }
            }
        }

        stage('Build jar') {
            steps {
                script {
                    def profile = '-P dev'
                    if (env.TAG_NAME) {
                        profile = '-P prod'
                    }
                    container('jdk-17') {
                        sh """
                            mvn -B clean package ${profile}
                            cp boot/target/carbonio-tasks-*-jar-with-dependencies.jar package/carbonio-tasks.jar
                        """
                    }
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
                   branch 'devel'
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

        /*
        * Here we build the deb/rpm packages: since the build uses the PKGBUILD file, we set its pkgrel
        * value here dynamically without committing the changes.
        */
        stage('Build deb/rpm') {
            steps {
                script {
                    // Determine pkgrel based on branch
                    if (env.GIT_BRANCH == 'devel') {
                        env.PKGREL = '1'
                        echo "Building RELEASE packages with pkgrel=1"
                    } else {
                        env.PKGREL = "SNAPSHOT-${env.GIT_COMMIT_SHORT}"
                        echo "Building SNAPSHOT packages with pkgrel=${env.PKGREL}"
                    }

                    // Modify PKGBUILD file
                    sh """
                        sed -i 's/pkgrel="SNAPSHOT"/pkgrel="${env.PKGREL}"/' package/PKGBUILD
                        cat package/PKGBUILD | grep pkgrel
                    """
                }

                echo 'Building deb/rpm packages'
                buildStage([
                    rockySinglePkg: true,
                    ubuntuSinglePkg: true
                ])

                script {
                    // Restore PKGBUILD to avoid committing changes
                    sh 'git checkout -- package/PKGBUILD'
                }
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

        /*
        * This creates a pre-release branch using semantic release to bump version and generate changelog
        */
        stage('Prepare Release') {
            when {
                allOf {
                    branch 'devel'
                    expression { params.RELEASE_TO_RC == true }
                    not {
                        expression {
                            return env.GIT_COMMIT_MSG.contains('[skip ci]') ||
                                   env.GIT_COMMIT_MSG.contains('chore(release):')
                        }
                    }
                }
            }
            steps {
                script {
                    sh '''
                        git config user.name "Jenkins CI"
                        git config user.email "ci@zextras.com"
                    '''

                    env.PRE_RELEASE_BRANCH = "pre-release"

                    sh """
                        git checkout devel
                        git pull origin devel

                        git branch -D ${env.PRE_RELEASE_BRANCH} 2>/dev/null || true
                        git push origin --delete ${env.PRE_RELEASE_BRANCH} 2>/dev/null || true

                        git checkout -b ${env.PRE_RELEASE_BRANCH}
                        git push origin ${env.PRE_RELEASE_BRANCH}
                    """

                    withEnv([
                        "GIT_BRANCH=${env.PRE_RELEASE_BRANCH}",
                        "BRANCH_NAME=${env.PRE_RELEASE_BRANCH}"
                    ]) {
                        sh 'npx semantic-release --no-ci'
                    }

                    env.RELEASE_VERSION = sh(
                        script: 'git describe --tags --abbrev=0',
                        returnStdout: true
                    ).trim()

                    sh """
                        git push origin --delete ${env.RELEASE_VERSION} 2>/dev/null || true
                        git tag -d ${env.RELEASE_VERSION}
                        git push origin ${env.PRE_RELEASE_BRANCH}
                    """

                    def prBody = """🤖 Automated release preparation for ${env.RELEASE_VERSION}"""

                    sh """
                        curl -X POST \
                          -H "Authorization: token ${GITHUB_TOKEN_PSW}" \
                          -H "Accept: application/vnd.github.v3+json" \
                          https://api.github.com/repos/zextras/carbonio-tasks-ce/pulls \
                          -d '{"title": "Release ${env.RELEASE_VERSION}", "head": "${env.PRE_RELEASE_BRANCH}", "base": "devel", "body": ${groovy.json.JsonOutput.toJson(prBody)}}' > pr-response.json
                    """

                    env.PR_NUMBER = sh(
                        script: 'cat pr-response.json | grep -o \'"number": [0-9]*\' | grep -o \'[0-9]*\'',
                        returnStdout: true
                    ).trim()

                    echo "Created PR #${env.PR_NUMBER} for release ${env.RELEASE_VERSION}"
                }
            }
        }

        stage('Tag for release') {
            when {
                allOf {
                    branch 'devel'
                    expression {
                        return env.GIT_COMMIT_MSG.contains('chore(release):') &&
                               env.GIT_COMMIT_MSG.contains('[skip ci]')
                    }
                    expression {
                        def version = sh(
                            script: 'echo "${GIT_COMMIT_MSG}" | grep -oP "chore\\\\(release\\\\): \\\\K[0-9]+\\\\.[0-9]+\\\\.[0-9]+" || echo ""',
                            returnStdout: true
                        ).trim()

                        if (!version) {
                            return false
                        }

                        sh 'git fetch --tags --force'

                        def tagExists = sh(
                            script: "git tag -l v${version}",
                            returnStdout: true
                        ).trim()

                        return tagExists == ''
                    }
                }
            }
            steps {
                script {
                    sh '''
                        git config user.name "Jenkins CI"
                        git config user.email "ci@zextras.com"

                        VERSION=$(echo "${GIT_COMMIT_MSG}" | grep -oP "chore\\(release\\): \\K[0-9]+\\.[0-9]+\\.[0-9]+")
                        TAG="v${VERSION}"

                        git tag -a "${TAG}" -m "chore(release): ${VERSION}"
                        git push origin "${TAG}"
                    '''

                    env.TAG_CREATED = 'true'

                    def tagName = sh(script: 'git describe --tags --abbrev=0', returnStdout: true).trim()
                    echo "Created and pushed tag ${tagName}"
                }
            }
        }

        stage('Build and Publish Docker Image') {
            when {
                not {
                    anyOf {
                        buildingTag()
                        expression { env.BRANCH_NAME.startsWith('PR-') }
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