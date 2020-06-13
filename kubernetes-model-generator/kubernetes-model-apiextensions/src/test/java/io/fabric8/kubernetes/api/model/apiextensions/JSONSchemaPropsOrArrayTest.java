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

public class JSONSchemaPropsOrArrayTest {
  @Test
  public void itDeserializesAdditionalPropertiesTrue() throws IOException {
    InputStream resourceAsStream = getClass().getResourceAsStream("/items_array.json");
    JSONSchemaProps props = new ObjectMapper().readValue(resourceAsStream, JSONSchemaProps.class);

    Assertions.assertEquals(props, new JSONSchemaPropsBuilder()
      .withType("object")
      .withItems(new JSONSchemaPropsOrArrayBuilder()
        .withJSONSchemas(
          new JSONSchemaPropsBuilder().withType("object").build(),
          new JSONSchemaPropsBuilder().withType("string").build())
        .build())
      .build());
  }

  @Test
  public void itSerializesAdditionalPropertiesTrue() throws JsonProcessingException {
    String expectedJson = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/items_array.json"), StandardCharsets.UTF_8))
      .lines()
      .collect(Collectors.joining("\n"));

    String outputJson = new ObjectMapper().writeValueAsString(new JSONSchemaPropsBuilder()
      .withType("object")
      .withItems(new JSONSchemaPropsOrArrayBuilder()
        .withJSONSchemas(
          new JSONSchemaPropsBuilder().withType("object").build(),
          new JSONSchemaPropsBuilder().withType("string").build())
        .build())
      .build());

    Assertions.assertEquals(expectedJson, outputJson);
  }

  @Test
  public void itDeserializesAdditionalPropertiesTyped() throws IOException {
    InputStream resourceAsStream = getClass().getResourceAsStream("/items_typed.json");
    JSONSchemaProps props = new ObjectMapper().readValue(resourceAsStream, JSONSchemaProps.class);

    Assertions.assertEquals(props, new JSONSchemaPropsBuilder()
      .withType("object")
      .withItems(new JSONSchemaPropsOrArrayBuilder().withSchema(new JSONSchemaPropsBuilder().withType("object").build()).build())
      .build());
  }

  @Test
  public void itSerializesAdditionalPropertiesTyped() throws JsonProcessingException {
    String expectedJson = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/items_typed.json"), StandardCharsets.UTF_8))
      .lines()
      .collect(Collectors.joining("\n"));

    String outputJson = new ObjectMapper().writeValueAsString(new JSONSchemaPropsBuilder()
      .withType("object")
      .withItems(new JSONSchemaPropsOrArrayBuilder().withSchema(new JSONSchemaPropsBuilder().withType("object").build()).build())
      .build());

    Assertions.assertEquals(expectedJson, outputJson);
  }
}
