package com.zextras.carbonio.tasks;

import static graphql.schema.idl.RuntimeWiring.newRuntimeWiring;

import graphql.kickstart.servlet.GraphQLConfiguration;
import graphql.kickstart.servlet.GraphQLHttpServlet;
import graphql.schema.StaticDataFetcher;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;

public class GraphQLServlet extends GraphQLHttpServlet {

  @Override
  protected GraphQLConfiguration getConfiguration() {

    String schema = "type Query{hello: String}";

    SchemaParser schemaParser = new SchemaParser();
    TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.parse(schema);

    RuntimeWiring runtimeWiring = newRuntimeWiring()
        .type("Query", builder -> builder.dataFetcher("hello", new StaticDataFetcher("world")))
        .build();

    SchemaGenerator schemaGenerator = new SchemaGenerator();
    return GraphQLConfiguration.with(
        schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring)).build();
  }
}
