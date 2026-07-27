// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

library(
    identifier: 'jenkins-lib-common@native-it',
    retriever: modernSCM([
        $class: 'GitSCMSource',
        credentialsId: 'jenkins-integration-with-github-account',
        remote: 'git@github.com:zextras/jenkins-lib-common.git',
    ])
)

properties(defaultPipelineProperties())

dt3_pipeline(
    repoName: 'carbonio-tasks-ce',
    mavenPublish: ['app'],
    nativeBuild: [runnerName: 'carbonio-tasks-ce-runner'],
    packaging: [
        buildFlags: '-ds',
    ],
    docker: [
        [dockerfile: 'docker/Dockerfile',
         imageName: 'carbonio-tasks-ce',
         title: 'Carbonio Tasks CE',
         description: 'Carbonio Tasks CE Service'],
    ],
    reuse: [projectType: 'CE'],
    flywayGuard: [
        migrationPaths: ['app/src/main/resources/db/migration'],
    ]
)
