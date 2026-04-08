// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks;

import com.zextras.carbonio.user_management.sdk.grpc.GetUserMyselfRequest;
import com.zextras.carbonio.user_management.sdk.grpc.UserManagementServiceGrpc;
import com.zextras.carbonio.user_management.sdk.grpc.UserMyselfProto;
import com.zextras.carbonio.user_management.sdk.grpc.UserMyselfResponse;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * In-process gRPC service used by Quarkus @QuarkusTest infrastructure to handle
 * UserManagementService calls. Since Quarkus test mode redirects all gRPC clients to the
 * in-process server (port 9001), this service intercepts token validation requests and returns
 * pre-registered mock responses.
 *
 * <p>Token registrations are managed by {@link MockUserManagementTestResource} which populates
 * {@link #TOKEN_MAP} before test execution.
 */
@GrpcService
public class TestUserManagementService
    extends UserManagementServiceGrpc.UserManagementServiceImplBase {

  /**
   * Shared token registry. {@link MockUserManagementTestResource#start()} populates this before
   * any test runs.
   */
  public static final Map<String, UserMyselfProto> TOKEN_MAP = new ConcurrentHashMap<>();

  @Override
  public void getUserMyself(
      GetUserMyselfRequest request, StreamObserver<UserMyselfResponse> responseObserver) {
    String token = request.getToken();
    UserMyselfProto user = TOKEN_MAP.get(token);
    if (user == null) {
      responseObserver.onError(
          Status.UNAUTHENTICATED.withDescription("Invalid token: " + token).asRuntimeException());
      return;
    }
    responseObserver.onNext(UserMyselfResponse.newBuilder().setUser(user).build());
    responseObserver.onCompleted();
  }
}
