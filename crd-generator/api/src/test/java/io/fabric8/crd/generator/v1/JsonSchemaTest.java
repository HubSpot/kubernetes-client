/*
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
package io.fabric8.crd.generator.v1;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

import io.fabric8.crd.example.annotated.Annotated;
import io.fabric8.crd.example.basic.Basic;
import io.fabric8.crd.example.extraction.CollectionCyclicSchemaSwap;
import io.fabric8.crd.example.extraction.CyclicSchemaSwap;
import io.fabric8.crd.example.extraction.DeeplyNestedSchemaSwaps;
import io.fabric8.crd.example.extraction.Extraction;
import io.fabric8.crd.example.extraction.IncorrectExtraction;
import io.fabric8.crd.example.extraction.IncorrectExtraction2;
import io.fabric8.crd.example.extraction.MultipleSchemaSwaps;
import io.fabric8.crd.example.extraction.NestedSchemaSwap;
import io.fabric8.crd.example.generic.ResourceWithGeneric;
import io.fabric8.crd.example.json.ContainingJson;
import io.fabric8.crd.example.person.Person;
import io.fabric8.crd.example.serialization.AnnotatedSerializationExample;
import io.fabric8.crd.example.serialization.SerializationExample;
import io.fabric8.crd.generator.utils.Types;
import io.fabric8.kubernetes.api.model.AnyType;
import io.fabric8.kubernetes.api.model.apiextensions.v1.JSONSchemaProps;
import io.fabric8.kubernetes.api.model.apiextensions.v1.JSONSchemaPropsBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.v1.ValidationRule;
import io.fabric8.kubernetes.api.model.apiextensions.v1.JSONSchemaPropsOrArray;
import io.sundr.model.TypeDef;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;




class JsonSchemaTest {

  @Test
  void shouldCreatAnyTypeWithoutProperties() {
    TypeDef any = Types.typeDefFrom(AnyType.class);
    JSONSchemaProps schema = JsonSchema.from(any);
    assertNotNull(any);
    assertSchemaHasNumberOfProperties(schema, 0);
    assertTrue(schema.getXKubernetesPreserveUnknownFields());
  }

  @Test
  void shouldCreateJsonSchemaFromClass() {
    TypeDef person = Types.typeDefFrom(Person.class);
    JSONSchemaProps schema = JsonSchema.from(person);
    assertNotNull(schema);
    Map<String, JSONSchemaProps> properties = assertSchemaHasNumberOfProperties(schema, 7);
    final List<String> personTypes = properties.get("type").getEnum().stream().map(JsonNode::asText)
        .collect(Collectors.toList());
    assertEquals(2, personTypes.size());
    assertTrue(personTypes.contains("crazy"));
    assertTrue(personTypes.contains("crazier"));
    final Map<String, JSONSchemaProps> addressProperties = properties.get("addresses").getItems()
        .getSchema().getProperties();
    assertEquals(5, addressProperties.size());
    final List<String> addressTypes = addressProperties.get("type").getEnum().stream()
        .map(JsonNode::asText)
        .collect(Collectors.toList());
    assertEquals(2, addressTypes.size());
    assertTrue(addressTypes.contains("home"));
    assertTrue(addressTypes.contains("work"));

    final TypeDef def = Types.typeDefFrom(Basic.class);
    schema = JsonSchema.from(def);
    assertNotNull(schema);
    properties = schema.getProperties();
    assertNotNull(properties);
    assertEquals(2, properties.size());
    Map<String, JSONSchemaProps> spec = properties.get("spec").getProperties();
    assertEquals("integer", spec.get("myInt").getType());
    assertEquals("integer", spec.get("myLong").getType());
    assertEquals("number", spec.get("myDouble").getType());
    assertEquals("number", spec.get("myFloat").getType());
    Map<String, JSONSchemaProps> status = properties.get("status").getProperties();
    assertEquals("string", status.get("message").getType());
  }

  @Test
  void shouldAugmentPropertiesSchemaFromAnnotations() throws JsonProcessingException {
    TypeDef annotated = Types.typeDefFrom(Annotated.class);
    JSONSchemaProps schema = JsonSchema.from(annotated);
    assertNotNull(schema);
    Map<String, JSONSchemaProps> properties = assertSchemaHasNumberOfProperties(schema, 2);
    final JSONSchemaProps specSchema = properties.get("spec");
    Map<String, JSONSchemaProps> spec = assertSchemaHasNumberOfProperties(specSchema, 21);

    // check descriptions are present
    assertTrue(spec.containsKey("from-field"));
    JSONSchemaProps prop = spec.get("from-field");
    assertEquals("from-field-description", prop.getDescription());
    assertTrue(spec.containsKey("from-getter"));
    prop = spec.get("from-getter");
    assertEquals("from-getter-description", prop.getDescription());

    // fields without description annotation shouldn't have them
    assertTrue(spec.containsKey("unnamed"));
    assertNull(spec.get("unnamed").getDescription());
    assertTrue(spec.containsKey("emptySetter"));
    assertNull(spec.get("emptySetter").getDescription());
    assertTrue(spec.containsKey("anEnum"));

    Function<String, JSONSchemaPropsBuilder> type = t -> new JSONSchemaPropsBuilder().withType(t);
    assertEquals(type.apply("integer").withMinimum(-5.0).build(), spec.get("min"));
    assertEquals(type.apply("integer").withMaximum(5.0).build(), spec.get("max"));
    assertEquals(type.apply("string").withPattern("\\b[1-9]\\b").build(), spec.get("singleDigit"));
    assertEquals(type.apply("string").withNullable(true).build(), spec.get("nullable"));
    assertEquals(type.apply("string").withDefault(TextNode.valueOf("my-value")).build(), spec.get("defaultValue"));
    assertEquals(type.apply("string").withDefault(TextNode.valueOf("my-value2")).build(), spec.get("defaultValue2"));
    assertEquals(type.apply("string").withEnum(TextNode.valueOf("non"), TextNode.valueOf("oui")).build(), spec.get("anEnum"));
    assertEquals(type.apply("string").build(), spec.get("bool"));
    assertEquals(type.apply("string").build(), spec.get("num"));
    assertEquals(type.apply("string").build(), spec.get("numFloat"));
    assertEquals(type.apply("string").build(), spec.get("numInt"));
    assertEquals(type.apply("string").withFormat("date-time").build(), spec.get("issuedAt"));

    // check required list, should register properties with their modified name if needed
    final List<String> required = specSchema.getRequired();
    assertEquals(3, required.size());
    assertTrue(required.contains("emptySetter"));
    assertTrue(required.contains("emptySetter2"));
    assertTrue(required.contains("from-getter"));

    // check ignored fields
    assertFalse(spec.containsKey("ignoredFoo"));
    assertFalse(spec.containsKey("ignoredBar"));

    final JSONSchemaProps k8sValidationProps = spec.get("kubernetesValidationRule");
    final List<ValidationRule> k8sValidationRulesSingle = k8sValidationProps.getXKubernetesValidations();
    assertNotNull(k8sValidationRulesSingle);
    assertEquals(1, k8sValidationRulesSingle.size());
    assertEquals("self.startwith('prefix-')", k8sValidationRulesSingle.get(0).getRule());
    assertEquals("kubernetesValidationRule must start with prefix 'prefix-'", k8sValidationRulesSingle.get(0).getMessage());
    assertNull(k8sValidationRulesSingle.get(0).getMessageExpression());
    assertNull(k8sValidationRulesSingle.get(0).getReason());
    assertNull(k8sValidationRulesSingle.get(0).getFieldPath());
    assertNull(k8sValidationRulesSingle.get(0).getOptionalOldSelf());

    final JSONSchemaProps kubernetesValidationsRepeated = spec.get("kubernetesValidationRules");
    final List<ValidationRule> kubernetesValidationsRepeatedRules = kubernetesValidationsRepeated.getXKubernetesValidations();
    assertNotNull(kubernetesValidationsRepeatedRules);
    assertEquals(3, kubernetesValidationsRepeatedRules.size());
    assertEquals("first.rule", kubernetesValidationsRepeatedRules.get(0).getRule());
    assertNull(kubernetesValidationsRepeatedRules.get(0).getFieldPath());
    assertNull(kubernetesValidationsRepeatedRules.get(0).getReason());
    assertNull(kubernetesValidationsRepeatedRules.get(0).getMessage());
    assertNull(kubernetesValidationsRepeatedRules.get(0).getMessageExpression());
    assertNull(kubernetesValidationsRepeatedRules.get(0).getOptionalOldSelf());
    assertEquals("second.rule", kubernetesValidationsRepeatedRules.get(1).getRule());
    assertNull(kubernetesValidationsRepeatedRules.get(1).getFieldPath());
    assertNull(kubernetesValidationsRepeatedRules.get(1).getReason());
    assertNull(kubernetesValidationsRepeatedRules.get(1).getMessage());
    assertNull(kubernetesValidationsRepeatedRules.get(1).getMessageExpression());
    assertNull(kubernetesValidationsRepeatedRules.get(1).getOptionalOldSelf());

    // check descriptions are present
    assertTrue(spec.containsKey("renamedField"));
    assertFalse(spec.containsKey("fieldToRename"));
  }

  @Test
  void shouldProduceKubernetesPreserveFields() {
    TypeDef containingJson = Types.typeDefFrom(ContainingJson.class);
    JSONSchemaProps schema = JsonSchema.from(containingJson);
    assertNotNull(schema);
    Map<String, JSONSchemaProps> properties = assertSchemaHasNumberOfProperties(schema, 2);
    final JSONSchemaProps specSchema = properties.get("spec");
    Map<String, JSONSchemaProps> spec = assertSchemaHasNumberOfProperties(specSchema, 3);

    // check preserve unknown fields is present
    assertTrue(spec.containsKey("free"));
    JSONSchemaProps freeField = spec.get("free");

    assertNull(freeField.getType());
    assertTrue(freeField.getXKubernetesPreserveUnknownFields());

    assertTrue(spec.containsKey("field"));
    JSONSchemaProps field = spec.get("field");

    assertEquals("integer", field.getType());
    assertNull(field.getXKubernetesPreserveUnknownFields());

    assertTrue(spec.containsKey("foo"));
    JSONSchemaProps fooField = spec.get("foo");

    assertEquals("object", fooField.getType());
    assertTrue(fooField.getXKubernetesPreserveUnknownFields());

    Map<String, JSONSchemaProps> fooProperties =  fooField.getProperties();
    JSONSchemaProps configAsMapField = fooProperties.get("configAsMap");

    assertNotNull(configAsMapField);
    assertTrue(configAsMapField.getXKubernetesPreserveUnknownFields());
  }

  @Test
  void shouldExtractPropertiesSchemaFromExtractValueAnnotation() {
    TypeDef extraction = Types.typeDefFrom(Extraction.class);
    JSONSchemaProps schema = JsonSchema.from(extraction);
    assertNotNull(schema);
    Map<String, JSONSchemaProps> properties = assertSchemaHasNumberOfProperties(schema, 2);
    final JSONSchemaProps specSchema = properties.get("spec");
    Map<String, JSONSchemaProps> spec = assertSchemaHasNumberOfProperties(specSchema, 3);

    // check typed SchemaFrom
    JSONSchemaProps foo = spec.get("foo");
    Map<String, JSONSchemaProps> fooProps = foo.getProperties();
    assertNotNull(fooProps);

    // you can change everything
    assertEquals("integer", fooProps.get("BAZ").getType());
    assertTrue(foo.getRequired().contains("BAZ"));

    // you can exclude fields
    assertNull(fooProps.get("baz"));

    // check typed SchemaSwap
    JSONSchemaProps bar = spec.get("bar");
    Map<String, JSONSchemaProps> barProps = bar.getProperties();
    assertNotNull(barProps);
    assertTrue(bar.getXKubernetesPreserveUnknownFields());

    // you can change everything
    assertEquals("integer", barProps.get("BAZ").getType());
    assertTrue(bar.getRequired().contains("BAZ"));

    // you can exclude fields
    assertNull(barProps.get("baz"));

    // verify that x-kubernetes-preserve-unknown-fields isn't on parent object when
    // nested object is annotated with @PreserveUnknownFields
    JSONSchemaProps qux = spec.get("qux");
    Map<String, JSONSchemaProps> quxProps = qux.getProperties();
    assertTrue(quxProps.get("foo").getXKubernetesPreserveUnknownFields());
    assertNull(qux.getXKubernetesPreserveUnknownFields());
  }

  @Test
  void shouldExtractPropertiesSchemaFromSchemaSwapAnnotations() {
    TypeDef extraction = Types.typeDefFrom(MultipleSchemaSwaps.class);
    JSONSchemaProps schema = JsonSchema.from(extraction);
    assertNotNull(schema);
    Map<String, JSONSchemaProps> properties = assertSchemaHasNumberOfProperties(schema, 2);
    final JSONSchemaProps specSchema = properties.get("spec");
    Map<String, JSONSchemaProps> spec = assertSchemaHasNumberOfProperties(specSchema, 4);

    // 'first' is replaced by SchemaSwap from int to string
    JSONSchemaProps first = spec.get("first");
    assertPropertyHasType(first, "shouldBeString", "string");

    // 'second' is replaced by the same SchemaSwap that is applied multiple times
    JSONSchemaProps second = spec.get("second");
    assertPropertyHasType(second, "shouldBeString", "string");

    // 'third' is replaced by another SchemaSwap
    JSONSchemaProps third = spec.get("third");
    assertPropertyHasType(third, "shouldBeInt", "integer");

    // 'fourth' is replaced by another SchemaSwap and its property deleted
    JSONSchemaProps fourth = spec.get("fourth");
    Map<String, JSONSchemaProps> properties4 = fourth.getProperties();
    assertNotNull(properties);
    assertTrue(properties4.isEmpty());
  }

  @Test
  void shouldApplySchemaSwapsMultipleTimesInDeepClassHierarchy() {
    TypeDef extraction = Types.typeDefFrom(DeeplyNestedSchemaSwaps.class);
    JSONSchemaProps schema = JsonSchema.from(extraction);
    assertNotNull(schema);
    Map<String, JSONSchemaProps> properties = assertSchemaHasNumberOfProperties(schema, 2);
    Map<String, JSONSchemaProps> spec = assertSchemaHasNumberOfProperties(properties.get("spec"), 2);

    assertPropertyHasType(spec.get("myObject"), "shouldBeString", "string");
    Map<String, JSONSchemaProps> level1 = assertSchemaHasNumberOfProperties(spec.get("level1"), 3);

    assertPropertyHasType(level1.get("myObject"), "shouldBeString", "string");
    List<Map<String, JSONSchemaProps>> levels2 = new ArrayList<>();
    levels2.add(assertSchemaHasNumberOfProperties(level1.get("level2a"), 3));
    levels2.add(assertSchemaHasNumberOfProperties(level1.get("level2b"), 3));

    for (Map<String, JSONSchemaProps> level2 : levels2) {
      assertPropertyHasType(level2.get("myObject1"), "shouldBeString", "string");
      assertPropertyHasType(level2.get("myObject2"), "shouldBeString", "string");

      Map<String, JSONSchemaProps> level3 = assertSchemaHasNumberOfProperties(level2.get("level3"), 2);
      assertPropertyHasType(level3.get("myObject1"), "shouldBeString", "string");
      assertPropertyHasType(level3.get("myObject2"), "shouldBeString", "string");
    }
  }

  @Test
  void shouldApplyCyclicSchemaSwaps() {
    TypeDef extraction = Types.typeDefFrom(CyclicSchemaSwap.class);
    JSONSchemaProps schema = JsonSchema.from(extraction);
    assertNotNull(schema);

    Map<String, JSONSchemaProps> properties = assertSchemaHasNumberOfProperties(schema, 2);
    Map<String, JSONSchemaProps> spec = assertSchemaHasNumberOfProperties(properties.get("spec"), 3);

    // the collection should emit a single level then terminate with void
    assertNull(spec.get("roots").getItems().getSchema().getProperties().get("level").getProperties().get("level"));

    assertPropertyHasType(spec.get("myObject"), "value", "integer");

    // the field should emit a single level then terminate with void
    assertNull(spec.get("root").getProperties().get("level").getProperties().get("level"));
  }

  @Test
  void shouldApplyCollectionCyclicSchemaSwaps() {
    TypeDef extraction = Types.typeDefFrom(CollectionCyclicSchemaSwap.class);
    JSONSchemaProps schema = JsonSchema.from(extraction);
    assertNotNull(schema);

    Map<String, JSONSchemaProps> properties = assertSchemaHasNumberOfProperties(schema, 2);
    Map<String, JSONSchemaProps> spec = assertSchemaHasNumberOfProperties(properties.get("spec"), 2);

    assertPropertyHasType(spec.get("myObject"), "value", "integer");
    Map<String, JSONSchemaProps> level1 = assertSchemaHasNumberOfProperties(spec.get("levels").getItems().getSchema(), 2);

    assertPropertyHasType(level1.get("myObject"), "value", "integer");
    Map<String, JSONSchemaProps> level2 = assertSchemaHasNumberOfProperties(level1.get("levels").getItems().getSchema(), 2);

    assertPropertyHasType(level2.get("myObject"), "value", "integer");
    Map<String, JSONSchemaProps> level3 = assertSchemaHasNumberOfProperties(level2.get("levels").getItems().getSchema(), 2);

    assertPropertyHasType(level3.get("myObject"), "value", "integer");
    // should terminate at the 3rd level with any - this is probably not quite the behavior we want
    // targeting collection properties with a non-collection terminal seems problematic
    JSONSchemaProps terminal = level3.get("levels");
    assertNull(terminal.getItems());
    assertTrue(terminal.getXKubernetesPreserveUnknownFields());
    assertSchemaHasNumberOfProperties(terminal, 0);
  }

  @Test
  void shouldThrowIfSchemaSwapHasUnmatchedField() {
    TypeDef incorrectExtraction = Types.typeDefFrom(IncorrectExtraction.class);
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> JsonSchema.from(incorrectExtraction));
    assertEquals(
        "Unmatched SchemaSwaps: @SchemaSwap(originalType=io.fabric8.crd.example.extraction.ExtractionSpec, fieldName=\"FOO\", targetType=io"
            + ".fabric8.crd.example.extraction.FooExtractor) on io.fabric8.crd.example.extraction.IncorrectExtraction",
        exception.getMessage());
  }

  @Test
  void shouldThrowIfSchemaSwapHasUnmatchedClass() {
    TypeDef incorrectExtraction2 = Types.typeDefFrom(IncorrectExtraction2.class);
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> JsonSchema.from(incorrectExtraction2));
    assertEquals(
        "Unmatched SchemaSwaps: @SchemaSwap(originalType=io.fabric8.crd.example.basic.BasicSpec, fieldName=\"bar\", targetType=io.fabric8.crd"
            + ".example.extraction.FooExtractor) on io.fabric8.crd.example.extraction.IncorrectExtraction2",
        exception.getMessage());
  }

  @Test
  void shouldThrowIfSchemaSwapNested() {
    TypeDef nested = Types.typeDefFrom(NestedSchemaSwap.class);
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> JsonSchema.from(nested));
    assertEquals(
        "Nested SchemaSwap: @SchemaSwap(originalType=io.fabric8.crd.example.extraction.NestedSchemaSwap.End, fieldName=\"value\", targetType=java.lang.Void) "
            + "on io.fabric8.crd.example.extraction.NestedSchemaSwap.Intermediate",
        exception.getMessage());
  }

  private static Map<String, JSONSchemaProps> assertSchemaHasNumberOfProperties(JSONSchemaProps specSchema, int expected) {
    Map<String, JSONSchemaProps> spec = specSchema.getProperties();
    assertEquals(expected, spec.size());
    return spec;
  }

  private static void assertPropertyHasType(JSONSchemaProps spec, String name, String expectedType) {
    Map<String, JSONSchemaProps> properties = spec.getProperties();
    assertNotNull(properties);
    JSONSchemaProps property = properties.get(name);
    assertNotNull(property, "Property " + name + " should exist");
    assertEquals(expectedType, property.getType(), "Property " + name + " should have expected type");
  }

  @Test
  void shouldProcessGenericClasses() {
    TypeDef resourceWithGeneric = Types.typeDefFrom(ResourceWithGeneric.class);
    JSONSchemaProps schema = JsonSchema.from(resourceWithGeneric);
    assertNotNull(schema);

    Map<String, JSONSchemaProps> properties = schema.getProperties();
    assertEquals(2, properties.size());

    final JSONSchemaProps specSchema = properties.get("spec");
    Map<String, JSONSchemaProps> spec = specSchema.getProperties();
    assertEquals(3, spec.size());

    JSONSchemaProps foo = spec.get("foo");
    assertNotNull(foo);
    Map<String, JSONSchemaProps> fooProps = foo.getProperties();
    assertNotNull(fooProps);
    assertEquals("string", fooProps.get("bar").getType());

    JSONSchemaProps baz = spec.get("baz");
    assertNotNull(baz);
    Map<String, JSONSchemaProps> bazProps = baz.getProperties();
    assertNotNull(bazProps);
    assertEquals("integer", bazProps.get("bar").getType());

    JSONSchemaProps qux = spec.get("qux");
    assertNotNull(qux);
    Map<String, JSONSchemaProps> quxProps = qux.getProperties();
    assertEquals(2, quxProps.size());

    JSONSchemaProps quux = quxProps.get("quux");
    assertNotNull(quux);
    Map<String, JSONSchemaProps> quuxProps = quux.getProperties();
    assertNotNull(quuxProps);
    assertEquals("string", quuxProps.get("bar").getType());

    JSONSchemaProps corge = quxProps.get("corge");
    assertNotNull(corge);
    JSONSchemaPropsOrArray corgeItems = corge.getItems();
    assertNotNull(corgeItems);
    JSONSchemaProps corgeItemsProps = corgeItems.getSchema();
    assertNotNull(corgeItemsProps);
    assertEquals("string", corgeItemsProps.getType());
  }

  @Test
  void shouldProduceDeserializableNames() {
    TypeDef serializationExample = Types.typeDefFrom(SerializationExample.class);
    JSONSchemaProps schema = JsonSchema.from(serializationExample);
    assertNotNull(schema);

    Map<String, JSONSchemaProps> properties = schema.getProperties();
    assertEquals(3, properties.size());

    assertTrue(properties.containsKey("url"));
    assertTrue(properties.containsKey("fooBar"));
    assertTrue(properties.containsKey("normalCase"));
  }

  @Test
  void itShouldNotChangeAnnotatedNames() {
    TypeDef annotatedSerializationExample = Types.typeDefFrom(AnnotatedSerializationExample.class);
    JSONSchemaProps schema = JsonSchema.from(annotatedSerializationExample);
    assertNotNull(schema);

    Map<String, JSONSchemaProps> properties = schema.getProperties();
    assertEquals(1, properties.size());

    assertTrue(properties.containsKey("fOoBar"));
  }

  @Test
  void shouldSetFormatFromFormatAnnotation() {
    TypeDef annotated = Types.typeDefFrom(Annotated.class);
    JSONSchemaProps schema = JsonSchema.from(annotated);
    assertNotNull(schema);
    Map<String, JSONSchemaProps> properties = assertSchemaHasNumberOfProperties(schema, 2);
    final JSONSchemaProps specSchema = properties.get("spec");
    Map<String, JSONSchemaProps> spec = specSchema.getProperties();

    // Check that the issuedAt field has the correct format
    assertTrue(spec.containsKey("issuedAt"));
    JSONSchemaProps issuedAtProp = spec.get("issuedAt");
    assertEquals("date-time", issuedAtProp.getFormat());
    assertEquals("string", issuedAtProp.getType());
  }
}
