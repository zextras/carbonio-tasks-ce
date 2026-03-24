// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.auth;

import com.zextras.carbonio.user_management.sdk.grpc.GetUserMyselfRequest;
import com.zextras.carbonio.user_management.sdk.grpc.UserInfoProto;
import com.zextras.carbonio.user_management.sdk.grpc.UserManagementServiceGrpc.UserManagementServiceBlockingStub;
import com.zextras.carbonio.user_management.sdk.grpc.UserMyselfProto;
import com.zextras.carbonio.user_management.sdk.grpc.UserMyselfResponse;
import com.zextras.carbonio.user_management.sdk.grpc.UserTypeProto;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AuthenticationServletFilterTest {

  private UserManagementServiceBlockingStub userManagementStubMock;

  @BeforeEach
  void setUp() {
    userManagementStubMock = Mockito.mock(UserManagementServiceBlockingStub.class);
  }

  @Test
  void givenAFilterConfigTheInitShouldInitializeTheFilter() throws ServletException {
    FilterConfig filterConfigMock = Mockito.mock(FilterConfig.class);
    Mockito.when(filterConfigMock.getServletContext())
        .thenReturn(Mockito.mock(ServletContext.class));
    AuthenticationServletFilter authenticationServletFilter =
        new AuthenticationServletFilter(userManagementStubMock);

    authenticationServletFilter.init(filterConfigMock);

    Mockito.verify(filterConfigMock, Mockito.times(1)).getServletContext();
    Mockito.verifyNoInteractions(userManagementStubMock);
  }

  @Test
  void givenARequestWithAValidCookieTheDoFilterShouldAddTheRequesterIdInTheServletContext()
      throws ServletException, IOException {
    Cookie[] cookies = {
      new Cookie("ZM_AUTH_TOKEN", "zm-token"), new Cookie("ZX_AUTH_TOKEN", "zx-token")
    };
    HttpServletRequest httpRequestMock = Mockito.mock(HttpServletRequest.class);
    Mockito.when(httpRequestMock.getCookies()).thenReturn(cookies);

    HttpServletResponse httpResponseMock = Mockito.mock(HttpServletResponse.class);
    FilterChain filterChainMock = Mockito.mock(FilterChain.class);

    UserInfoProto userInfo = UserInfoProto.newBuilder()
        .setUserId("00000000-0000-0000-0000-000000000000")
        .setType(UserTypeProto.INTERNAL)
        .setStatus("active")
        .build();

    UserMyselfProto userMyself = UserMyselfProto.newBuilder()
        .setInfo(userInfo)
        .addFeatures("carbonioFeatureTasksEnabled")
        .build();

    UserMyselfResponse grpcResponse = UserMyselfResponse.newBuilder()
        .setUser(userMyself)
        .build();

    GetUserMyselfRequest expectedRequest =
        GetUserMyselfRequest.newBuilder().setToken("zm-token").build();

    Mockito.when(userManagementStubMock.getUserMyself(expectedRequest))
        .thenReturn(grpcResponse);

    AuthenticationServletFilter authenticationServletFilter =
        new AuthenticationServletFilter(userManagementStubMock);

    authenticationServletFilter.doFilter(httpRequestMock, httpResponseMock, filterChainMock);

    Mockito.verify(httpRequestMock, Mockito.times(1)).getCookies();

    Mockito.verify(userManagementStubMock, Mockito.times(1)).getUserMyself(expectedRequest);

    Mockito.verify(httpRequestMock, Mockito.times(1))
        .setAttribute("requesterId", "00000000-0000-0000-0000-000000000000");

    Mockito.verify(filterChainMock, Mockito.times(1)).doFilter(httpRequestMock, httpResponseMock);
  }

  @Test
  void givenANotHttpRequestTheDoFilterShouldBlockTheRequest() throws ServletException, IOException {
    ServletRequest httpRequestMock = Mockito.mock(ServletRequest.class);
    ServletResponse httpResponseMock = Mockito.mock(ServletResponse.class);
    FilterChain filterChainMock = Mockito.mock(FilterChain.class);

    AuthenticationServletFilter authenticationServletFilter =
        new AuthenticationServletFilter(userManagementStubMock);

    authenticationServletFilter.doFilter(httpRequestMock, httpResponseMock, filterChainMock);

    Mockito.verifyNoInteractions(userManagementStubMock);
    Mockito.verifyNoInteractions(httpRequestMock);
    Mockito.verifyNoInteractions(httpResponseMock);
    Mockito.verifyNoInteractions(filterChainMock);
  }

  @Test
  void givenARequestWithAnUnsupportedCookieTypeTheDoFilterShouldRespondWithA401StatusCode()
      throws ServletException, IOException {
    Cookie[] cookies = {new Cookie("UNSUPPORTED_COOKIE_TYPE", "zm-token")};
    HttpServletRequest httpRequestMock = Mockito.mock(HttpServletRequest.class);
    Mockito.when(httpRequestMock.getCookies()).thenReturn(cookies);
    HttpServletResponse httpResponseMock = Mockito.mock(HttpServletResponse.class);
    FilterChain filterChainMock = Mockito.mock(FilterChain.class);

    AuthenticationServletFilter authenticationServletFilter =
        new AuthenticationServletFilter(userManagementStubMock);

    authenticationServletFilter.doFilter(httpRequestMock, httpResponseMock, filterChainMock);

    Mockito.verify(httpRequestMock, Mockito.times(1)).getCookies();
    Mockito.verify(httpResponseMock, Mockito.times(1))
        .setStatus(HttpServletResponse.SC_UNAUTHORIZED);

    Mockito.verifyNoInteractions(userManagementStubMock);
    Mockito.verify(httpRequestMock, Mockito.never())
        .setAttribute(Mockito.anyString(), Mockito.anyString());
    Mockito.verifyNoInteractions(filterChainMock);
  }

  @Test
  void givenARequestWithAnInvalidCookieTheDoFilterShouldRespondWithA401StatusCode()
      throws ServletException, IOException {
    Cookie[] cookies = {new Cookie("ZM_AUTH_TOKEN", "invalid-token")};
    HttpServletRequest httpRequestMock = Mockito.mock(HttpServletRequest.class);
    Mockito.when(httpRequestMock.getCookies()).thenReturn(cookies);
    HttpServletResponse httpResponseMock = Mockito.mock(HttpServletResponse.class);
    FilterChain filterChainMock = Mockito.mock(FilterChain.class);

    GetUserMyselfRequest expectedRequest =
        GetUserMyselfRequest.newBuilder().setToken("invalid-token").build();

    Mockito.when(userManagementStubMock.getUserMyself(expectedRequest))
        .thenThrow(new StatusRuntimeException(Status.UNAUTHENTICATED));

    AuthenticationServletFilter authenticationServletFilter =
        new AuthenticationServletFilter(userManagementStubMock);

    authenticationServletFilter.doFilter(httpRequestMock, httpResponseMock, filterChainMock);

    Mockito.verify(httpRequestMock, Mockito.times(1)).getCookies();

    Mockito.verify(userManagementStubMock, Mockito.times(1)).getUserMyself(expectedRequest);

    Mockito.verify(httpResponseMock, Mockito.times(1))
        .setStatus(HttpServletResponse.SC_UNAUTHORIZED);

    Mockito.verify(httpRequestMock, Mockito.never())
        .setAttribute(Mockito.anyString(), Mockito.anyString());
    Mockito.verifyNoInteractions(filterChainMock);
  }

  @Test
  void givenARequestFromAGuestUserTheDoFilterShouldRespondWithA401StatusCode()
      throws ServletException, IOException {
    Cookie[] cookies = {new Cookie("ZM_AUTH_TOKEN", "guest-token")};
    HttpServletRequest httpRequestMock = Mockito.mock(HttpServletRequest.class);
    Mockito.when(httpRequestMock.getCookies()).thenReturn(cookies);
    HttpServletResponse httpResponseMock = Mockito.mock(HttpServletResponse.class);
    FilterChain filterChainMock = Mockito.mock(FilterChain.class);

    UserInfoProto userInfo = UserInfoProto.newBuilder()
        .setUserId("guest-user-id")
        .setType(UserTypeProto.GUEST)
        .setStatus("active")
        .build();

    UserMyselfProto guestUser = UserMyselfProto.newBuilder()
        .setInfo(userInfo)
        .build();

    UserMyselfResponse grpcResponse = UserMyselfResponse.newBuilder()
        .setUser(guestUser)
        .build();

    GetUserMyselfRequest expectedRequest =
        GetUserMyselfRequest.newBuilder().setToken("guest-token").build();

    Mockito.when(userManagementStubMock.getUserMyself(expectedRequest))
        .thenReturn(grpcResponse);

    AuthenticationServletFilter authenticationServletFilter =
        new AuthenticationServletFilter(userManagementStubMock);

    authenticationServletFilter.doFilter(httpRequestMock, httpResponseMock, filterChainMock);

    Mockito.verify(httpRequestMock, Mockito.times(1)).getCookies();

    Mockito.verify(userManagementStubMock, Mockito.times(1)).getUserMyself(expectedRequest);

    Mockito.verify(httpResponseMock, Mockito.times(1))
        .setStatus(HttpServletResponse.SC_UNAUTHORIZED);

    Mockito.verify(httpRequestMock, Mockito.never())
        .setAttribute(Mockito.anyString(), Mockito.anyString());
    Mockito.verifyNoInteractions(filterChainMock);
  }
}
