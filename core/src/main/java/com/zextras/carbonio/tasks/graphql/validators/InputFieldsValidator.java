// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.graphql.validators;

import com.zextras.carbonio.tasks.Constants.GraphQL.Inputs;
import com.zextras.carbonio.tasks.Constants.GraphQL.Inputs.NewTaskInput;
import graphql.GraphQLError;
import graphql.execution.instrumentation.fieldvalidation.FieldAndArguments;
import graphql.execution.instrumentation.fieldvalidation.FieldValidationEnvironment;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

public class InputFieldsValidator {

  public BiFunction<FieldAndArguments, FieldValidationEnvironment, Optional<GraphQLError>>
      createTaskValidator() {
    return (fieldAndArguments, fieldValidationEnvironment) -> {
      Map<String, Object> createTask = fieldAndArguments.getArgumentValuesByName();
      Map<String, Object> newTaskArguments = (Map<String, Object>) createTask.get(Inputs.NEW_TASK);
      String title = (String) newTaskArguments.get(NewTaskInput.TITLE);

      List<String> errors = new ArrayList<>();
      checkStringLength(NewTaskInput.TITLE, title, Inputs.TITLE_MAX_LENGTH).ifPresent(errors::add);
      if (newTaskArguments.containsKey(NewTaskInput.DESCRIPTION)) {
        String description = (String) newTaskArguments.get(NewTaskInput.DESCRIPTION);
        checkStringLength(NewTaskInput.DESCRIPTION, description, Inputs.DESCRIPTION_MAX_LENGTH)
            .ifPresent(errors::add);
      }

      return errors.size() == 0
          ? Optional.empty()
          : Optional.of(fieldValidationEnvironment.mkError(String.join("\n", errors)));
    };
  }

  private Optional<String> checkStringLength(String fieldName, String input, int maxLength) {
    return (input.length() > maxLength)
        ? Optional.of(
            String.format("Invalid %s. Length is more than %s characters", fieldName, maxLength))
        : Optional.empty();
  }
}
