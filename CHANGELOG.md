## [1.2.0](https://github.com/zextras/carbonio-tasks-ce/compare/v1.1.4...v1.2.0) (2026-06-30)

## [1.1.4](https://github.com/zextras/carbonio-tasks-ce/compare/v1.1.3...v1.1.4) (2026-06-22)

### Bug Fixes

* units: remove unneeded ReadOnlyPaths ([fd25898](https://github.com/zextras/carbonio-tasks-ce/commit/fd25898fc7296bf3cb764069b9e3d650c3c70fa4))

## [1.1.3](https://github.com/zextras/carbonio-tasks-ce/compare/v1.1.2...v1.1.3) (2026-06-21)

### Bug Fixes

* declare docs license centrally (header-less generated docs) ([#143](https://github.com/zextras/carbonio-tasks-ce/issues/143)) ([1aaacaf](https://github.com/zextras/carbonio-tasks-ce/commit/1aaacaf396cb1e7c752e2ff1f9e02541da5f118d))

## [1.1.2](https://github.com/zextras/carbonio-tasks-ce/compare/v1.1.1...v1.1.2) (2026-06-19)

### Bug Fixes

* project-owned REUSE.toml ([#141](https://github.com/zextras/carbonio-tasks-ce/issues/141)) ([baf7838](https://github.com/zextras/carbonio-tasks-ce/commit/baf7838d0959c93fa8e6f8b8c2c0629ad9db7756))
* **setup:** bump extensions to 1.9.1-1 and harden carbonio-tasks-setup ([#140](https://github.com/zextras/carbonio-tasks-ce/issues/140)) ([79aa533](https://github.com/zextras/carbonio-tasks-ce/commit/79aa5334ff17647e58ff4b459cb825a73cfda7ed))

## [1.1.1](https://github.com/zextras/carbonio-tasks-ce/compare/v1.1.0...v1.1.1) (2026-05-27)

### Bug Fixes

* **deps:** add explicit service-discover-base dependency ([#123](https://github.com/zextras/carbonio-tasks-ce/issues/123)) ([a503d7c](https://github.com/zextras/carbonio-tasks-ce/commit/a503d7cc1167c3b5cacbdfd8e5b8d5fa892029ff))

## [1.1.0](https://github.com/zextras/carbonio-tasks-ce/compare/v1.0.1...v1.1.0) (2026-05-04)

### Features

* migrate to gRPC UM SDK ([#105](https://github.com/zextras/carbonio-tasks-ce/issues/105)) ([8f3bdbc](https://github.com/zextras/carbonio-tasks-ce/commit/8f3bdbcacaf6b7514348bb3c06456aed2abb3ca7))
* migrate to quarkus ([#110](https://github.com/zextras/carbonio-tasks-ce/issues/110)) ([8ae03f9](https://github.com/zextras/carbonio-tasks-ce/commit/8ae03f9d2cf65821a3c36184670fa4f1b869ad62))
* systemd hardening and service-discover.target orchestration ([#108](https://github.com/zextras/carbonio-tasks-ce/issues/108)) ([28294c3](https://github.com/zextras/carbonio-tasks-ce/commit/28294c31500a9d49456f1547dc883ddf0fbe3193))

### Bug Fixes

* add @Name annotations to preserve GraphQL type names ([#111](https://github.com/zextras/carbonio-tasks-ce/issues/111)) ([916e23c](https://github.com/zextras/carbonio-tasks-ce/commit/916e23c84daa5d2fd877b15545fc86a06768d357))

<!--
SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>

SPDX-License-Identifier: AGPL-3.0-only
-->

## [1.0.1](https://github.com/zextras/carbonio-tasks-ce/compare/v1.0.0...v1.0.1) (2026-02-23)

### Bug Fixes

* **deps:** update dependency org.postgresql:postgresql to v42.7.7 [security] ([#84](https://github.com/zextras/carbonio-tasks-ce/issues/84)) ([4c1dbf6](https://github.com/zextras/carbonio-tasks-ce/commit/4c1dbf6c206eb91eb957a5eb3ebae6101a20bd74))

## [1.0.0](https://github.com/zextras/carbonio-tasks-ce/compare/v0.5.1...v1.0.0) (2025-11-14)

### ⚠ BREAKING CHANGES

* update release config and trigger first major bump (#76)

### Features

* build packages from docker ([#57](https://github.com/zextras/carbonio-tasks-ce/issues/57)) ([a9c4f9c](https://github.com/zextras/carbonio-tasks-ce/commit/a9c4f9c934df6789daf905e9cc94ac7cd3a2c093))

### Bug Fixes

* cache graphql config to avoid thread leak ([#64](https://github.com/zextras/carbonio-tasks-ce/issues/64)) ([34e1a21](https://github.com/zextras/carbonio-tasks-ce/commit/34e1a21788245580291be5b5c5e593d2a68cfca2))
* filter feature enabled flag for requester on auth ([#66](https://github.com/zextras/carbonio-tasks-ce/issues/66)) ([4a5b090](https://github.com/zextras/carbonio-tasks-ce/commit/4a5b0904f29e1359d6f846f4a67bfbaebf4172a3))
* host network on frontend docker ([#51](https://github.com/zextras/carbonio-tasks-ce/issues/51)) ([63d503b](https://github.com/zextras/carbonio-tasks-ce/commit/63d503be0b5188af2eda2fb93e512d2be2fcdd27))
* only allow internal users to call tasks ([#65](https://github.com/zextras/carbonio-tasks-ce/issues/65)) ([5da15be](https://github.com/zextras/carbonio-tasks-ce/commit/5da15beb192f33c6e32679da8c9ae40c79b52d8b))
* revert WantedBy for compatibility with older systems ([#58](https://github.com/zextras/carbonio-tasks-ce/issues/58)) ([50a6e08](https://github.com/zextras/carbonio-tasks-ce/commit/50a6e08a03ca5d9f39ecc221ff0d768e7296ef85))
* update release config and trigger first major bump ([#76](https://github.com/zextras/carbonio-tasks-ce/issues/76)) ([648b2f5](https://github.com/zextras/carbonio-tasks-ce/commit/648b2f5ef4fcb8235c5b0480ae70e60befd5cdcc))

## [0.5.1](https://github.com/zextras/carbonio-tasks-ce/compare/v0.5.0...v0.5.1) (2024-11-27)
## [0.5.0](https://github.com/zextras/carbonio-tasks-ce/compare/v0.4.1...v0.5.0) (2024-11-18)

### Features

* replace health checks from ready to live ([#44](https://github.com/zextras/carbonio-tasks-ce/issues/44)) ([378569f](https://github.com/zextras/carbonio-tasks-ce/commit/378569f48f2e5f06c1c3b63709b3908d0a1f21ec))
## [0.4.1](https://github.com/zextras/carbonio-tasks-ce/compare/v0.4.0...v0.4.1) (2024-08-27)

### Features

* add ubuntu 24.04 (ubuntu-noble) support ([#40](https://github.com/zextras/carbonio-tasks-ce/issues/40)) ([2fe7a86](https://github.com/zextras/carbonio-tasks-ce/commit/2fe7a869821f2c3744f5a4fc1d886ac82120536c))
## [0.4.0](https://github.com/zextras/carbonio-tasks-ce/compare/v0.3.0...v0.4.0) (2024-06-17)

### Features

* update tasks to use new user management sdk with returned user type ([#36](https://github.com/zextras/carbonio-tasks-ce/issues/36)) ([c342609](https://github.com/zextras/carbonio-tasks-ce/commit/c34260933431e04da90b3c3ada890672051868e4))
* use new user management sdk with returned user status ([#35](https://github.com/zextras/carbonio-tasks-ce/issues/35)) ([54c111e](https://github.com/zextras/carbonio-tasks-ce/commit/54c111e3ac57b6bd94c8ca7790ea9e540e80b4df))
## [0.3.0](https://github.com/zextras/carbonio-tasks-ce/compare/v0.2.1...v0.3.0) (2024-04-12)

### Features

* add Flyway to manage database migrations ([#32](https://github.com/zextras/carbonio-tasks-ce/issues/32)) ([a694542](https://github.com/zextras/carbonio-tasks-ce/commit/a69454232487807ff0699642911f725c52ffcbca))

### Bug Fixes

* *.hcl: apply corrections to validate with hclfmt ([#29](https://github.com/zextras/carbonio-tasks-ce/issues/29)) ([1215f93](https://github.com/zextras/carbonio-tasks-ce/commit/1215f935407bf7be18475ce87182efdef0beac1d))
* make the service re-throws runtime exception in the Boot class ([#31](https://github.com/zextras/carbonio-tasks-ce/issues/31)) ([7189173](https://github.com/zextras/carbonio-tasks-ce/commit/718917357fec8164f12eefbbd0c1b978cd951f69))
## [0.2.1](https://github.com/zextras/carbonio-tasks-ce/compare/v0.2.0...v0.2.1) (2024-01-16)

### Features

* move to yap agent and add rhel9 support ([#27](https://github.com/zextras/carbonio-tasks-ce/issues/27)) ([190c41d](https://github.com/zextras/carbonio-tasks-ce/commit/190c41d71e27737495ac843bd2bbd0d25f3d9418))
## [0.2.0](https://github.com/zextras/carbonio-tasks-ce/compare/v0.1.0...v0.2.0) (2023-10-27)

### Features

* implement TrashTask API to mark a task as trashed  ([#24](https://github.com/zextras/carbonio-tasks-ce/issues/24)) ([263d26e](https://github.com/zextras/carbonio-tasks-ce/commit/263d26ef0e7aacb9a0637a45e97650357588bc17)), closes [Status#TRASH](https://github.com/zextras/Status/issues/TRASH)
* update FindTasks to return even the completed tasks ([#23](https://github.com/zextras/carbonio-tasks-ce/issues/23)) ([f43d67c](https://github.com/zextras/carbonio-tasks-ce/commit/f43d67cd3cfedb3bf27925a9defc43c8837f1549)), closes [TaskRepositoryEbean#getTasks](https://github.com/zextras/TaskRepositoryEbean/issues/getTasks) [TaskRepositoryEbean#getTasks](https://github.com/zextras/TaskRepositoryEbean/issues/getTasks)
## [0.1.0](https://github.com/zextras/carbonio-tasks-ce/compare/v0.0.1...v0.1.0) (2023-05-30)

### Features

* reduce Hikari max pool size to 2 ([#19](https://github.com/zextras/carbonio-tasks-ce/issues/19)) ([c21c72d](https://github.com/zextras/carbonio-tasks-ce/commit/c21c72d8ffd18cf724f76cbd6ad793dd88337005))
## [0.0.1](https://github.com/zextras/carbonio-tasks-ce/compare/5b93cb953c097f7f45f74025b5f07ba64346dcc5...v0.0.1) (2023-04-27)

### Features

* TSK-12 - Setup the repository and an empty maven project ([#1](https://github.com/zextras/carbonio-tasks-ce/issues/1)) ([5b93cb9](https://github.com/zextras/carbonio-tasks-ce/commit/5b93cb953c097f7f45f74025b5f07ba64346dcc5))
* TSK-13 - Add GraphQL and RESTEasy servlet + health APIs ([#3](https://github.com/zextras/carbonio-tasks-ce/issues/3)) ([e7d2013](https://github.com/zextras/carbonio-tasks-ce/commit/e7d2013c8e7f33d305a959e2382db76fc928471b))
* TSK-14 - Create package and add consul configuration ([#2](https://github.com/zextras/carbonio-tasks-ce/issues/2)) ([19861f6](https://github.com/zextras/carbonio-tasks-ce/commit/19861f6c0da78b82392fc7852df1238ed0c4fb41))
* TSK-19 - Add Task entity and TaskRepository ([#7](https://github.com/zextras/carbonio-tasks-ce/issues/7)) ([f7c6913](https://github.com/zextras/carbonio-tasks-ce/commit/f7c69138aa2a6f83f4b95919dc012dc0e1720f16))
* TSK-23 - Implement CreateTask API ([#9](https://github.com/zextras/carbonio-tasks-ce/issues/9)) ([25c11be](https://github.com/zextras/carbonio-tasks-ce/commit/25c11be0272f37757380da4da6bad248ef226ec7))
* TSK-27 - Implement GetTask API ([#10](https://github.com/zextras/carbonio-tasks-ce/issues/10)) ([06eec01](https://github.com/zextras/carbonio-tasks-ce/commit/06eec01d49efbc2e88171fbcbbda839d6f987d81))
* TSK-28 - Create tests for health and graphql endpoints ([#5](https://github.com/zextras/carbonio-tasks-ce/issues/5)) ([faa47df](https://github.com/zextras/carbonio-tasks-ce/commit/faa47dfa10b21b19f9ab833c7f7a44da150ef44a))
* TSK-29 - Add Config, ServiceDiscover and Database connection ([#6](https://github.com/zextras/carbonio-tasks-ce/issues/6)) ([4d91b76](https://github.com/zextras/carbonio-tasks-ce/commit/4d91b7616470b4ec03e7f6efb15d183c30ea1817))
* TSK-3 Add GraphQL provider and Implement FindTasks API ([#8](https://github.com/zextras/carbonio-tasks-ce/issues/8)) ([817c6cb](https://github.com/zextras/carbonio-tasks-ce/commit/817c6cb254d247b1a97a484dff153019073433cb))
* TSK-31 implement AuthenticationServletFilter to validate cookies ([#12](https://github.com/zextras/carbonio-tasks-ce/issues/12)) ([ea00eb0](https://github.com/zextras/carbonio-tasks-ce/commit/ea00eb07d57f2fa356617bec9cbffe10c21d2cc1))
* TSK-34 implement UpdateTask API ([#13](https://github.com/zextras/carbonio-tasks-ce/issues/13)) ([af83313](https://github.com/zextras/carbonio-tasks-ce/commit/af83313c893ffe853ce9a71ce4148732c7d5c89e)), closes [TaskRepository#updateTask](https://github.com/zextras/TaskRepository/issues/updateTask) [TaskDataFetchers#updateTask](https://github.com/zextras/TaskDataFetchers/issues/updateTask)

### Bug Fixes

* findTasks should return only tasks in open an state ([#16](https://github.com/zextras/carbonio-tasks-ce/issues/16)) ([2b06bff](https://github.com/zextras/carbonio-tasks-ce/commit/2b06bff5f4120b42a57297997c3187f43bb651d2))
* TSK-32 - Fix database initialization and upstream IP in hcl ([#11](https://github.com/zextras/carbonio-tasks-ce/issues/11)) ([3512587](https://github.com/zextras/carbonio-tasks-ce/commit/3512587806d644f9ee1a9137e1c00bd006484c59))
* TSK-35 improve Health API + improve servlet creations ([#14](https://github.com/zextras/carbonio-tasks-ce/issues/14)) ([e029811](https://github.com/zextras/carbonio-tasks-ce/commit/e0298116eee20275c606f8eb7c7f43789cbb176d)), closes [TaskModule#getUserManagementClient](https://github.com/zextras/TaskModule/issues/getUserManagementClient)
