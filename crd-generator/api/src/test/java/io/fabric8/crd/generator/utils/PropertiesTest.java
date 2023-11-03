package io.fabric8.crd.generator.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.fabric8.crd.example.accessible.AccessibleClass;
import io.fabric8.crd.example.webserver.WebServerWithStatusProperty;
import io.sundr.model.Property;
import io.sundr.model.TypeDef;

public class PropertiesTest {
  @Test
  public void testClassWithNoPublicProperties() {
    TypeDef def = Types.typeDefFrom(WebServerWithStatusProperty.class);
    List<Property> props = Properties.getVisibleProperties(def);
    assertEquals(0, props.size());
  }

  private Property findByName(List<Property> props, String name) {
    return props.stream()
      .filter(p -> p.getName().equals(name))
      .findAny()
      .get();
  }

  @Test
  public void testClassWithMixedVisibility() {
    TypeDef def = Types.typeDefFrom(AccessibleClass.class);
    List<Property> props = Properties.getVisibleProperties(def);
    assertEquals(3, props.size());

    Property publicProp = findByName(props, "visible");
    assertTrue(publicProp.isPublic());

    Property getterProp = findByName(props, "gettable");
    assertTrue(getterProp.isPrivate());
    // Got annotations from getter method
    assertEquals(1, getterProp.getAnnotations().size());

    Property boolProp = findByName(props, "truthy");
    assertFalse(boolProp.isPrivate());
    // Got annotations from getter method
    assertEquals(0, boolProp.getAnnotations().size());
  }
}
