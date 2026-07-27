// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.clients;

import com.zextras.carbonio.user_management.sdk.rest.model.MyselfDto;
import com.zextras.carbonio.user_management.sdk.rest.model.UserInfoDto;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Registers the carbonio-user-management-rest-sdk response DTOs for reflection in the native
 * image.
 *
 * <p>{@link com.zextras.carbonio.tasks.auth.AuthenticationFilter} deserializes
 * carbonio-user-management's {@code /internal/users/myself} response into {@link MyselfDto}
 * (which nests {@link UserInfoDto}) via Jackson. The SDK jar ships no native-image metadata of
 * its own (no {@code reflect-config.json}), and Quarkus's {@code JacksonProcessor} only
 * auto-registers a class for reflection when it carries one of a fixed set of Jackson
 * annotations ({@code @JsonDeserialize}, {@code @JsonSerialize}, {@code @JsonNaming}, {@code
 * @JsonAutoDetect}, {@code @JsonCreator}, {@code @JsonSubTypes}, {@code @JsonTypeIdResolver},
 * {@code @JsonIdentityInfo}). Both generated DTOs only carry {@code @JsonProperty}, {@code
 * @JsonPropertyOrder} and {@code @JsonInclude}, none of which trigger that auto-registration.
 * Jandex indexing these classes (e.g. via {@code quarkus.index-dependency}) does not help either
 * - indexing is not reflection registration.
 *
 * <p>Without this class, Jackson cannot construct these DTOs under GraalVM: {@code readValue}
 * throws {@code InvalidDefinitionException} (an {@code IOException}), the generated client wraps
 * it as {@code throw new ApiException(e)} (the {@code Throwable}-only constructor, which never
 * sets {@code code}, so {@code getCode()} returns {@code 0}), and {@link
 * com.zextras.carbonio.tasks.auth.AuthenticationFilter}'s catch block rejects the request with
 * 401 - even though carbonio-user-management returned a valid 200.
 *
 * <p>carbonio-tasks-ce builds and ships its own native binary (see this repo's {@code
 * Jenkinsfile} {@code nativeBuild}, {@code package/PKGBUILD} and {@code docker/Dockerfile}), so -
 * unlike the Advanced twin of this class - the registration cannot be deferred to another module
 * that happens to run the native build: it has to live here.
 *
 * <p>{@link com.zextras.carbonio.user_management.sdk.rest.model.AbstractOpenApiSchema}, the
 * generator's {@code oneOf}/{@code anyOf} base class, is deliberately NOT registered here: the
 * {@code /internal} API exposes no {@code oneOf}/{@code anyOf} schema, no generated model extends
 * it, and nothing ever deserializes into it, so registering it would be dead configuration.
 */
@RegisterForReflection(targets = {MyselfDto.class, UserInfoDto.class})
public final class UserManagementSdkReflectionConfig {

  private UserManagementSdkReflectionConfig() {}
}
