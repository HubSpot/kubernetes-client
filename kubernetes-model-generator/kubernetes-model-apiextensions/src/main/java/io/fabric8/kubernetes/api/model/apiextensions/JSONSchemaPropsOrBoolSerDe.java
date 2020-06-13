package io.fabric8.kubernetes.api.model.apiextensions;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class JSONSchemaPropsOrBoolSerDe {

  public static class Serializer extends JsonSerializer<JSONSchemaPropsOrBool> {
    @Override
    public void serialize(JSONSchemaPropsOrBool jsonSchemaPropsOrBool,
                          JsonGenerator jsonGenerator,
                          SerializerProvider serializerProvider) throws IOException {
      if (jsonSchemaPropsOrBool.getSchema() != null) {
        jsonGenerator.writeObject(jsonSchemaPropsOrBool.getSchema());
      } else {
        jsonGenerator.writeBoolean(jsonSchemaPropsOrBool.getAllows());
      }
    }
  }

  public static class Deserializer extends JsonDeserializer<JSONSchemaPropsOrBool> {

    @Override
    public JSONSchemaPropsOrBool deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
      JSONSchemaPropsOrBoolBuilder builder = new JSONSchemaPropsOrBoolBuilder();
      if (jsonParser.isExpectedStartObjectToken()) {
        builder.withSchema(
          jsonParser.readValueAs(JSONSchemaProps.class));
        builder.withAllows(true);
      } else {
        builder.withAllows(jsonParser.getBooleanValue());
      }
      return builder.build();
    }
  }
}
