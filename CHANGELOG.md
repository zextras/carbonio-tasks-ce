<!--
SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>

SPDX-License-Identifier: AGPL-3.0-only
-->

# Changelog

All notable changes to this project will be documented in this file. See [standard-version](https://github.com/conventional-changelog/standard-version) for commit guidelines.

## [0.5.0](https://github.com/Zextras/carbonio-tasks-ce/compare/v0.4.1...v0.5.0) (2024-11-17)


### Features

* replace health checks from ready to live ([#44](https://github.com/Zextras/carbonio-tasks-ce/issues/44)) ([378569f](https://github.com/Zextras/carbonio-tasks-ce/commit/378569f48f2e5f06c1c3b63709b3908d0a1f21ec))

### [0.4.1](https://github.com/Zextras/carbonio-tasks-ce/compare/v0.4.0...v0.4.1) (2024-08-27)


### Features

* add ubuntu 24.04 (ubuntu-noble) support ([#40](https://github.com/Zextras/carbonio-tasks-ce/issues/40)) ([2fe7a86](https://github.com/Zextras/carbonio-tasks-ce/commit/2fe7a869821f2c3744f5a4fc1d886ac82120536c))

## [0.4.0](https://github.com/Zextras/carbonio-tasks-ce/compare/v0.3.0...v0.4.0) (2024-06-17)


### Features

* update tasks to use new user management sdk with returned user type ([#36](https://github.com/Zextras/carbonio-tasks-ce/issues/36)) ([c342609](https://github.com/Zextras/carbonio-tasks-ce/commit/c34260933431e04da90b3c3ada890672051868e4))
* use new user management sdk with returned user status ([#35](https://github.com/Zextras/carbonio-tasks-ce/issues/35)) ([54c111e](https://github.com/Zextras/carbonio-tasks-ce/commit/54c111e3ac57b6bd94c8ca7790ea9e540e80b4df))

## [0.3.0](https://github.com/Zextras/carbonio-tasks-ce/compare/v0.2.1...v0.3.0) (2024-04-12)


### Features

* add Flyway to manage database migrations ([#32](https://github.com/Zextras/carbonio-tasks-ce/issues/32)) ([a694542](https://github.com/Zextras/carbonio-tasks-ce/commit/a69454232487807ff0699642911f725c52ffcbca))


### Bug Fixes

* *.hcl: apply corrections to validate with hclfmt ([#29](https://github.com/Zextras/carbonio-tasks-ce/issues/29)) ([1215f93](https://github.com/Zextras/carbonio-tasks-ce/commit/1215f935407bf7be18475ce87182efdef0beac1d))
* make the service re-throws runtime exception in the Boot class ([#31](https://github.com/Zextras/carbonio-tasks-ce/issues/31)) ([7189173](https://github.com/Zextras/carbonio-tasks-ce/commit/718917357fec8164f12eefbbd0c1b978cd951f69))

### [0.2.1](https://github.com/Zextras/carbonio-tasks-ce/compare/v0.2.0...v0.2.1) (2024-01-16)

### Features

* move to yap agent and add rhel9 support ([#27](https://github.com/Zextras/carbonio-tasks-ce/issues/27)) ([190c41d](https://github.com/Zextras/carbonio-tasks-ce/commit/190c41d71e27737495ac843bd2bbd0d25f3d9418))

### [0.2.0](https://github.com/Zextras/carbonio-tasks-ce/compare/v0.2.0...v0.1.0) (2023-10-27)

### Features

* implement TrashTask API to mark a task as trashed  ([#24](https://github.com/Zextras/carbonio-tasks-ce/issues/24)) ([263d26e](https://github.com/Zextras/carbonio-tasks-ce/commit/263d26ef0e7aacb9a0637a45e97650357588bc17))
* update FindTasks to return even the completed tasks ([#23](https://github.com/Zextras/carbonio-tasks-ce/issues/23)) ([f43d67c](https://github.com/Zextras/carbonio-tasks-ce/commit/f43d67cd3cfedb3bf27925a9defc43c8837f1549))

## [0.1.0](https://github.com/Zextras/carbonio-tasks-ce/compare/v0.0.1...v0.1.0) (2023-05-30)

### Features

* reduce Hikari max pool size to 2 ([#19](https://github.com/Zextras/carbonio-tasks-ce/issues/19)) ([c21c72d](https://github.com/Zextras/carbonio-tasks-ce/commit/c21c72d8ffd18cf724f76cbd6ad793dd88337005))

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
