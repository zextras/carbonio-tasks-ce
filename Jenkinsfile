// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

library(
    identifier: 'jenkins-lib-common@dt3-pipeline',
    retriever: modernSCM([
        $class: 'GitSCMSource',
        credentialsId: 'jenkins-integration-with-github-account',
        remote: 'git@github.com:zextras/jenkins-lib-common.git',
    ])
)

properties(defaultPipelineProperties())

dt3_pipeline(
    repoName: 'carbonio-tasks-ce',
    projectType: 'CE',
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
    reuse: [:],
    bumpDownstream: [
        repo:                   'zextras/carbonio-tasks',
        branch:                 'devel',
        property:               'carbonio-tasks-ce.version',
        notificationRecipients: [
            'matteo.galvagni@zextras.com',
            'noman.alishaukat@zextras.com',
            'riccardo.degan@zextras.com',
        ],
    ]
)
