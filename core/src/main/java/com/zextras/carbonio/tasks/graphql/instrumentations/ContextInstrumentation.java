// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.graphql.instrumentations;

import com.zextras.carbonio.tasks.Constants.GraphQL.Context;
import graphql.ExecutionResult;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.SimpleInstrumentation;
import graphql.execution.instrumentation.SimpleInstrumentationContext;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters;
import jakarta.servlet.http.HttpServletRequest;
import java.util.NoSuchElementException;

public class ContextInstrumentation extends SimpleInstrumentation {

  @Override
  public InstrumentationContext<ExecutionResult> beginExecution(
      InstrumentationExecutionParameters parameters, InstrumentationState instrumentationState) {
    HttpServletRequest request = parameters.getGraphQLContext().get(HttpServletRequest.class);

    if (request != null) {
      String requesterId = (String) request.getAttribute(Context.REQUESTER_ID);

      if (requesterId != null) {
        parameters.getGraphQLContext().put(Context.REQUESTER_ID, requesterId);
        return SimpleInstrumentationContext.noOp();
      }
      throw new NoSuchElementException("Unable to find the requestId attribute in the request");
    }

    throw new NoSuchElementException("Unable to find an HttpServletRequest object in the context");
  }
}
