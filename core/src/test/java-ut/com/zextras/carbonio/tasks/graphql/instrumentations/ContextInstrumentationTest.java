// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.graphql.instrumentations;

import graphql.GraphQLContext;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters;
import jakarta.servlet.http.HttpServletRequest;
import java.util.NoSuchElementException;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ContextInstrumentationTest {

  @Test
  void
      givenARequestWithARequesterIdAttributeTheInstrumentationShouldPutRequesterIdInGraphQLContext() {
    // Given
    HttpServletRequest requestMock = Mockito.mock(HttpServletRequest.class);
    Mockito.when(requestMock.getAttribute("requesterId")).thenReturn("requester-uuid");

    GraphQLContext graphQLContextMock = Mockito.mock(GraphQLContext.class);
    Mockito.when(graphQLContextMock.get(HttpServletRequest.class)).thenReturn(requestMock);

    InstrumentationExecutionParameters parametersMock =
        Mockito.mock(InstrumentationExecutionParameters.class);
    Mockito.when(parametersMock.getGraphQLContext()).thenReturn(graphQLContextMock);

    InstrumentationState instrumentationStateMock = Mockito.mock(InstrumentationState.class);

    ContextInstrumentation contextInstrumentation = new ContextInstrumentation();

    // When
    contextInstrumentation.beginExecution(parametersMock, instrumentationStateMock);

    // Then
    Mockito.verify(graphQLContextMock, Mockito.times(1)).put("requesterId", "requester-uuid");
    Mockito.verifyNoInteractions(instrumentationStateMock);
  }

  @Test
  void givenANullRequestTheInstrumentationShouldThrowAnException() {
    // Given
    GraphQLContext graphQLContextMock = Mockito.mock(GraphQLContext.class);
    Mockito.when(graphQLContextMock.get(HttpServletRequest.class)).thenReturn(null);

    InstrumentationExecutionParameters parametersMock =
        Mockito.mock(InstrumentationExecutionParameters.class);
    Mockito.when(parametersMock.getGraphQLContext()).thenReturn(graphQLContextMock);

    InstrumentationState instrumentationStateMock = Mockito.mock(InstrumentationState.class);

    ContextInstrumentation contextInstrumentation = new ContextInstrumentation();

    // When
    ThrowableAssert.ThrowingCallable throwable =
        () -> contextInstrumentation.beginExecution(parametersMock, instrumentationStateMock);

    // Then
    Assertions.assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(throwable);
    Mockito.verify(graphQLContextMock, Mockito.never())
        .put(Mockito.anyString(), Mockito.anyString());
    Mockito.verifyNoInteractions(instrumentationStateMock);
  }

  @Test
  void givenARequestWithANullRequesterIdTheInstrumentationShouldThrowAnException() {
    // Given
    HttpServletRequest requestMock = Mockito.mock(HttpServletRequest.class);
    Mockito.when(requestMock.getAttribute("requesterId")).thenReturn(null);

    GraphQLContext graphQLContextMock = Mockito.mock(GraphQLContext.class);
    Mockito.when(graphQLContextMock.get(HttpServletRequest.class)).thenReturn(requestMock);

    InstrumentationExecutionParameters parametersMock =
        Mockito.mock(InstrumentationExecutionParameters.class);
    Mockito.when(parametersMock.getGraphQLContext()).thenReturn(graphQLContextMock);

    InstrumentationState instrumentationStateMock = Mockito.mock(InstrumentationState.class);

    ContextInstrumentation contextInstrumentation = new ContextInstrumentation();

    // When
    ThrowableAssert.ThrowingCallable throwable =
        () -> contextInstrumentation.beginExecution(parametersMock, instrumentationStateMock);

    // Then
    Assertions.assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(throwable);
    Mockito.verify(graphQLContextMock, Mockito.never())
        .put(Mockito.anyString(), Mockito.anyString());
    Mockito.verifyNoInteractions(instrumentationStateMock);
  }
}
