package io.fabric8.crd.example.generic;

public class ResourceWithGenericSpec {
  Generic<String> foo;
  Generic<Integer> baz;
  NestedGeneric<String> qux;
}
