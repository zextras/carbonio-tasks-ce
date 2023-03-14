// SPDX-FileCopyrightText: 2022 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.graphql;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.tasks.Constants.GraphQL.Queries;
import com.zextras.carbonio.tasks.Constants.GraphQL.Types;
import com.zextras.carbonio.tasks.dal.dao.Priority;
import com.zextras.carbonio.tasks.dal.dao.Status;
import com.zextras.carbonio.tasks.graphql.datafetchers.DateTimeScalar;
import com.zextras.carbonio.tasks.graphql.datafetchers.ServiceInfoDataFetcher;
import com.zextras.carbonio.tasks.graphql.datafetchers.TaskDataFetchers;
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

  private static final String schemaURL = "/api/schema.graphql";

  private final ServiceInfoDataFetcher serviceInfoDataFetcher;
  private final TaskDataFetchers taskDataFetchers;

  @Inject
  public GraphQLProvider(
      ServiceInfoDataFetcher serviceInfoDataFetcher, TaskDataFetchers taskDataFetchers) {
    this.serviceInfoDataFetcher = serviceInfoDataFetcher;
    this.taskDataFetchers = taskDataFetchers;
  }

  /**
   * @return a {@link FieldValidationInstrumentation} necessary to bind queries and mutations to a
   *     specific method necessary to validate their inputs
   */
  public FieldValidationInstrumentation buildValidationInstrumentation() {
    FieldValidation fieldValidation = new SimpleFieldValidation();
    /*
    // TODO: in the near future it will be used for the create/update tasks mutations
     .addRule(
         ResultPath.parse("/" + Queries.CREATE_TASKS),
         inputFieldsController.getTasksValidation())

    */
    return new FieldValidationInstrumentation(fieldValidation);
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
                .dataFetcher(Queries.FIND_TASKS, taskDataFetchers.findTasks()))
        .build();
  }

  /**
   * Imports the GraphQL SDL file (from the resource folder) and creates the {@link GraphQLSchema}
   * object associating the parsed SDL with the {@link RuntimeWiring} object.
   *
   * @return the {@link GraphQLSchema}.
   */
  GraphQLSchema buildSchema() {
    InputStream inputStream = getClass().getResourceAsStream(schemaURL);
    Reader schema = new InputStreamReader(inputStream);
    SchemaParser schemaParser = new SchemaParser();

    return new SchemaGenerator().makeExecutableSchema(schemaParser.parse(schema), buildWiring());
  }
}
