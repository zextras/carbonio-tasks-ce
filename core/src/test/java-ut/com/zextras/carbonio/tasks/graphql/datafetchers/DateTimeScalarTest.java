// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.graphql.datafetchers;

import com.sun.jdi.LongValue;
import com.sun.jdi.Type;
import com.sun.jdi.VirtualMachine;
import graphql.language.IntValue;
import graphql.language.StringValue;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;
import java.math.BigInteger;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ThrowableAssert;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;

public class DateTimeScalarTest {

  static Stream<Arguments> generateUnsupportedType() {
    return Stream.of(Arguments.of(Boolean.FALSE), Arguments.of("ciao"), Arguments.of(new Object()));
  }

  @Test
  public void graphQlScalarTypeShouldConstructADateTimeScalarCorrectly() {
    // Given & When
    GraphQLScalarType dateTimeScalarType = new DateTimeScalar().graphQLScalarType();

    // Then
    Assertions.assertThat(dateTimeScalarType.getName()).isEqualTo("DateTime");
    Assertions.assertThat(dateTimeScalarType.getDescription())
        .isEqualTo("A custom scalar representing a date in a timestamp format");
    Assertions.assertThat(dateTimeScalarType.getCoercing()).isInstanceOf(DateTimeScalar.class);
  }

  @Test
  public void givenAStringOfALongTheSerializeShouldReturnASerializedLong() {
    // Given
    DateTimeScalar dateTimeScalar = new DateTimeScalar();
    String inputValue = "333";

    // When
    Long serialize = dateTimeScalar.serialize(inputValue);

    // Then
    Assertions.assertThat(serialize).isEqualTo(333L);
  }

  @ParameterizedTest
  @MethodSource("generateUnsupportedType")
  public void givenAnUnsupportedTypeTheSerializeShouldThrownAnException(Object input) {
    // Given
    DateTimeScalar dateTimeScalar = new DateTimeScalar();

    // When
    ThrowableAssert.ThrowingCallable throwable = () -> dateTimeScalar.serialize(input);

    // Then
    Assertions.assertThatExceptionOfType(CoercingSerializeException.class).isThrownBy(throwable);
  }

  @Test
  public void givenALongTheParseValueShouldReturnAParsedLong() {
    // Given
    DateTimeScalar dateTimeScalar = new DateTimeScalar();
    Long inputValue = 15L;

    // When
    Long parsedValue = dateTimeScalar.parseValue(inputValue);

    // Then
    Assertions.assertThat(parsedValue).isEqualTo(15L);
  }

  @Test
  public void givenAnIntegerTheParseValueShouldReturnAParsedLong() {
    // Given
    DateTimeScalar dateTimeScalar = new DateTimeScalar();
    Integer inputValue = 15;

    // When
    Long parsedValue = dateTimeScalar.parseValue(inputValue);

    // Then
    Assertions.assertThat(parsedValue).isEqualTo(15L);
  }

  @ParameterizedTest
  @NullSource
  @MethodSource("generateUnsupportedType")
  public void givenAnUnsupportedTypeTheParseValueShouldThrownAnException(Object input) {
    // Given
    DateTimeScalar dateTimeScalar = new DateTimeScalar();

    // When
    ThrowableAssert.ThrowingCallable throwable = () -> dateTimeScalar.parseValue(input);

    // Then
    Assertions.assertThatExceptionOfType(CoercingParseValueException.class).isThrownBy(throwable);
  }

  @Test
  public void givenALongValueTheParseLiteralShouldReturnAParsedLong() {
    // Given
    DateTimeScalar dateTimeScalar = new DateTimeScalar();
    LongValue inputValue =
        new LongValue() {
          @Override
          public int compareTo(@NotNull LongValue longValue) {
            return 0;
          }

          @Override
          public VirtualMachine virtualMachine() {
            return null;
          }

          @Override
          public Type type() {
            return null;
          }

          @Override
          public boolean booleanValue() {
            return false;
          }

          @Override
          public byte byteValue() {
            return 0;
          }

          @Override
          public char charValue() {
            return 0;
          }

          @Override
          public short shortValue() {
            return 0;
          }

          @Override
          public int intValue() {
            return 5;
          }

          @Override
          public long longValue() {
            return 5;
          }

          @Override
          public float floatValue() {
            return 0;
          }

          @Override
          public double doubleValue() {
            return 0;
          }

          @Override
          public long value() {
            return 5;
          }
        };

    // When
    Long parsedValue = dateTimeScalar.parseValue(inputValue.longValue());

    // Then
    Assertions.assertThat(parsedValue).isEqualTo(5L);
  }

  @Test
  public void givenAnIntegerValueTheParseLiteralShouldReturnAParsedLong() {
    // Given
    DateTimeScalar dateTimeScalar = new DateTimeScalar();
    IntValue inputValue = new IntValue(BigInteger.valueOf(5));

    // When
    Long parsedValue = dateTimeScalar.parseLiteral(inputValue);

    // Then
    Assertions.assertThat(parsedValue).isEqualTo(5L);
  }

  @Test
  public void givenAStringValueOfALongTheParseLiteralShouldReturnAParsedLong() {
    // Given
    DateTimeScalar dateTimeScalar = new DateTimeScalar();
    StringValue inputValue = new StringValue("4");

    // When
    Long parsedValue = dateTimeScalar.parseLiteral(inputValue);

    // Then
    Assertions.assertThat(parsedValue).isEqualTo(4L);
  }

  @ParameterizedTest
  @NullSource
  @MethodSource("generateUnsupportedType")
  public void givenAnUnsupportedTypeTheParseLiteralShouldThrownAnException(Object input) {
    // Given
    DateTimeScalar dateTimeScalar = new DateTimeScalar();

    // When
    ThrowableAssert.ThrowingCallable throwable = () -> dateTimeScalar.parseLiteral(input);

    // Then
    Assertions.assertThatExceptionOfType(CoercingParseLiteralException.class).isThrownBy(throwable);
  }
}
