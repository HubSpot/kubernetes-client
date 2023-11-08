/**
 * Copyright (C) 2015 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.crd.example.map;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;

public class ContainingMapsSpec {

  private Map<String, List<String>> test = null;

  public Map<String, List<String>> getTest() {
    return test;
  }

  private Map<String, Map<String, List<Boolean>>> test2 = null;

  public Map<String, Map<String, List<Boolean>>> getTest2() {
    return test2;
  }

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

  public static class Bar {
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

    public Bar() {}
  }


  public Map<UpdateRiskLevel, Bar> mapWithEnumKey;

}
