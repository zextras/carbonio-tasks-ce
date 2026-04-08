// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.auth;

import com.zextras.carbonio.tasks.clients.UserManagementClient;
import com.zextras.carbonio.user_management.sdk.grpc.GetUserMyselfRequest;
import com.zextras.carbonio.user_management.sdk.grpc.UserInfoProto;
import com.zextras.carbonio.user_management.sdk.grpc.UserManagementServiceGrpc.UserManagementServiceBlockingStub;
import com.zextras.carbonio.user_management.sdk.grpc.UserMyselfProto;
import com.zextras.carbonio.user_management.sdk.grpc.UserMyselfResponse;
import com.zextras.carbonio.user_management.sdk.grpc.UserTypeProto;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * Unit tests for {@link AuthenticationFilter}. No CDI container is started — all dependencies are
 * injected manually.
 */
class AuthenticationFilterTest {

  private UserManagementClient umClientMock;
  private UserManagementServiceBlockingStub stubMock;
  private AuthenticationFilter filter;

  @BeforeEach
  void setUp() throws Exception {
    umClientMock = Mockito.mock(UserManagementClient.class);
    stubMock = Mockito.mock(UserManagementServiceBlockingStub.class);

    Mockito.when(umClientMock.getBlockingStub()).thenReturn(stubMock);

    filter = new AuthenticationFilter();
    inject(filter, "userManagementClient", umClientMock);
  }

  private static void inject(Object target, String fieldName, Object value) throws Exception {
    var field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }

  private RoutingContext buildRoutingContext(Cookie cookie) {
    RoutingContext ctx = Mockito.mock(RoutingContext.class);
    HttpServerRequest request = Mockito.mock(HttpServerRequest.class);
    HttpServerResponse response = Mockito.mock(HttpServerResponse.class);

    Mockito.when(ctx.request()).thenReturn(request);
    Mockito.when(ctx.response()).thenReturn(response);
    Mockito.when(response.setStatusCode(Mockito.anyInt())).thenReturn(response);

    Mockito.when(request.getCookie("ZM_AUTH_TOKEN")).thenReturn(cookie);
    return ctx;
  }

  @Test
  void givenAValidCookieForAnActiveInternalUserWithTasksFeatureTheFilterShouldSetRequesterId() {
    Cookie zmCookie = Mockito.mock(Cookie.class);
    Mockito.when(zmCookie.getValue()).thenReturn("valid-token");

    UserInfoProto userInfo =
        UserInfoProto.newBuilder()
            .setUserId("00000000-0000-0000-0000-000000000000")
            .setType(UserTypeProto.INTERNAL)
            .setStatus("active")
            .build();

    UserMyselfProto userMyself =
        UserMyselfProto.newBuilder()
            .setInfo(userInfo)
            .addFeatures("carbonioFeatureTasksEnabled")
            .build();

    UserMyselfResponse grpcResponse = UserMyselfResponse.newBuilder().setUser(userMyself).build();

    GetUserMyselfRequest expectedRequest =
        GetUserMyselfRequest.newBuilder().setToken("valid-token").build();

    Mockito.when(stubMock.getUserMyself(expectedRequest)).thenReturn(grpcResponse);

    RoutingContext ctx = buildRoutingContext(zmCookie);

    filter.filter(ctx);

    ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Object> valueCaptor = ArgumentCaptor.forClass(Object.class);
    Mockito.verify(ctx).put(keyCaptor.capture(), valueCaptor.capture());
    Assertions.assertThat(valueCaptor.getValue()).isEqualTo("00000000-0000-0000-0000-000000000000");
    Mockito.verify(ctx).next();
    Mockito.verify(ctx.response(), Mockito.never()).setStatusCode(401);
  }

  @Test
  void givenMissingCookieTheFilterShouldRespondWith401() {
    RoutingContext ctx = buildRoutingContext(null);

    filter.filter(ctx);

    Mockito.verify(ctx.response()).setStatusCode(401);
    Mockito.verify(ctx.response()).end();
    Mockito.verify(ctx, Mockito.never()).next();
    Mockito.verifyNoInteractions(umClientMock);
  }

