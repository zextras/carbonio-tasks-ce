// SPDX-FileCopyrightText: 2022 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.graphql;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.tasks.Constants.GraphQL.Inputs;
import com.zextras.carbonio.tasks.Constants.GraphQL.Mutations;
import com.zextras.carbonio.tasks.Constants.GraphQL.Queries;
import com.zextras.carbonio.tasks.Constants.GraphQL.Types;
import com.zextras.carbonio.tasks.dal.dao.Priority;
import com.zextras.carbonio.tasks.dal.dao.Status;
import com.zextras.carbonio.tasks.graphql.datafetchers.DateTimeScalar;
import com.zextras.carbonio.tasks.graphql.datafetchers.ServiceInfoDataFetcher;
import com.zextras.carbonio.tasks.graphql.datafetchers.TaskDataFetchers;
import com.zextras.carbonio.tasks.graphql.instrumentations.ContextInstrumentation;
import com.zextras.carbonio.tasks.graphql.validators.InputFieldsValidator;
import graphql.execution.ResultPath;
import graphql.execution.instrumentation.SimpleInstrumentation;
import graphql.execution.instrumentation.fieldvalidation.FieldValidation;
import graphql.execution.instrumentation.fieldvalidation.FieldValidationInstrumentation;
import graphql.execution.instrumentation.fieldvalidation.SimpleFieldValidation;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * Exposes methods to set up a {@link GraphQLServlet} configuration with all the necessary
 * properties. In particular, it allows to:
 *
 * <ul>
 *   <li>Load The GraphQL SDL schema from resources
 *   <li>Create a {@link RuntimeWiring} to bind queries and mutations to the related data-fetchers
 *   <li>Create a {@link FieldValidationInstrumentation} to bind queries and mutations to the
 *       related validation logic to validate their inputs
 * </ul>
 */
@Singleton
public class GraphQLProvider {

  private static final String SCHEMA_URL = "/api/schema.graphql";

  private final ContextInstrumentation contextInstrumentation;
  private final ServiceInfoDataFetcher serviceInfoDataFetcher;
  private final TaskDataFetchers taskDataFetchers;
  private final InputFieldsValidator inputFieldsValidator;

  @Inject
  public GraphQLProvider(
      ContextInstrumentation contextInstrumentation,
      ServiceInfoDataFetcher serviceInfoDataFetcher,
      TaskDataFetchers taskDataFetchers,
      InputFieldsValidator inputFieldsValidator) {

    this.contextInstrumentation = contextInstrumentation;
    this.serviceInfoDataFetcher = serviceInfoDataFetcher;
    this.taskDataFetchers = taskDataFetchers;
    this.inputFieldsValidator = inputFieldsValidator;
  }

  /**
   * @return a {@link FieldValidationInstrumentation} necessary to bind queries and mutations to a
   *     specific method necessary to validate their inputs
   */
  public FieldValidationInstrumentation buildValidationInstrumentation() {
    FieldValidation fieldValidation =
        new SimpleFieldValidation()
            .addRule(
                ResultPath.parse("/" + Mutations.CREATE_TASK),
                inputFieldsValidator.upsertTaskValidator(Inputs.NEW_TASK))
            .addRule(
                ResultPath.parse("/" + Mutations.UPDATE_TASK),
                inputFieldsValidator.upsertTaskValidator(Inputs.UPDATE_TASK));

    return new FieldValidationInstrumentation(fieldValidation);
  }

  public SimpleInstrumentation getContextInstrumentation() {
    return contextInstrumentation;
  }

  /**
   * Creates a {@link RuntimeWiring} object binding:
   *
   * <ul>
   *   <li>The DateTime GraphQL scalar to the {@link DateTimeScalar} object
   *   <li>The {@link Types#PRIORITY} to the related enumerator
   *   <li>The {@link Types#STATUS} to the related enumerator
   *   <li>The queries and the mutations to the related {@link DataFetcher}s.
   * </ul>
   *
   * @return a {@link RuntimeWiring} instance.
   */
  private RuntimeWiring buildWiring() {
    return RuntimeWiring.newRuntimeWiring()
        .scalar(new DateTimeScalar().graphQLScalarType())
        .type(newTypeWiring(Types.PRIORITY).enumValues(Priority::valueOf))
        .type(newTypeWiring(Types.STATUS).enumValues(Status::valueOf))
        .type(
            newTypeWiring("Query")
                .dataFetcher(Queries.GET_SERVICE_INFO, serviceInfoDataFetcher)
                .dataFetcher(Queries.GET_TASK, taskDataFetchers.getTask())
                .dataFetcher(Queries.FIND_TASKS, taskDataFetchers.findTasks()))
        .type(
            newTypeWiring("Mutation")
                .dataFetcher(Mutations.CREATE_TASK, taskDataFetchers.createTask())
                .dataFetcher(Mutations.UPDATE_TASK, taskDataFetchers.updateTask()))
        .build();
  }

  /**
   * Imports the GraphQL SDL file (from the resource folder) and creates the {@link GraphQLSchema}
   * object associating the parsed SDL with the {@link RuntimeWiring} object.
   *
   * @return the {@link GraphQLSchema}.
   */
  GraphQLSchema buildSchema() {
    InputStream inputStream = getClass().getResourceAsStream(SCHEMA_URL);
    Reader schema = new InputStreamReader(inputStream);
    SchemaParser schemaParser = new SchemaParser();

    return new SchemaGenerator().makeExecutableSchema(schemaParser.parse(schema), buildWiring());
  }
}
