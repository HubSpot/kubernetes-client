package io.fabric8.crd.example.map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_EMPTY)
public interface RiskLevelSettingsIF {
  default int getConcurrency() {
    return 1;
  }

  static RiskLevelSettings getDefault(UpdateRiskLevel riskLevel) {
    switch (riskLevel) {
      case EXPERIMENTAL:
      case LOW:
      case MEDIUM:
      case HIGH:
        return RiskLevelSettings.builder().setConcurrency(1).build();
      default:
        throw new RuntimeException("unknown risk level " + riskLevel.name());
    }
  }
}

