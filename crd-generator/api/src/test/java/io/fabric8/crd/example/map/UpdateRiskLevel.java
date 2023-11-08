package io.fabric8.crd.example.map;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum UpdateRiskLevel {
  EXPERIMENTAL(0),
  LOW(20),
  MEDIUM(40),
  HIGH(60);

  private final int priority;

  UpdateRiskLevel(int priority) {
    this.priority = priority;
  }

  public int getPriority() {
    return priority;
  }

  @JsonCreator
  public static UpdateRiskLevel fromString(String name) {
    return UpdateRiskLevel.valueOf(name.toUpperCase());
  }
}