  @Test
  void givenAnInvalidTokenTheFilterShouldRespondWith401() {
    Cookie zmCookie = Mockito.mock(Cookie.class);
    Mockito.when(zmCookie.getValue()).thenReturn("invalid-token");

    GetUserMyselfRequest expectedRequest =
        GetUserMyselfRequest.newBuilder().setToken("invalid-token").build();

    Mockito.when(stubMock.getUserMyself(expectedRequest))
        .thenThrow(new StatusRuntimeException(Status.UNAUTHENTICATED));

    RoutingContext ctx = buildRoutingContext(zmCookie);

    filter.filter(ctx);

    Mockito.verify(ctx.response()).setStatusCode(401);
    Mockito.verify(ctx.response()).end();
    Mockito.verify(ctx, Mockito.never()).next();
  }

  @Test
  void givenAGuestUserTheFilterShouldRespondWith401() {
    Cookie zmCookie = Mockito.mock(Cookie.class);
    Mockito.when(zmCookie.getValue()).thenReturn("guest-token");

    UserInfoProto userInfo =
        UserInfoProto.newBuilder()
            .setUserId("guest-id")
            .setType(UserTypeProto.GUEST)
            .setStatus("active")
            .build();

    UserMyselfProto guestUser = UserMyselfProto.newBuilder().setInfo(userInfo).build();
    UserMyselfResponse grpcResponse = UserMyselfResponse.newBuilder().setUser(guestUser).build();

    GetUserMyselfRequest expectedRequest =
        GetUserMyselfRequest.newBuilder().setToken("guest-token").build();

    Mockito.when(stubMock.getUserMyself(expectedRequest)).thenReturn(grpcResponse);

    RoutingContext ctx = buildRoutingContext(zmCookie);

    filter.filter(ctx);

    Mockito.verify(ctx.response()).setStatusCode(401);
    Mockito.verify(ctx.response()).end();
    Mockito.verify(ctx, Mockito.never()).next();
  }

  @Test
  void givenAnInactiveUserTheFilterShouldRespondWith401() {
    Cookie zmCookie = Mockito.mock(Cookie.class);
    Mockito.when(zmCookie.getValue()).thenReturn("inactive-token");

    UserInfoProto userInfo =
        UserInfoProto.newBuilder()
            .setUserId("inactive-id")
            .setType(UserTypeProto.INTERNAL)
            .setStatus("locked")
            .build();

    UserMyselfProto userMyself = UserMyselfProto.newBuilder().setInfo(userInfo).build();
    UserMyselfResponse grpcResponse = UserMyselfResponse.newBuilder().setUser(userMyself).build();

    GetUserMyselfRequest expectedRequest =
        GetUserMyselfRequest.newBuilder().setToken("inactive-token").build();

    Mockito.when(stubMock.getUserMyself(expectedRequest)).thenReturn(grpcResponse);

    RoutingContext ctx = buildRoutingContext(zmCookie);

    filter.filter(ctx);

    Mockito.verify(ctx.response()).setStatusCode(401);
    Mockito.verify(ctx.response()).end();
    Mockito.verify(ctx, Mockito.never()).next();
  }

  @Test
  void givenAUserWithoutTasksFeatureTheFilterShouldRespondWith401() {
    Cookie zmCookie = Mockito.mock(Cookie.class);
    Mockito.when(zmCookie.getValue()).thenReturn("no-tasks-token");

    UserInfoProto userInfo =
        UserInfoProto.newBuilder()
            .setUserId("user-id")
            .setType(UserTypeProto.INTERNAL)
            .setStatus("active")
            .build();

    UserMyselfProto userMyself = UserMyselfProto.newBuilder().setInfo(userInfo).build();
    UserMyselfResponse grpcResponse = UserMyselfResponse.newBuilder().setUser(userMyself).build();

    GetUserMyselfRequest expectedRequest =
        GetUserMyselfRequest.newBuilder().setToken("no-tasks-token").build();

    Mockito.when(stubMock.getUserMyself(expectedRequest)).thenReturn(grpcResponse);

    RoutingContext ctx = buildRoutingContext(zmCookie);

    filter.filter(ctx);

    Mockito.verify(ctx.response()).setStatusCode(401);
    Mockito.verify(ctx.response()).end();
    Mockito.verify(ctx, Mockito.never()).next();
  }
}
