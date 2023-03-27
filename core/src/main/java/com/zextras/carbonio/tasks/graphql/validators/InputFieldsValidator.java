// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.graphql.validators;

import com.zextras.carbonio.tasks.Constants.GraphQL.Inputs;
import com.zextras.carbonio.tasks.Constants.GraphQL.Inputs.TaskInput;
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
      upsertTaskValidator(String inputField) {

    return (fieldAndArguments, fieldValidationEnvironment) -> {
      Map<String, Object> newTaskArguments = fieldAndArguments.getArgumentValue(inputField);
      List<String> errors = new ArrayList<>();

      // Check title attribute
      if (newTaskArguments.containsKey(TaskInput.TITLE)) {
        String title = (String) newTaskArguments.get(TaskInput.TITLE);
        checkStringLength(TaskInput.TITLE, title, Inputs.TITLE_MAX_LENGTH).ifPresent(errors::add);
      }

      // Check description attribute
      if (newTaskArguments.containsKey(TaskInput.DESCRIPTION)) {
        String description = (String) newTaskArguments.get(TaskInput.DESCRIPTION);
        checkStringLength(TaskInput.DESCRIPTION, description, Inputs.DESCRIPTION_MAX_LENGTH)
            .ifPresent(errors::add);
      }

      // Check reminderAllDay attribute
      Long reminderAt = (Long) newTaskArguments.get(TaskInput.REMINDER_AT);
      Boolean reminderAllDay = (Boolean) newTaskArguments.get(TaskInput.REMINDER_ALL_DAY);

      // The reminder all day flag must not be set if there is no reminder and vice versa!
      if ((reminderAt == null && reminderAllDay != null)
          || (reminderAt != null && reminderAllDay == null)) {
        errors.add("The reminderAt and the reminderAllDay attributes must be both always set");
      }

      return errors.isEmpty()
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
