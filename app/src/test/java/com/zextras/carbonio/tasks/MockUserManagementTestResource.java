// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks;

import com.zextras.carbonio.user_management.sdk.grpc.GetUserMyselfRequest;
import com.zextras.carbonio.user_management.sdk.grpc.UserInfoProto;
import com.zextras.carbonio.user_management.sdk.grpc.UserManagementServiceGrpc;
import com.zextras.carbonio.user_management.sdk.grpc.UserMyselfProto;
import com.zextras.carbonio.user_management.sdk.grpc.UserMyselfResponse;
import com.zextras.carbonio.user_management.sdk.grpc.UserTypeProto;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Starts a mock gRPC server on a random TCP port that mimics carbonio-user-management for
 * integration tests. Configures the Quarkus {@code @GrpcClient("user-management")} to connect to
 * this server.
 *
 * <p>The pre-registered token {@code fake-user-cookie} maps to user-id
 * {@code 00000000-0000-0000-0000-000000000000}.
 */
public class MockUserManagementTestResource implements QuarkusTestResourceLifecycleManager {

  public static final String FAKE_TOKEN = "fake-user-cookie";
  public static final String FAKE_USER_ID = "00000000-0000-0000-0000-000000000000";

  private Server server;

  @Override
  public Map<String, String> start() {
    MockUserManagementService mockService = new MockUserManagementService();

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

    mockService.registerToken(FAKE_TOKEN, userMyself);

    try {
      server =
          ServerBuilder.forPort(0)
              .directExecutor()
              .addService(mockService)
              .build()
              .start();
    } catch (IOException e) {
      throw new RuntimeException("Failed to start mock UM gRPC server", e);
    }

    int port = server.getPort();

    // Override the Quarkus gRPC client config to point at our mock server
    return Map.of(
        "quarkus.grpc.clients.user-management.host", "localhost",
        "quarkus.grpc.clients.user-management.port", String.valueOf(port),
        "quarkus.grpc.clients.user-management.plain-text", "true");
  }

  @Override
  public void stop() {
    if (server != null) {
      server.shutdownNow();
    }
  }

  /** Mock gRPC UserManagementService for integration tests. */
  public static class MockUserManagementService
      extends UserManagementServiceGrpc.UserManagementServiceImplBase {

    private final Map<String, UserMyselfProto> tokenMap = new HashMap<>();

    public void registerToken(String token, UserMyselfProto user) {
      tokenMap.put(token, user);
    }

    @Override
    public void getUserMyself(
        GetUserMyselfRequest request, StreamObserver<UserMyselfResponse> responseObserver) {
      String token = request.getToken();
      UserMyselfProto user = tokenMap.get(token);
      if (user == null) {
        responseObserver.onError(
            Status.UNAUTHENTICATED.withDescription("Invalid token").asRuntimeException());
        return;
      }
      responseObserver.onNext(UserMyselfResponse.newBuilder().setUser(user).build());
      responseObserver.onCompleted();
    }
  }
}
