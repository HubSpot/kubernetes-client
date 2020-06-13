package io.fabric8.kubernetes.api.model.apiextensions;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class JSONSerDe {
  public static class Serializer extends JsonSerializer<JSON> {
    @Override
    public void serialize(JSON json,
                          JsonGenerator jsonGenerator,
                          SerializerProvider serializerProvider) throws IOException {
      if (json.getRaw() != null && !json.getRaw().isEmpty()) {
        jsonGenerator.writeRawValue(json.getRaw());
      } else {
        jsonGenerator.writeNull();
      }
    }
  }

  public static class Deserializer extends JsonDeserializer<JSON> {

    @Override
    public JSON deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
      JsonNode node = jsonParser.readValueAsTree();
      if (node.isNull()) {
        return new JSON(null);
      }
      return new JSON(node.toString());
    }

    @Override
    public JSON getNullValue(DeserializationContext ctxt) throws JsonMappingException {
      return new JSON(null);
    }
  }
}
