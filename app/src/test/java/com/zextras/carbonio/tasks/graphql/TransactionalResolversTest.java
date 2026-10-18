// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.ThrowsDeclaration;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.GraphQLException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Resolvers are transactional by default (Quarkus adds {@link Transactional} to them): none opts
 * out, and a failed commit stays a generic error because no resolver declares the checked {@code
 * jakarta.transaction} exceptions (undeclared, they reach SmallRye wrapped in an unchecked one).
 */
class TransactionalResolversTest {

  private static List<JavaClass> apis;

  @BeforeAll
  static void importTheGraphQLApis() {
    apis = new ArrayList<>();
    for (JavaClass javaClass :
        new ClassFileImporter().importPackages("com.zextras.carbonio.tasks")) {
      if (javaClass.isAnnotatedWith(GraphQLApi.class)) {
        apis.add(javaClass);
      }
    }
    assertThat(apis).extracting(JavaClass::getSimpleName).contains("TasksGraphQLApi");
  }

  @Test
  void noResolverOverridesTheResolverTransaction() {
    List<String> overrides = new ArrayList<>();
    for (JavaClass api : apis) {
      for (JavaMethod method : api.getMethods()) {
        if (method.isAnnotatedWith(Transactional.class)) {
          overrides.add(api.getSimpleName() + "#" + method.getName());
        }
      }
    }

    assertThat(overrides).isEmpty();
  }

  @Test
  void noGraphQLApiClassIsTransactional() {
    assertThat(apis)
        .filteredOn(api -> api.isAnnotatedWith(Transactional.class))
        .extracting(JavaClass::getSimpleName)
        .isEmpty();
  }

  @Test
  void resolversDeclareNoCheckedExceptionButGraphQLException() {
    List<String> declared = new ArrayList<>();
    for (JavaClass api : apis) {
      for (JavaMethod method : api.getMethods()) {
        for (ThrowsDeclaration<JavaMethod> thrown : method.getThrowsClause()) {
          JavaClass type = thrown.getRawType();
          if (!type.isAssignableTo(GraphQLException.class)
              && !type.isAssignableTo(RuntimeException.class)) {
            declared.add(api.getSimpleName() + "#" + method.getName() + " " + type.getName());
          }
        }
      }
    }

    assertThat(declared).isEmpty();
  }
}
