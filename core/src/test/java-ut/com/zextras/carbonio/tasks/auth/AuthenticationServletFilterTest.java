// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.auth;

import com.zextras.carbonio.usermanagement.UserManagementClient;
import com.zextras.carbonio.usermanagement.entities.UserId;
import com.zextras.carbonio.usermanagement.exceptions.UnAuthorized;
import io.vavr.control.Try;
import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AuthenticationServletFilterTest {

  private UserManagementClient userManagementClientMock;

  @BeforeEach
  void setUp() {
    userManagementClientMock = Mockito.mock(UserManagementClient.class);
  }

  @Test
  void givenAFilterConfigTheInitShouldInitializeTheFilter() throws ServletException {
    // Given
    FilterConfig filterConfigMock = Mockito.mock(FilterConfig.class);
    Mockito.when(filterConfigMock.getServletContext())
        .thenReturn(Mockito.mock(ServletContext.class));
    AuthenticationServletFilter authenticationServletFilter =
        new AuthenticationServletFilter(userManagementClientMock);

    // When
    authenticationServletFilter.init(filterConfigMock);

    // Then
    Mockito.verify(filterConfigMock, Mockito.times(1)).getServletContext();
    Mockito.verifyNoInteractions(userManagementClientMock);
  }

  @Test
  void givenARequestWithAValidCookieTheDoFilterShouldAddTheRequesterIdInTheServletContext()
      throws ServletException, IOException {
    // Given
    Cookie[] cookies = {
      new Cookie("ZM_AUTH_TOKEN", "zm-token"), new Cookie("ZX_AUTH_TOKEN", "zx-token")
    };
    HttpServletRequest httpRequestMock = Mockito.mock(HttpServletRequest.class);
    Mockito.when(httpRequestMock.getCookies()).thenReturn(cookies);

    HttpServletResponse httpResponseMock = Mockito.mock(HttpServletResponse.class);
    FilterChain filterChainMock = Mockito.mock(FilterChain.class);

    UserId userId = new UserId();
    userId.setUserId("00000000-0000-0000-0000-000000000000");
    Mockito.when(userManagementClientMock.validateUserToken("zm-token"))
        .thenReturn(Try.success(userId));

    AuthenticationServletFilter authenticationServletFilter =
        new AuthenticationServletFilter(userManagementClientMock);

    // When
    authenticationServletFilter.doFilter(httpRequestMock, httpResponseMock, filterChainMock);

    // Then
    Mockito.verify(httpRequestMock, Mockito.times(1)).getCookies();

    Mockito.verify(userManagementClientMock, Mockito.times(1)).validateUserToken("zm-token");

    Mockito.verify(httpRequestMock, Mockito.times(1))
        .setAttribute("requesterId", "00000000-0000-0000-0000-000000000000");

    Mockito.verify(filterChainMock, Mockito.times(1)).doFilter(httpRequestMock, httpResponseMock);
  }

  @Test
  void givenANotHttpRequestTheDoFilterShouldBlockTheRequest() throws ServletException, IOException {
    // Given
    ServletRequest httpRequestMock = Mockito.mock(ServletRequest.class);
    ServletResponse httpResponseMock = Mockito.mock(ServletResponse.class);
    FilterChain filterChainMock = Mockito.mock(FilterChain.class);

    AuthenticationServletFilter authenticationServletFilter =
        new AuthenticationServletFilter(userManagementClientMock);

    // When
    authenticationServletFilter.doFilter(httpRequestMock, httpResponseMock, filterChainMock);

    // Then
    Mockito.verifyNoInteractions(userManagementClientMock);
    Mockito.verifyNoInteractions(httpRequestMock);
    Mockito.verifyNoInteractions(httpResponseMock);
    Mockito.verifyNoInteractions(filterChainMock);
  }

  @Test
  void givenARequestWithAnUnsupportedCookieTypeTheDoFilterShouldRespondWithA401StatusCode()
      throws ServletException, IOException {
    // Given
    Cookie[] cookies = {new Cookie("UNSUPPORTED_COOKIE_TYPE", "zm-token")};
    HttpServletRequest httpRequestMock = Mockito.mock(HttpServletRequest.class);
    Mockito.when(httpRequestMock.getCookies()).thenReturn(cookies);
    HttpServletResponse httpResponseMock = Mockito.mock(HttpServletResponse.class);
    FilterChain filterChainMock = Mockito.mock(FilterChain.class);

    AuthenticationServletFilter authenticationServletFilter =
        new AuthenticationServletFilter(userManagementClientMock);

    // When
    authenticationServletFilter.doFilter(httpRequestMock, httpResponseMock, filterChainMock);

    // Then
    Mockito.verify(httpRequestMock, Mockito.times(1)).getCookies();
    Mockito.verify(httpResponseMock, Mockito.times(1))
        .setStatus(HttpServletResponse.SC_UNAUTHORIZED);

    Mockito.verifyNoInteractions(userManagementClientMock);
    Mockito.verify(httpRequestMock, Mockito.never())
        .setAttribute(Mockito.anyString(), Mockito.anyString());
    Mockito.verifyNoInteractions(filterChainMock);
  }

  @Test
  void givenARequestWithAnInvalidCookieTheDoFilterShouldRespondWithA401StatusCode()
      throws ServletException, IOException {
    // Given
    Cookie[] cookies = {new Cookie("ZM_AUTH_TOKEN", "invalid-token")};
    HttpServletRequest httpRequestMock = Mockito.mock(HttpServletRequest.class);
    Mockito.when(httpRequestMock.getCookies()).thenReturn(cookies);
    HttpServletResponse httpResponseMock = Mockito.mock(HttpServletResponse.class);
    FilterChain filterChainMock = Mockito.mock(FilterChain.class);

    Mockito.when(userManagementClientMock.validateUserToken("invalid-token"))
        .thenReturn(Try.failure(new UnAuthorized()));

    AuthenticationServletFilter authenticationServletFilter =
        new AuthenticationServletFilter(userManagementClientMock);

    // When
    authenticationServletFilter.doFilter(httpRequestMock, httpResponseMock, filterChainMock);

    // Then
    Mockito.verify(httpRequestMock, Mockito.times(1)).getCookies();

    Mockito.verify(userManagementClientMock, Mockito.times(1)).validateUserToken("invalid-token");

    Mockito.verify(httpResponseMock, Mockito.times(1))
        .setStatus(HttpServletResponse.SC_UNAUTHORIZED);

    Mockito.verify(httpRequestMock, Mockito.never())
        .setAttribute(Mockito.anyString(), Mockito.anyString());
    Mockito.verifyNoInteractions(filterChainMock);
  }
}
