// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.graphql.instrumentations;

import com.google.common.collect.ImmutableMap;
import com.zextras.carbonio.tasks.graphql.validators.InputFieldsValidator;
import graphql.GraphQLError;
import graphql.execution.instrumentation.fieldvalidation.FieldAndArguments;
import graphql.execution.instrumentation.fieldvalidation.FieldValidationEnvironment;
import java.util.Map;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class InputFieldsValidatorTest {
  static String string1024chars =
      "drmOaBQ4v5xzTnbRdqt7PdUgK49oV5CstdsskBAnMEoJUypa410nWWGeoGFW5mv"
          + "mVMCjPjmCNAXww9Ptjj3ywNdmIV8cJbFT7ueOKLwaOgGucFuhhRCfXUHN4YCmxFJ4yfST67Amc8x5R2tJU2pHfgmLOwSN"
          + "r5xjogSsSvzfDTjVs5ohj2PK9zjl75mX7fWKZpYvLS1SkMGFWCUvYH5GnH9CP6aG8SwhIGH8Kmk0yNPvtDm9NTUMszj4C"
          + "p5Vuhwm78sqnlo2gZESyAeDd2O3HbSKm98CUrwfk4xHwq9LuRoTgD6WQk21LLTSv1j4GoQvElIHYcL0pdTEFEj2LUdRob"
          + "we1k3cKlR5J0iL52UEdUeLyeV6dVvPg5UTFbudNZm0urVouCFIC1bPvyhq7to7VHAfVVZFKJzCdNIqnAiVXR3hZgLnSrG"
          + "bjdA5et5d5W3cMXeN7WhMmQqpF6xacMLHclNWqjZkcPJs8tPEr89rBXn1UlaFiIU4QAXsuZZOXemUP3gyafKzSgaRZL2x"
          + "kcKT7vZuPkPBPxybQnV5CmGdI0zq5sZjU1BxIqxLH25g89Cf9xcGdoCW0nWzF1gp4EhBQ7Rc7wTNJYNcxdw5c8lUqLMrC"
          + "1c815Tmi9tIxFxjANqNSbocmWbUcqV5jtpJEY7oyZYcUW5JxFcUwyackjslB05XWPd5Rcb0gqdd72QEvyAh6vKUdNjymw"
          + "k19Eqk6Cs67Qx8yhcyLKoXZmJqTBCJ1NTYgP3ziMuvphC71phsGt8pO0V7hjoDBdmJH2zrvqxULJyKMsj5sHMEnFFtSyZ"
          + "WDxMHHA9yepuv33HIqV9zuC5LcYmKSyjtwj1yC1us3r2RPwNWZyFf6nV1ppwDjrIPZZ7UEjAjpH42WBgrGO9FHwfJz3CS"
          + "40DuRFXTHhWjwYXa3lzFkzaD734vOl0zLlKs7taKDWEAsCof04MyuYaOREVt9P9X14utNkgLI9wr9zWPLDOqQtYlGo4N2"
          + "ZBaFs607fCWOfMw6KZqwfP33gHFwE0a";

  @Test
  public void givenValidTaskInputAttributesTheUpsertTaskValidatorShouldNotReturnErrors() {
    // Given
    Map<String, Object> newTaskInput =
        ImmutableMap.<String, Object>builder()
            .put("title", "valid title")
            .put("description", "valid description")
            .put("reminderAt", 5L)
            .put("reminderAllDay", false)
            .build();

    FieldAndArguments fieldAndArgumentsMock = Mockito.mock(FieldAndArguments.class);
    Mockito.when(fieldAndArgumentsMock.getArgumentValue("newTask")).thenReturn(newTaskInput);

    FieldValidationEnvironment environmentMock = Mockito.mock(FieldValidationEnvironment.class);

    InputFieldsValidator inputFieldsValidator = new InputFieldsValidator();

    // When
    Optional<GraphQLError> optErrors =
        inputFieldsValidator
            .upsertTaskValidator("newTask")
            .apply(fieldAndArgumentsMock, environmentMock);

    // Then
    Mockito.verify(fieldAndArgumentsMock, Mockito.times(1)).getArgumentValue("newTask");
    Mockito.verifyNoInteractions(environmentMock);

    Assertions.assertThat(optErrors).isEmpty();
  }

  @Test
  public void givenAllTaskInputAttributesInvalidTheUpsertTaskValidatorShouldNotReturnErrors() {
    // Given
    String description =
        string1024chars + string1024chars + string1024chars + string1024chars + "0";
    Map<String, Object> newTaskInput =
        ImmutableMap.<String, Object>builder()
            .put("title", string1024chars + "0")
            .put("description", description)
            .put("reminderAt", 5L)
            .build();

    FieldAndArguments fieldAndArgumentsMock = Mockito.mock(FieldAndArguments.class);
    Mockito.when(fieldAndArgumentsMock.getArgumentValue("newTask")).thenReturn(newTaskInput);

    FieldValidationEnvironment environmentMock = Mockito.mock(FieldValidationEnvironment.class);
    Mockito.when(
            environmentMock.mkError(
                "Invalid title. Length is more than 1024 characters\n"
                    + "Invalid description. Length is more than 4096 characters\n"
                    + "The reminderAt and the reminderAllDay attributes must be both always set"))
        .thenReturn(Mockito.mock(GraphQLError.class));

    InputFieldsValidator inputFieldsValidator = new InputFieldsValidator();

    // When
    Optional<GraphQLError> optErrors =
        inputFieldsValidator
            .upsertTaskValidator("newTask")
            .apply(fieldAndArgumentsMock, environmentMock);

    // Then
    Mockito.verify(fieldAndArgumentsMock, Mockito.times(1)).getArgumentValue("newTask");
    Mockito.verify(environmentMock, Mockito.times(1))
        .mkError(
            "Invalid title. Length is more than 1024 characters\n"
                + "Invalid description. Length is more than 4096 characters\n"
                + "The reminderAt and the reminderAllDay attributes must be both always set");

    Assertions.assertThat(optErrors).isPresent();
  }

  @Test
  public void givenATaskTitleOf1024CharsTheUpsertTaskValidatorShouldNotReturnErrors() {
    // Given
    Map<String, Object> newTaskInput =
        ImmutableMap.<String, Object>builder().put("title", string1024chars).build();

    FieldAndArguments fieldAndArgumentsMock = Mockito.mock(FieldAndArguments.class);
    Mockito.when(fieldAndArgumentsMock.getArgumentValue("newTask")).thenReturn(newTaskInput);

    FieldValidationEnvironment environmentMock = Mockito.mock(FieldValidationEnvironment.class);

    InputFieldsValidator inputFieldsValidator = new InputFieldsValidator();

    // When
    Optional<GraphQLError> optErrors =
        inputFieldsValidator
            .upsertTaskValidator("newTask")
            .apply(fieldAndArgumentsMock, environmentMock);

    // Then
    Mockito.verify(fieldAndArgumentsMock, Mockito.times(1)).getArgumentValue("newTask");
    Mockito.verifyNoInteractions(environmentMock);

    Assertions.assertThat(optErrors).isEmpty();
  }

  @Test
  public void givenATaskTitleTooLongTheUpsertTaskValidatorShouldReturnAnError() {
    // Given
    Map<String, Object> newTaskInput =
        ImmutableMap.<String, Object>builder().put("title", string1024chars + "0").build();

    FieldAndArguments fieldAndArgumentsMock = Mockito.mock(FieldAndArguments.class);
    Mockito.when(fieldAndArgumentsMock.getArgumentValue("newTask")).thenReturn(newTaskInput);

    FieldValidationEnvironment environmentMock = Mockito.mock(FieldValidationEnvironment.class);
    Mockito.when(environmentMock.mkError("Invalid title. Length is more than 1024 characters"))
        .thenReturn(Mockito.mock(GraphQLError.class));

    InputFieldsValidator inputFieldsValidator = new InputFieldsValidator();

    // When
    Optional<GraphQLError> optErrors =
        inputFieldsValidator
            .upsertTaskValidator("newTask")
            .apply(fieldAndArgumentsMock, environmentMock);

    // Then
    Mockito.verify(fieldAndArgumentsMock, Mockito.times(1)).getArgumentValue("newTask");
    Mockito.verify(environmentMock, Mockito.times(1))
        .mkError("Invalid title. Length is more than 1024 characters");

    Assertions.assertThat(optErrors).isPresent();
  }

  @Test
  public void givenATaskDescriptionOf4096CharsTheUpsertTaskValidatorShouldNotReturnErrors() {
    // Given
    String description = string1024chars + string1024chars + string1024chars + string1024chars;
    Map<String, Object> newTaskInput =
        ImmutableMap.<String, Object>builder().put("description", description).build();

    FieldAndArguments fieldAndArgumentsMock = Mockito.mock(FieldAndArguments.class);
    Mockito.when(fieldAndArgumentsMock.getArgumentValue("newTask")).thenReturn(newTaskInput);

    FieldValidationEnvironment environmentMock = Mockito.mock(FieldValidationEnvironment.class);

    InputFieldsValidator inputFieldsValidator = new InputFieldsValidator();

    // When
    Optional<GraphQLError> optErrors =
        inputFieldsValidator
            .upsertTaskValidator("newTask")
            .apply(fieldAndArgumentsMock, environmentMock);

    // Then
    Mockito.verify(fieldAndArgumentsMock, Mockito.times(1)).getArgumentValue("newTask");
    Mockito.verifyNoInteractions(environmentMock);

    Assertions.assertThat(optErrors).isEmpty();
  }

  @Test
  public void givenATaskDescriptionTooLongTheUpsertTaskValidatorShouldReturnAnError() {
    // Given
    String description =
        string1024chars + string1024chars + string1024chars + string1024chars + "0";
    Map<String, Object> newTaskInput =
        ImmutableMap.<String, Object>builder().put("description", description).build();

    FieldAndArguments fieldAndArgumentsMock = Mockito.mock(FieldAndArguments.class);
    Mockito.when(fieldAndArgumentsMock.getArgumentValue("newTask")).thenReturn(newTaskInput);

    FieldValidationEnvironment environmentMock = Mockito.mock(FieldValidationEnvironment.class);
    Mockito.when(
            environmentMock.mkError("Invalid description. Length is more than 4096 characters"))
        .thenReturn(Mockito.mock(GraphQLError.class));

    InputFieldsValidator inputFieldsValidator = new InputFieldsValidator();

    // When
    Optional<GraphQLError> optErrors =
        inputFieldsValidator
            .upsertTaskValidator("newTask")
            .apply(fieldAndArgumentsMock, environmentMock);

    // Then
    Mockito.verify(fieldAndArgumentsMock, Mockito.times(1)).getArgumentValue("newTask");
    Mockito.verify(environmentMock, Mockito.times(1))
        .mkError("Invalid description. Length is more than 4096 characters");

    Assertions.assertThat(optErrors).isPresent();
  }

  @Test
  public void givenATaskReminderAtAndAReminderAllDayTheUpsertTaskValidatorShouldNotReturnErrors() {
    // Given
    Map<String, Object> newTaskInput =
        ImmutableMap.<String, Object>builder()
            .put("reminderAt", 5L)
            .put("reminderAllDay", true)
            .build();

    FieldAndArguments fieldAndArgumentsMock = Mockito.mock(FieldAndArguments.class);
    Mockito.when(fieldAndArgumentsMock.getArgumentValue("newTask")).thenReturn(newTaskInput);

    FieldValidationEnvironment environmentMock = Mockito.mock(FieldValidationEnvironment.class);

    InputFieldsValidator inputFieldsValidator = new InputFieldsValidator();

    // When
    Optional<GraphQLError> optErrors =
        inputFieldsValidator
            .upsertTaskValidator("newTask")
            .apply(fieldAndArgumentsMock, environmentMock);

    // Then
    Mockito.verify(fieldAndArgumentsMock, Mockito.times(1)).getArgumentValue("newTask");
    Mockito.verifyNoInteractions(environmentMock);

    Assertions.assertThat(optErrors).isEmpty();
  }

  @Test
  public void givenOnlyATaskReminderAtTheUpsertTaskValidatorShouldReturnAnError() {
    // Given
    Map<String, Object> newTaskInput =
        ImmutableMap.<String, Object>builder().put("reminderAt", 5L).build();

    FieldAndArguments fieldAndArgumentsMock = Mockito.mock(FieldAndArguments.class);
    Mockito.when(fieldAndArgumentsMock.getArgumentValue("newTask")).thenReturn(newTaskInput);

    FieldValidationEnvironment environmentMock = Mockito.mock(FieldValidationEnvironment.class);
    Mockito.when(
            environmentMock.mkError(
                "The reminderAt and the reminderAllDay attributes must be both always set"))
        .thenReturn(Mockito.mock(GraphQLError.class));

    InputFieldsValidator inputFieldsValidator = new InputFieldsValidator();

    // When
    Optional<GraphQLError> optErrors =
        inputFieldsValidator
            .upsertTaskValidator("newTask")
            .apply(fieldAndArgumentsMock, environmentMock);

    // Then
    Mockito.verify(fieldAndArgumentsMock, Mockito.times(1)).getArgumentValue("newTask");
    Mockito.verify(environmentMock, Mockito.times(1))
        .mkError("The reminderAt and the reminderAllDay attributes must be both always set");

    Assertions.assertThat(optErrors).isPresent();
  }

  @Test
  public void givenOnlyATaskReminderAllDayTheUpsertTaskValidatorShouldReturnAnError() {
    // Given
    Map<String, Object> newTaskInput =
        ImmutableMap.<String, Object>builder().put("reminderAllDay", true).build();

    FieldAndArguments fieldAndArgumentsMock = Mockito.mock(FieldAndArguments.class);
    Mockito.when(fieldAndArgumentsMock.getArgumentValue("newTask")).thenReturn(newTaskInput);

    FieldValidationEnvironment environmentMock = Mockito.mock(FieldValidationEnvironment.class);
    Mockito.when(
            environmentMock.mkError(
                "The reminderAt and the reminderAllDay attributes must be both always set"))
        .thenReturn(Mockito.mock(GraphQLError.class));

    InputFieldsValidator inputFieldsValidator = new InputFieldsValidator();

    // When
    Optional<GraphQLError> optErrors =
        inputFieldsValidator
            .upsertTaskValidator("newTask")
            .apply(fieldAndArgumentsMock, environmentMock);

    // Then
    Mockito.verify(fieldAndArgumentsMock, Mockito.times(1)).getArgumentValue("newTask");
    Mockito.verify(environmentMock, Mockito.times(1))
        .mkError("The reminderAt and the reminderAllDay attributes must be both always set");

    Assertions.assertThat(optErrors).isPresent();
  }
}
