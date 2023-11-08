package io.fabric8.crd.example.map;

public class Bar {
  private int anInt;

  static Bar getDefault(UpdateRiskLevel riskLevel) {
    switch (riskLevel) {
      case EXPERIMENTAL:
      case LOW:
      case MEDIUM:
      case HIGH:
        return new Bar();
      default:
        throw new RuntimeException("unknown risk level " + riskLevel.name());
    }
  }

  public Bar() {
  }
}
