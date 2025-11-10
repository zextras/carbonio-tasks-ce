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

def gitSetup() {
    sh '''
        git config user.email "bot@zextras.com"
        git config user.name "Tarsier Bot"
    '''

    def repoOriginUrl = sh(
        script: "git remote -v | head -n1 | cut -d\$'\t' -f2 | cut -d' ' -f1",
        returnStdout: true
    ).trim()

    if (repoOriginUrl.startsWith('git@github.com:')) {
        def newOriginUrl = repoOriginUrl.replaceFirst(
            'git@github.com:',
            "https://\${ZXBOT_TOKEN}@github.com/"
        )
        sh "git remote set-url origin ${newOriginUrl}"
        echo "Remote changed to HTTPS with token authentication"
    }
}

def gitPush(Map opts = [:]) {
    def gitOptions = []
    if (opts.followTags == true) {
        gitOptions << '--follow-tags'
    }

    sh "git push ${gitOptions.join(' ')} origin HEAD:${opts.branch}"
}

def openGithubPr(Map args = [:]) {
    def repoOwner = 'zextras'
    def repoName = 'carbonio-tasks-ce'

    echo "Creating PR on ${repoOwner}/${repoName}"

    sh """
        curl -f -L \
          -X POST \
          -H "Accept: application/vnd.github+json" \
          -H "Authorization: Bearer \${ZXBOT_TOKEN}" \
          -H "X-GitHub-Api-Version: 2022-11-28" \
          https://api.github.com/repos/${repoOwner}/${repoName}/pulls \
          -d '{
            "title": "${args.title}",
            "head": "${args.head}",
            "base": "${args.base}",
            "body": "${args.body ?: ''}",
            "maintainer_can_modify": true
          }'
    """
}

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
                    if (env.GIT_BRANCH == 'devel') {
                        env.PKGREL = '1'
                        echo "Building RELEASE packages with pkgrel=1"
                    } else {
                        env.PKGREL = "SNAPSHOT-${env.GIT_COMMIT_SHORT}"
                        echo "Building SNAPSHOT packages with pkgrel=${env.PKGREL}"
                    }

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
                    sh 'git checkout -- package/PKGBUILD'
                }
            }
        }

        stage('Upload artifacts') {
            steps {
                uploadStage(
                    packages: yapHelper.getPackageNames(),
                    rockySinglePkg: true,
                    ubuntuSinglePkg: true
                )
            }
        }

        stage('Prepare Release') {
            agent {
                node {
                    label 'nodejs-v1'
                }
            }
            when {
                allOf {
                    /*branch 'devel' TODO uncomment after testing*/
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
                    container('nodejs-20') {
                        checkout([
                            $class: 'GitSCM',
                            branches: scm.branches,
                            userRemoteConfigs: scm.userRemoteConfigs,
                            extensions: [
                                [$class: 'CloneOption', noTags: false, shallow: false]
                            ]
                        ])

                        withCredentials([
                            usernamePassword(
                                credentialsId: 'jenkins-integration-with-github-account',
                                passwordVariable: 'ZXBOT_TOKEN',
                                usernameVariable: 'ZXBOT_NAME'
                            )
                        ]) {
                            sh 'apt-get update && apt-get install -y openssh-client'

                            gitSetup()

                            env.PRE_RELEASE_BRANCH = "pre-release"

                            sh """
                                git fetch --unshallow || true
                                git checkout devel
                                git pull origin devel

                                git branch -D ${env.PRE_RELEASE_BRANCH} 2>/dev/null || true
                                git push origin --delete ${env.PRE_RELEASE_BRANCH} 2>/dev/null || true

                                git checkout -b ${env.PRE_RELEASE_BRANCH}
                                git push origin ${env.PRE_RELEASE_BRANCH}
                            """

                            withEnv(["GITHUB_TOKEN=${env.ZXBOT_TOKEN}"]) {
                                sh '''
                                    npx semantic-release --no-ci || {
                                        echo "Semantic release failed or not configured"
                                        echo "Continuing without version bump..."
                                    }
                                '''
                            }

                            env.RELEASE_VERSION = sh(
                                script: 'git describe --tags --abbrev=0 2>/dev/null',
                                returnStdout: true
                            ).trim()

                            sh """
                                git push origin --delete ${env.RELEASE_VERSION} 2>/dev/null || true
                                git tag -d ${env.RELEASE_VERSION} 2>/dev/null || true
                            """

                            gitPush(
                                branch: env.PRE_RELEASE_BRANCH,
                                followTags: false
                            )

                            echo "Pre-release branch created: ${env.PRE_RELEASE_BRANCH}"
                            echo "Target version: ${env.RELEASE_VERSION}"
                        }
                    }
                }
            }
            post {
                success {
                    script {
                        container('nodejs-20') {
                            withCredentials([
                                usernamePassword(
                                    credentialsId: 'jenkins-integration-with-github-account',
                                    passwordVariable: 'ZXBOT_TOKEN',
                                    usernameVariable: 'ZXBOT_NAME'
                                )
                            ]) {
                                catchError(buildResult: 'SUCCESS', stageResult: 'SUCCESS') {
                                    openGithubPr(
                                        title: "Release ${env.RELEASE_VERSION}",
                                        head: env.PRE_RELEASE_BRANCH,
                                        base: 'devel',
                                        body: "🤖 Automated release preparation for ${env.RELEASE_VERSION}"
                                    )
                                    echo "Pull Request created successfully"
                                }
                            }
                        }
                    }
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
                }
            }
            steps {
                script {
                    def version = sh(
                        script: 'echo "${GIT_COMMIT_MSG}" | grep -oP "chore\\\\(release\\\\): \\\\K[0-9]+\\\\.[0-9]+\\\\.[0-9]+" || echo ""',
                        returnStdout: true
                    ).trim()

                    if (!version) {
                        echo "No version found in commit message, skipping"
                        return
                    }

                    def tag = "v${version}"

                    withCredentials([
                        usernamePassword(
                            credentialsId: 'jenkins-integration-with-github-account',
                            passwordVariable: 'ZXBOT_TOKEN',
                            usernameVariable: 'ZXBOT_NAME'
                        )
                    ]) {
                        gitSetup()

                        sh 'git fetch --tags --force'

                        def tagExists = sh(
                            script: "git tag -l ${tag}",
                            returnStdout: true
                        ).trim()

                        if (tagExists) {
                            echo "Tag ${tag} already exists, skipping"
                            return
                        }

                        sh """
                            git tag -a "${tag}" -m "chore(release): ${version}"
                            git push origin "${tag}"
                        """

                        echo "Created and pushed tag ${tag}"
                    }
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
                catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
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
}