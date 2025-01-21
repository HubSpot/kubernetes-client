package io.fabric8.crd.example.serialization;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AnnotatedSerializationExample {
  String fOoBar;

  @JsonProperty("fOoBar")
  public String getFOoBar() {
    return fOoBar;
  }
}
