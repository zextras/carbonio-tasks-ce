// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.auth;

import com.zextras.carbonio.tasks.clients.UserManagementClient;
import com.zextras.carbonio.tasks.graphql.RequestContext;
import com.zextras.carbonio.user_management.sdk.grpc.GetUserMyselfRequest;
import com.zextras.carbonio.user_management.sdk.grpc.UserInfoProto;
import com.zextras.carbonio.user_management.sdk.grpc.UserManagementServiceGrpc.UserManagementServiceBlockingStub;
import com.zextras.carbonio.user_management.sdk.grpc.UserMyselfProto;
import com.zextras.carbonio.user_management.sdk.grpc.UserMyselfResponse;
import com.zextras.carbonio.user_management.sdk.grpc.UserTypeProto;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Map;
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
  private RequestContext requestContext;
  private AuthenticationFilter filter;

  @BeforeEach
  void setUp() throws Exception {
    umClientMock = Mockito.mock(UserManagementClient.class);
    stubMock = Mockito.mock(UserManagementServiceBlockingStub.class);
    requestContext = new RequestContext();

    Mockito.when(umClientMock.getBlockingStub()).thenReturn(stubMock);

    filter = new AuthenticationFilter();
    inject(filter, "userManagementClient", umClientMock);
    inject(filter, "requestContext", requestContext);
  }

  private static void inject(Object target, String fieldName, Object value) throws Exception {
    var field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }

  private ContainerRequestContext buildRequestContext(String path, Map<String, Cookie> cookies) {
    ContainerRequestContext ctx = Mockito.mock(ContainerRequestContext.class);
    UriInfo uriInfo = Mockito.mock(UriInfo.class);
    Mockito.when(uriInfo.getPath()).thenReturn(path);
    Mockito.when(ctx.getUriInfo()).thenReturn(uriInfo);
    Mockito.when(ctx.getCookies()).thenReturn(cookies);
    return ctx;
  }

  @Test
  void givenAHealthRequestTheFilterShouldSkipAuthentication() {
    ContainerRequestContext ctx = buildRequestContext("rest/health/live", Map.of());

    filter.filter(ctx);

    Mockito.verify(ctx, Mockito.never()).abortWith(Mockito.any());
    Mockito.verifyNoInteractions(umClientMock);
  }

  @Test
  void givenAValidCookieForAnActiveInternalUserWithTasksFeatureTheFilterShouldSetRequesterId() {
    Cookie zmCookie = new Cookie("ZM_AUTH_TOKEN", "valid-token");

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

    ContainerRequestContext ctx =
        buildRequestContext("graphql", Map.of("ZM_AUTH_TOKEN", zmCookie));

    filter.filter(ctx);

    Assertions.assertThat(requestContext.getRequesterId())
        .isEqualTo("00000000-0000-0000-0000-000000000000");
    Mockito.verify(ctx, Mockito.never()).abortWith(Mockito.any());
  }

  @Test
  void givenMissingCookieTheFilterShouldAbortWith401() {
    ContainerRequestContext ctx = buildRequestContext("graphql", Map.of());

    filter.filter(ctx);

    ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
    Mockito.verify(ctx).abortWith(captor.capture());
    Assertions.assertThat(captor.getValue().getStatus()).isEqualTo(401);
    Mockito.verifyNoInteractions(umClientMock);
  }

  @Test
  void givenAnInvalidTokenTheFilterShouldAbortWith401() {
    Cookie zmCookie = new Cookie("ZM_AUTH_TOKEN", "invalid-token");

    GetUserMyselfRequest expectedRequest =
        GetUserMyselfRequest.newBuilder().setToken("invalid-token").build();

    Mockito.when(stubMock.getUserMyself(expectedRequest))
        .thenThrow(new StatusRuntimeException(Status.UNAUTHENTICATED));

    ContainerRequestContext ctx =
        buildRequestContext("graphql", Map.of("ZM_AUTH_TOKEN", zmCookie));

    filter.filter(ctx);

    ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
    Mockito.verify(ctx).abortWith(captor.capture());
    Assertions.assertThat(captor.getValue().getStatus()).isEqualTo(401);
  }

  @Test
  void givenAGuestUserTheFilterShouldAbortWith401() {
    Cookie zmCookie = new Cookie("ZM_AUTH_TOKEN", "guest-token");

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

    ContainerRequestContext ctx =
        buildRequestContext("graphql", Map.of("ZM_AUTH_TOKEN", zmCookie));

    filter.filter(ctx);

    ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
    Mockito.verify(ctx).abortWith(captor.capture());
    Assertions.assertThat(captor.getValue().getStatus()).isEqualTo(401);
  }

  @Test
  void givenAnInactiveUserTheFilterShouldAbortWith401() {
    Cookie zmCookie = new Cookie("ZM_AUTH_TOKEN", "inactive-token");

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

    ContainerRequestContext ctx =
        buildRequestContext("graphql", Map.of("ZM_AUTH_TOKEN", zmCookie));

    filter.filter(ctx);

    ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
    Mockito.verify(ctx).abortWith(captor.capture());
    Assertions.assertThat(captor.getValue().getStatus()).isEqualTo(401);
  }
}
