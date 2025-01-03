package io.fabric8.crd.example.extraction;

import io.fabric8.crd.generator.annotation.PreserveUnknownFields;

public class Qux {
  @PreserveUnknownFields
  public Foo foo;
}
