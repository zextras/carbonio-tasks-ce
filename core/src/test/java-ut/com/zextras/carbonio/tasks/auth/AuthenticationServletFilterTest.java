// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.auth;

import com.zextras.carbonio.tasks.Constants.Config.UserService;
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
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.verify.VerificationTimes;

public class AuthenticationServletFilterTest {

  static ClientAndServer userManagementServiceMock;

  @BeforeAll
  static void init() {
    userManagementServiceMock = ClientAndServer.startClientAndServer(UserService.PORT);
  }

  @AfterEach
  public void cleanUp() {
    userManagementServiceMock.reset();
  }

  @AfterAll
  static void cleanUpAll() {
    userManagementServiceMock.stop();
  }

  @Test
  public void givenAFilterConfigTheInitShouldInitializeTheFilter() throws ServletException {
    // Given
    FilterConfig filterConfigMock = Mockito.mock(FilterConfig.class);
    Mockito.when(filterConfigMock.getServletContext())
        .thenReturn(Mockito.mock(ServletContext.class));
    AuthenticationServletFilter authenticationServletFilter = new AuthenticationServletFilter();

    // When
    authenticationServletFilter.init(filterConfigMock);

    // Then
    Mockito.verify(filterConfigMock, Mockito.times(1)).getServletContext();
  }

  @Test
  public void givenARequestWithAValidCookieTheDoFilterShouldAddTheRequesterIdInTheServletContext()
      throws ServletException, IOException {
    // Given
    Cookie[] cookies = {
      new Cookie("ZM_AUTH_TOKEN", "zm-token"), new Cookie("ZX_AUTH_TOKEN", "zx-token")
    };
    HttpServletRequest httpRequestMock = Mockito.mock(HttpServletRequest.class);
    Mockito.when(httpRequestMock.getCookies()).thenReturn(cookies);

    HttpServletResponse httpResponseMock = Mockito.mock(HttpServletResponse.class);
    FilterChain filterChainMock = Mockito.mock(FilterChain.class);

    userManagementServiceMock
        .when(
            HttpRequest.request()
                .withMethod(HttpMethod.GET.toString())
                .withPath("/auth/token/zm-token"))
        .respond(
            HttpResponse.response()
                .withStatusCode(HttpStatus.OK_200)
                .withBody("{\"userId\": \"00000000-0000-0000-0000-000000000000\"}"));

    AuthenticationServletFilter authenticationServletFilter = new AuthenticationServletFilter();

    // When
    authenticationServletFilter.doFilter(httpRequestMock, httpResponseMock, filterChainMock);

    // Then
    Mockito.verify(httpRequestMock, Mockito.times(1)).getCookies();

    userManagementServiceMock.verify(
        HttpRequest.request()
            .withMethod(HttpMethod.GET.toString())
            .withPath("/auth/token/zm-token"),
        VerificationTimes.once());

    Mockito.verify(httpRequestMock, Mockito.times(1))
        .setAttribute("requesterId", "00000000-0000-0000-0000-000000000000");

    Mockito.verify(filterChainMock, Mockito.times(1)).doFilter(httpRequestMock, httpResponseMock);
  }

  @Test
  public void givenANotHttpRequestTheDoFilterShouldBlockTheRequest()
      throws ServletException, IOException {
    // Given
    ServletRequest httpRequestMock = Mockito.mock(ServletRequest.class);
    ServletResponse httpResponseMock = Mockito.mock(ServletResponse.class);
    FilterChain filterChainMock = Mockito.mock(FilterChain.class);

    AuthenticationServletFilter authenticationServletFilter = new AuthenticationServletFilter();

    // When
    authenticationServletFilter.doFilter(httpRequestMock, httpResponseMock, filterChainMock);

    // Then
    userManagementServiceMock.verifyZeroInteractions();
    Mockito.verifyNoInteractions(httpRequestMock);
    Mockito.verifyNoInteractions(httpResponseMock);
    Mockito.verifyNoInteractions(filterChainMock);
  }

  @Test
  public void givenARequestWithAnUnsupportedCookieTypeTheDoFilterShouldRespondWithA401StatusCode()
      throws ServletException, IOException {
    // Given
    Cookie[] cookies = {new Cookie("UNSUPPORTED_COOKIE_TYPE", "zm-token")};
    HttpServletRequest httpRequestMock = Mockito.mock(HttpServletRequest.class);
    Mockito.when(httpRequestMock.getCookies()).thenReturn(cookies);
    HttpServletResponse httpResponseMock = Mockito.mock(HttpServletResponse.class);
    FilterChain filterChainMock = Mockito.mock(FilterChain.class);

    AuthenticationServletFilter authenticationServletFilter = new AuthenticationServletFilter();

    // When
    authenticationServletFilter.doFilter(httpRequestMock, httpResponseMock, filterChainMock);

    // Then
    Mockito.verify(httpRequestMock, Mockito.times(1)).getCookies();
    Mockito.verify(httpResponseMock, Mockito.times(1))
        .setStatus(HttpServletResponse.SC_UNAUTHORIZED);

    userManagementServiceMock.verifyZeroInteractions();
    Mockito.verify(httpRequestMock, Mockito.never())
        .setAttribute(Mockito.anyString(), Mockito.anyString());
    Mockito.verifyNoInteractions(filterChainMock);
  }

  @Test
  public void givenARequestWithAnInvalidCookieTheDoFilterShouldRespondWithA401StatusCode()
      throws ServletException, IOException {
    // Given
    Cookie[] cookies = {new Cookie("ZM_AUTH_TOKEN", "invalid-token")};
    HttpServletRequest httpRequestMock = Mockito.mock(HttpServletRequest.class);
    Mockito.when(httpRequestMock.getCookies()).thenReturn(cookies);
    HttpServletResponse httpResponseMock = Mockito.mock(HttpServletResponse.class);
    FilterChain filterChainMock = Mockito.mock(FilterChain.class);

    userManagementServiceMock
        .when(
            HttpRequest.request()
                .withMethod(HttpMethod.GET.toString())
                .withPath("/auth/token/invalid-token"))
        .respond(HttpResponse.response().withStatusCode(HttpStatus.UNAUTHORIZED_401));

    AuthenticationServletFilter authenticationServletFilter = new AuthenticationServletFilter();

    // When
    authenticationServletFilter.doFilter(httpRequestMock, httpResponseMock, filterChainMock);

    // Then
    Mockito.verify(httpRequestMock, Mockito.times(1)).getCookies();

    userManagementServiceMock.verify(
        HttpRequest.request()
            .withMethod(HttpMethod.GET.toString())
            .withPath("/auth/token/invalid-token"),
        VerificationTimes.once());

    Mockito.verify(httpResponseMock, Mockito.times(1))
        .setStatus(HttpServletResponse.SC_UNAUTHORIZED);

    Mockito.verify(httpRequestMock, Mockito.never())
        .setAttribute(Mockito.anyString(), Mockito.anyString());
    Mockito.verifyNoInteractions(filterChainMock);
  }
}
