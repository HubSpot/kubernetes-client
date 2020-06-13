package io.fabric8.kubernetes.api.model.apiextensions;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class JSONSchemaPropsOrArraySerDe {
  public static class Serializer extends JsonSerializer<JSONSchemaPropsOrArray> {
    @Override
    public void serialize(JSONSchemaPropsOrArray jsonSchemaPropsOrArray,
                          JsonGenerator jsonGenerator,
                          SerializerProvider serializerProvider) throws IOException {
      if (jsonSchemaPropsOrArray.getJSONSchemas() != null && jsonSchemaPropsOrArray.getJSONSchemas().size() > 0) {
        jsonGenerator.writeStartArray();
        for (JSONSchemaProps schema : jsonSchemaPropsOrArray.getJSONSchemas()) {
          jsonGenerator.writeObject(schema);
        }
        jsonGenerator.writeEndArray();
      } else {
        jsonGenerator.writeObject(jsonSchemaPropsOrArray.getSchema());
      }
    }
  }

  public static class Deserializer extends JsonDeserializer<JSONSchemaPropsOrArray> {

    @Override
    public JSONSchemaPropsOrArray deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
      JSONSchemaPropsOrArrayBuilder builder = new JSONSchemaPropsOrArrayBuilder();
      if (jsonParser.isExpectedStartObjectToken()) {
        builder.withSchema(
          jsonParser.readValueAs(JSONSchemaProps.class));
      } else if (jsonParser.isExpectedStartArrayToken()) {
        builder.withJSONSchemas(jsonParser.<List<JSONSchemaProps>>readValueAs(new TypeReference<List<JSONSchemaProps>>() {}));
      }
      return builder.build();
    }
  }
}
