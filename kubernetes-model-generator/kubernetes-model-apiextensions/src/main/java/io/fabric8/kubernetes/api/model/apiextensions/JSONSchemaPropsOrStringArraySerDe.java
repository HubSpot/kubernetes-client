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

public class JSONSchemaPropsOrStringArraySerDe {
  public static class Serializer extends JsonSerializer<JSONSchemaPropsOrStringArray> {
    @Override
    public void serialize(JSONSchemaPropsOrStringArray jsonSchemaPropsOrStringArray,
                          JsonGenerator jsonGenerator,
                          SerializerProvider serializerProvider) throws IOException {
      if (jsonSchemaPropsOrStringArray.getProperty() != null && jsonSchemaPropsOrStringArray.getProperty().size() > 0) {
        jsonGenerator.writeStartArray();
        for (String property : jsonSchemaPropsOrStringArray.getProperty()) {
          jsonGenerator.writeObject(property);
        }
        jsonGenerator.writeEndArray();
      } else {
        jsonGenerator.writeObject(jsonSchemaPropsOrStringArray.getSchema());
      }
    }
  }

  public static class Deserializer extends JsonDeserializer<JSONSchemaPropsOrStringArray> {

    @Override
    public JSONSchemaPropsOrStringArray deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
      JSONSchemaPropsOrStringArrayBuilder builder = new JSONSchemaPropsOrStringArrayBuilder();
      if (jsonParser.isExpectedStartObjectToken()) {
        builder.withSchema(
          jsonParser.readValueAs(JSONSchemaProps.class));
      } else if (jsonParser.isExpectedStartArrayToken()) {
        builder.withProperty(jsonParser.<List<String>>readValueAs(new TypeReference<List<String>>() {}));
      }
      return builder.build();
    }
  }
}
