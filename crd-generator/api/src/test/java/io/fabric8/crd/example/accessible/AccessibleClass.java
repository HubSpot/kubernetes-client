package io.fabric8.crd.example.accessible;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AccessibleClass {
  private int gettable;
  private String invisible;
  public String visible;
  public transient volatile Foo foo = new Foo();

  @JsonProperty("gettable")
  public int getGettable() {
    return gettable;
  }

  public boolean isTruthy() {
    return true;
  }

  public static class Foo {
    public String bar;
  }
}
