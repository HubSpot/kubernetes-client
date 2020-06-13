package io.fabric8.kubernetes.api.model.apiextensions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JSONSchemaPropsOrStringArrayTest {
  @Test
  public void itDeserializesAdditionalPropertiesTrue() throws IOException {
    InputStream resourceAsStream = getClass().getResourceAsStream("/dependencies_array.json");
    JSONSchemaProps props = new ObjectMapper().readValue(resourceAsStream, JSONSchemaProps.class);

    Assertions.assertEquals(props, new JSONSchemaPropsBuilder()
      .withType("object")
      .addToDependencies(
        "foo",
        new JSONSchemaPropsOrStringArrayBuilder()
          .withProperty("a", "b")
          .build())
      .build());
  }

  @Test
  public void itSerializesAdditionalPropertiesTrue() throws JsonProcessingException {
    String expectedJson = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/dependencies_array.json"), StandardCharsets.UTF_8))
      .lines()
      .collect(Collectors.joining("\n"));

    String outputJson = new ObjectMapper().writeValueAsString(new JSONSchemaPropsBuilder()
      .withType("object")
      .addToDependencies(
        "foo",
        new JSONSchemaPropsOrStringArrayBuilder()
          .withProperty("a", "b")
          .build())
      .build());

    Assertions.assertEquals(expectedJson, outputJson);
  }

  @Test
  public void itDeserializesAdditionalPropertiesTyped() throws IOException {
    InputStream resourceAsStream = getClass().getResourceAsStream("/dependencies_typed.json");
    JSONSchemaProps props = new ObjectMapper().readValue(resourceAsStream, JSONSchemaProps.class);

    Assertions.assertEquals(props, new JSONSchemaPropsBuilder()
      .withType("object")
      .addToDependencies(
        "foo",
        new JSONSchemaPropsOrStringArrayBuilder()
          .withNewSchema()
          .withType("object")
          .endSchema()
          .build())
      .build());
  }

  @Test
  public void itSerializesAdditionalPropertiesTyped() throws JsonProcessingException {
    String expectedJson = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/dependencies_typed.json"), StandardCharsets.UTF_8))
      .lines()
      .collect(Collectors.joining("\n"));

    String outputJson = new ObjectMapper().writeValueAsString(new JSONSchemaPropsBuilder()
      .withType("object")
      .addToDependencies(
        "foo",
        new JSONSchemaPropsOrStringArrayBuilder()
          .withNewSchema()
          .withType("object")
          .endSchema()
          .build())
      .build());

    Assertions.assertEquals(expectedJson, outputJson);
  }
}
