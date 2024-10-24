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

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ContainingMapsSpec {

  private Map<String, List<String>> test = null;

  public Map<String, List<String>> getTest() {
    return test;
  }

  private Map<String, Map<String, List<Boolean>>> test2 = null;

  public Map<String, Map<String, List<Boolean>>> getTest2() {
    return test2;
  }

  public enum Foo {
    BAR
  }

  private EnumMap<Foo, String> enumToStringMap;

}
