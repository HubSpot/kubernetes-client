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

public class JSONTest {
  @Test
  public void itDeserializesAdditionalPropertiesTrue() throws IOException {
    InputStream resourceAsStream = getClass().getResourceAsStream("/json_fields.json");
    JSONSchemaProps props = new ObjectMapper().readValue(resourceAsStream, JSONSchemaProps.class);

    Assertions.assertEquals(props, new JSONSchemaPropsBuilder()
      .withType("object")
      .addNewEnum("\"one\"")
      .addNewEnum("\"two\"")
      .addNewEnum("true")
      .addToEnum(new JSON(null))
      .addToEnum(new JSON(null))
      .withNewExample().withRaw("\"foo\"").endExample()
      .withNewDefault().withRaw("3").endDefault()
      .build());
  }

  @Test
  public void itSerializesAdditionalPropertiesTrue() throws JsonProcessingException {
    String expectedJson = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/json_fields.json"), StandardCharsets.UTF_8))
      .lines()
      .collect(Collectors.joining("\n"));

    String outputJson = new ObjectMapper().writeValueAsString(new JSONSchemaPropsBuilder()
      .withType("object")
      .addNewEnum("\"one\"")
      .addNewEnum("\"two\"")
      .addNewEnum("true")
      .addNewEnum("null")
      .addToEnum(new JSON(null))
      .withNewExample().withRaw("\"foo\"").endExample()
      .withNewDefault().withRaw("3").endDefault()
      .build());

    Assertions.assertEquals(expectedJson, outputJson);
  }
}
