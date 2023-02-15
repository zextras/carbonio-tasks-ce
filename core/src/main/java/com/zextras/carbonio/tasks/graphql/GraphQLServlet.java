// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.graphql;

import static graphql.schema.idl.RuntimeWiring.newRuntimeWiring;

import graphql.kickstart.servlet.GraphQLConfiguration;
import graphql.kickstart.servlet.GraphQLHttpServlet;
import graphql.schema.StaticDataFetcher;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.schema.idl.TypeRuntimeWiring;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/** This class is only a placeholder to start the servlet correctly */
public class GraphQLServlet extends GraphQLHttpServlet {

  /**
   * @return a <bold>WIP</bold> {@link GraphQLConfiguration} containing a {@link graphql.GraphQL}
   *     instance necessary to handle the http requests.
   */
  @Override
  protected GraphQLConfiguration getConfiguration() {
    InputStream schema = getClass().getResourceAsStream("/api/schema.graphql");
    SchemaParser schemaParser = new SchemaParser();
    TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.parse(schema);

    Map<String, Object> projectInfo = new HashMap<>();
    projectInfo.put("name", "carbonio-tasks-ce");
    projectInfo.put("version", "0.0.1");
    RuntimeWiring runtimeWiring =
        newRuntimeWiring()
            .type(
                TypeRuntimeWiring.newTypeWiring("Query")
                    .dataFetcher("getProjectInfo", new StaticDataFetcher(projectInfo)))
            .build();

    SchemaGenerator schemaGenerator = new SchemaGenerator();
    return GraphQLConfiguration.with(
            schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring))
        .build();
  }
}
