package io.fabric8.crd.generator.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.sundr.model.AnnotationRef;
import io.sundr.model.Method;
import io.sundr.model.Property;
import io.sundr.model.PropertyBuilder;
import io.sundr.model.TypeDef;

// TODO: We should use the visibility settings from the object mapper that is being used.
public class Properties {
  // Get a list of visible properties.
  // Returns public fields as well as private fields that are accessible
  // through get<fieldName> or is<fieldName> methods
  public static List<Property> getVisibleProperties(TypeDef def) {
    List<Property> props = new ArrayList<>();
    if (def.getFullyQualifiedName().startsWith("java.")) {
      return props;
    }

    // Get public properties
    for (Property p : def.getProperties()) {
      if (!p.isPublic() || p.isTransient()) {
        continue;
      }
      props.add(p);
    }

    // Get getters
    for (Method m : def.getMethods()) {
      if (
        !m.isPublic() || m.isTransient() || m.isStatic() ||
        m.getName().equals("getClass") ||
        !(m.getName().startsWith("get") || m.getName().startsWith("is"))
      ) {
        continue;
      }

      String propName = propertyNameForMethod(m);
      // Find the property for the getter.
      boolean found = false;
      for (Property p : def.getProperties()) {
        if (p.getName().equals(propName)) {

          List<AnnotationRef> mergedAnnotations = Stream.concat(
              p.getAnnotations().stream(),
              m.getAnnotations().stream()
            )
            .collect(Collectors.toList());

          Property newProp =  new PropertyBuilder(p).withAnnotations(mergedAnnotations).build();
          props.add(newProp);

          found = true;
          break;
        }
      }

      if (!found) {
        props.add(propertyFromMethod(m));
      }
    }

    return props;
  }

  private static Property propertyFromMethod(Method m) {
    return new PropertyBuilder()
      .withName(propertyNameForMethod(m))
      .withTypeRef(m.getReturnType())
      .withAnnotations(m.getAnnotations())
      .withModifiers(m.getModifiers())
      .withAttributes(m.getAttributes())
      .withComments(m.getComments())
      .build();
  }

  private static String propertyNameForMethod(Method method) {
    String name = method.getName();
    if (name.equals("get") || name.equals("is")) {
      return name;
    }

    if (name.startsWith("get")) {
      return lowerFirstLetter(removePrefix(name, "get"));
    } else if (name.startsWith("is")) {
      return lowerFirstLetter(removePrefix(name, "is"));
    }
    return name;
  }

  private static String removePrefix(String text, String prefix) {
    if (text.startsWith(prefix)) {
      return text.substring(prefix.length());
    }
    return text;
  }

  private static String lowerFirstLetter(String text) {
    if (text.isEmpty()) {
      return text;
    }

    return Character.toLowerCase(text.charAt(0)) + text.substring(1);
  }
}
