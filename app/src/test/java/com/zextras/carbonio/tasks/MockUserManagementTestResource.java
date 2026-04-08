// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks;

import com.zextras.carbonio.user_management.sdk.grpc.UserInfoProto;
import com.zextras.carbonio.user_management.sdk.grpc.UserMyselfProto;
import com.zextras.carbonio.user_management.sdk.grpc.UserTypeProto;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.util.Map;

/**
 * Populates the in-process {@link TestUserManagementService} token registry for integration tests.
 *
 * <p>Since Quarkus {@code @QuarkusTest} redirects all {@code @GrpcClient} channels to the
 * in-process gRPC server, the actual mock lives in {@link TestUserManagementService} which is
 * registered as a {@code @GrpcService}. This resource only registers token→user mappings in the
 * shared {@link TestUserManagementService#TOKEN_MAP} before any test runs.
 *
 * <p>The pre-registered token {@link #FAKE_TOKEN} maps to user-id {@link #FAKE_USER_ID}.
 */
public class MockUserManagementTestResource implements QuarkusTestResourceLifecycleManager {

  public static final String FAKE_TOKEN = "fake-user-cookie";
  public static final String FAKE_USER_ID = "00000000-0000-0000-0000-000000000000";

  @Override
  public Map<String, String> start() {
    UserInfoProto userInfo =
        UserInfoProto.newBuilder()
            .setUserId(FAKE_USER_ID)
            .setEmail("fake@example.com")
            .setFullName("Fake User")
            .setDomain("example.com")
            .setStatus("active")
            .setType(UserTypeProto.INTERNAL)
            .build();

    UserMyselfProto userMyself =
        UserMyselfProto.newBuilder()
            .setInfo(userInfo)
            .setLocale("en")
            .addFeatures("carbonioFeatureTasksEnabled")
            .build();

    TestUserManagementService.TOKEN_MAP.put(FAKE_TOKEN, userMyself);

    // No config overrides needed: Quarkus test mode already routes @GrpcClient calls to the
    // in-process server where TestUserManagementService is registered.
    return Map.of();
  }

  @Override
  public void stop() {
    TestUserManagementService.TOKEN_MAP.clear();
  }
}
