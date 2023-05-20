<!--
SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>

SPDX-License-Identifier: AGPL-3.0-only
-->

# Changelog

All notable changes to this project will be documented in this file. See [standard-version](https://github.com/conventional-changelog/standard-version) for commit guidelines.

### 0.0.1 (2023-04-27)


### Features

* TSK-12 - Setup the repository and an empty maven project ([#1](https://github.com/Zextras/carbonio-tasks-ce/issues/1)) ([5b93cb9](https://github.com/Zextras/carbonio-tasks-ce/commit/5b93cb953c097f7f45f74025b5f07ba64346dcc5))
* TSK-13 - Add GraphQL and RESTEasy servlet + health APIs ([#3](https://github.com/Zextras/carbonio-tasks-ce/issues/3)) ([e7d2013](https://github.com/Zextras/carbonio-tasks-ce/commit/e7d2013c8e7f33d305a959e2382db76fc928471b))
* TSK-14 - Create package and add consul configuration ([#2](https://github.com/Zextras/carbonio-tasks-ce/issues/2)) ([19861f6](https://github.com/Zextras/carbonio-tasks-ce/commit/19861f6c0da78b82392fc7852df1238ed0c4fb41))
* TSK-19 - Add Task entity and TaskRepository ([#7](https://github.com/Zextras/carbonio-tasks-ce/issues/7)) ([f7c6913](https://github.com/Zextras/carbonio-tasks-ce/commit/f7c69138aa2a6f83f4b95919dc012dc0e1720f16))
* TSK-23 - Implement CreateTask API ([#9](https://github.com/Zextras/carbonio-tasks-ce/issues/9)) ([25c11be](https://github.com/Zextras/carbonio-tasks-ce/commit/25c11be0272f37757380da4da6bad248ef226ec7))
* TSK-27 - Implement GetTask API ([#10](https://github.com/Zextras/carbonio-tasks-ce/issues/10)) ([06eec01](https://github.com/Zextras/carbonio-tasks-ce/commit/06eec01d49efbc2e88171fbcbbda839d6f987d81))
* TSK-28 - Create tests for health and graphql endpoints ([#5](https://github.com/Zextras/carbonio-tasks-ce/issues/5)) ([faa47df](https://github.com/Zextras/carbonio-tasks-ce/commit/faa47dfa10b21b19f9ab833c7f7a44da150ef44a))
* TSK-29 - Add Config, ServiceDiscover and Database connection ([#6](https://github.com/Zextras/carbonio-tasks-ce/issues/6)) ([4d91b76](https://github.com/Zextras/carbonio-tasks-ce/commit/4d91b7616470b4ec03e7f6efb15d183c30ea1817))
* TSK-3 Add GraphQL provider and Implement FindTasks API ([#8](https://github.com/Zextras/carbonio-tasks-ce/issues/8)) ([817c6cb](https://github.com/Zextras/carbonio-tasks-ce/commit/817c6cb254d247b1a97a484dff153019073433cb))
* TSK-31 implement AuthenticationServletFilter to validate cookies ([#12](https://github.com/Zextras/carbonio-tasks-ce/issues/12)) ([ea00eb0](https://github.com/Zextras/carbonio-tasks-ce/commit/ea00eb07d57f2fa356617bec9cbffe10c21d2cc1))
* TSK-34 implement UpdateTask API ([#13](https://github.com/Zextras/carbonio-tasks-ce/issues/13)) ([af83313](https://github.com/Zextras/carbonio-tasks-ce/commit/af83313c893ffe853ce9a71ce4148732c7d5c89e))


### Bug Fixes

* findTasks should return only tasks in an open state ([#16](https://github.com/Zextras/carbonio-tasks-ce/issues/16)) ([2b06bff](https://github.com/Zextras/carbonio-tasks-ce/commit/2b06bff5f4120b42a57297997c3187f43bb651d2))
* TSK-32 - Fix database initialization and upstream IP in hcl ([#11](https://github.com/Zextras/carbonio-tasks-ce/issues/11)) ([3512587](https://github.com/Zextras/carbonio-tasks-ce/commit/3512587806d644f9ee1a9137e1c00bd006484c59))
* TSK-35 improve Health API + improve servlet creations ([#14](https://github.com/Zextras/carbonio-tasks-ce/issues/14)) ([e029811](https://github.com/Zextras/carbonio-tasks-ce/commit/e0298116eee20275c606f8eb7c7f43789cbb176d))
