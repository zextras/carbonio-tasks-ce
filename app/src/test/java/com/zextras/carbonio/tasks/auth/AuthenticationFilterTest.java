// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.auth;

import com.zextras.carbonio.user_management.sdk.rest.ApiException;
import com.zextras.carbonio.user_management.sdk.rest.api.UserResourceApi;
import com.zextras.carbonio.user_management.sdk.rest.model.MyselfDto;
import com.zextras.carbonio.user_management.sdk.rest.model.UserInfoDto;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import java.util.List;
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

  private UserResourceApi userResourceApiMock;
  private AuthenticationFilter filter;

  @BeforeEach
  void setUp() throws Exception {
    userResourceApiMock = Mockito.mock(UserResourceApi.class);

    filter = new AuthenticationFilter();
    inject(filter, "userResourceApi", userResourceApiMock);
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
  void givenAValidCookieForAnActiveInternalUserWithTasksFeatureTheFilterShouldSetRequesterId()
      throws Exception {
    Cookie zmCookie = Mockito.mock(Cookie.class);
    Mockito.when(zmCookie.getValue()).thenReturn("valid-token");

    UserInfoDto userInfo =
        new UserInfoDto()
            .userId("00000000-0000-0000-0000-000000000000")
            .type("INTERNAL")
            .status("active");

    MyselfDto userMyself =
        new MyselfDto().info(userInfo).features(List.of("carbonioFeatureTasksEnabled"));

    Mockito.when(userResourceApiMock.internalUsersMyselfGet(null, "valid-token"))
        .thenReturn(userMyself);

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
    Mockito.verifyNoInteractions(userResourceApiMock);
  }

  @Test
  void givenAnInvalidTokenTheFilterShouldRespondWith401() throws Exception {
    Cookie zmCookie = Mockito.mock(Cookie.class);
    Mockito.when(zmCookie.getValue()).thenReturn("invalid-token");

    Mockito.when(userResourceApiMock.internalUsersMyselfGet(null, "invalid-token"))
        .thenThrow(new ApiException(401, "Unauthorized"));

    RoutingContext ctx = buildRoutingContext(zmCookie);

    filter.filter(ctx);

    Mockito.verify(ctx.response()).setStatusCode(401);
    Mockito.verify(ctx.response()).end();
    Mockito.verify(ctx, Mockito.never()).next();
  }

  @Test
  void givenAUserManagementServiceCallFailureWithNoHttpResponseTheFilterShouldRespondWith503()
      throws Exception {
    Cookie zmCookie = Mockito.mock(Cookie.class);
    Mockito.when(zmCookie.getValue()).thenReturn("some-token");

    // The generated client's Throwable-only ApiException constructor never sets `code`, so
    // getCode() returns 0. This is what happens on network failure, connection refused, request
    // timeout, and - in the native binary - a Jackson InvalidDefinitionException caused by
    // missing reflection metadata for the SDK's response DTOs.
    Mockito.when(userResourceApiMock.internalUsersMyselfGet(null, "some-token"))
        .thenThrow(new ApiException(new java.net.http.HttpTimeoutException("x")));

    RoutingContext ctx = buildRoutingContext(zmCookie);

    filter.filter(ctx);

    Mockito.verify(ctx.response()).setStatusCode(503);
    Mockito.verify(ctx.response()).end();
    Mockito.verify(ctx, Mockito.never()).next();
  }

  @Test
  void givenAGuestUserTheFilterShouldRespondWith403() throws Exception {
    Cookie zmCookie = Mockito.mock(Cookie.class);
    Mockito.when(zmCookie.getValue()).thenReturn("guest-token");

    UserInfoDto userInfo = new UserInfoDto().userId("guest-id").type("GUEST").status("active");

    MyselfDto guestUser = new MyselfDto().info(userInfo).features(List.of());

    Mockito.when(userResourceApiMock.internalUsersMyselfGet(null, "guest-token"))
        .thenReturn(guestUser);

    RoutingContext ctx = buildRoutingContext(zmCookie);

    filter.filter(ctx);

    Mockito.verify(ctx.response()).setStatusCode(403);
    Mockito.verify(ctx.response()).end();
    Mockito.verify(ctx, Mockito.never()).next();
  }

  @Test
  void givenAnInactiveUserTheFilterShouldRespondWith403() throws Exception {
    Cookie zmCookie = Mockito.mock(Cookie.class);
    Mockito.when(zmCookie.getValue()).thenReturn("inactive-token");

    UserInfoDto userInfo =
        new UserInfoDto().userId("inactive-id").type("INTERNAL").status("locked");

    MyselfDto userMyself = new MyselfDto().info(userInfo).features(List.of());

    Mockito.when(userResourceApiMock.internalUsersMyselfGet(null, "inactive-token"))
        .thenReturn(userMyself);

    RoutingContext ctx = buildRoutingContext(zmCookie);

    filter.filter(ctx);

    Mockito.verify(ctx.response()).setStatusCode(403);
    Mockito.verify(ctx.response()).end();
    Mockito.verify(ctx, Mockito.never()).next();
  }

  @Test
  void givenAUserWithoutTasksFeatureTheFilterShouldRespondWith403() throws Exception {
    Cookie zmCookie = Mockito.mock(Cookie.class);
    Mockito.when(zmCookie.getValue()).thenReturn("no-tasks-token");

    UserInfoDto userInfo = new UserInfoDto().userId("user-id").type("INTERNAL").status("active");

    MyselfDto userMyself = new MyselfDto().info(userInfo).features(List.of());

    Mockito.when(userResourceApiMock.internalUsersMyselfGet(null, "no-tasks-token"))
        .thenReturn(userMyself);

    RoutingContext ctx = buildRoutingContext(zmCookie);

    filter.filter(ctx);

    Mockito.verify(ctx.response()).setStatusCode(403);
    Mockito.verify(ctx.response()).end();
    Mockito.verify(ctx, Mockito.never()).next();
  }
}
