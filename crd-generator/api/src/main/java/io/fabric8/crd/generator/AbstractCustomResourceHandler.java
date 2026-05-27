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
package io.fabric8.crd.generator;

import io.fabric8.crd.generator.annotation.AdditionalPrinterColumn;
import io.fabric8.crd.generator.annotation.AdditionalPrinterColumns;
import io.fabric8.crd.generator.decorator.Decorator;
import io.fabric8.crd.generator.visitor.AdditionalPrinterColumnDetector;
import io.fabric8.crd.generator.visitor.ClassDependenciesVisitor;
import io.fabric8.crd.generator.visitor.LabelSelectorPathDetector;
import io.fabric8.crd.generator.visitor.SpecReplicasPathDetector;
import io.fabric8.crd.generator.visitor.StatusReplicasPathDetector;
import io.fabric8.kubernetes.client.utils.Utils;
import io.sundr.builder.Visitor;
import io.sundr.model.AnnotationRef;
import io.sundr.model.Property;
import io.sundr.model.TypeDef;
import io.sundr.model.TypeDefBuilder;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * This class encapsulates the common behavior between v1beta1 and v1 CRD generation logic. The
 * intent is that each CRD spec version is implemented as a sub-class of this one.
 */
public abstract class AbstractCustomResourceHandler {

  protected final Resources resources;
  private final boolean parallel;

  protected AbstractCustomResourceHandler(Resources resources, boolean parallel) {
    this.resources = resources;
    this.parallel = parallel;
  }

  public void handle(CustomResourceInfo config) {
    final String name = config.crdName();
    final String version = config.version();

    TypeDef def = config.definition();

    SpecReplicasPathDetector specReplicasPathDetector = new SpecReplicasPathDetector();
    StatusReplicasPathDetector statusReplicasPathDetector = new StatusReplicasPathDetector();
    LabelSelectorPathDetector labelSelectorPathDetector = new LabelSelectorPathDetector();
    AdditionalPrinterColumnDetector additionalPrinterColumnDetector = new AdditionalPrinterColumnDetector();

    ClassDependenciesVisitor traversedClassesVisitor = new ClassDependenciesVisitor(config.crClassName(), name);

    List<Visitor<TypeDefBuilder>> visitors = new ArrayList<>();
    if (config.specClassName().isPresent()) {
      visitors.add(specReplicasPathDetector);
    }
    if (config.statusClassName().isPresent()) {
      visitors.add(statusReplicasPathDetector);
    }
    visitors.add(labelSelectorPathDetector);
    visitors.add(additionalPrinterColumnDetector);
    visitors.add(traversedClassesVisitor);

    visitTypeDef(def, visitors);

    addDecorators(config, def, specReplicasPathDetector.getPath(),
        statusReplicasPathDetector.getPath(), labelSelectorPathDetector.getPath());

    Map<String, Property> additionalPrinterColumns = new HashMap<>(additionalPrinterColumnDetector.getProperties());
    additionalPrinterColumns.forEach((path, property) -> {
      Map<String, Object> parameters = property.getAnnotations().stream()
          .filter(a -> a.getClassRef().getName().equals("PrinterColumn")).map(AnnotationRef::getParameters)
          .findFirst().orElse(Collections.emptyMap());
      String type = AbstractJsonSchema.getSchemaTypeFor(property.getTypeRef());
      String column = (String) parameters.get("name");
      if (Utils.isNullOrEmpty(column)) {
        column = property.getName().toUpperCase();
      }
      String description = property.getComments().stream().filter(l -> !l.trim().startsWith("@"))
          .collect(Collectors.joining(" ")).trim();
      String format = (String) parameters.get("format");
      int priority = (int) parameters.getOrDefault("priority", 0);

      resources.decorate(
          getPrinterColumnDecorator(name, version, path, type, column, description, format, priority));
    });

    if(!maybeAddPrinterColumnDecoratorsFromSingleAnnotation(name, version, def)) {
      maybeAddPrinterColumnDecoratorsFromRepeatableAnnotation(name, version, def);
    }
  }

  private TypeDef visitTypeDef(TypeDef def, List<Visitor<TypeDefBuilder>> visitors) {
    if (visitors.isEmpty()) {
      return def;
    }
    if (parallel) {
      return visitTypeDefInParallel(def, visitors);
    } else {
      return visitTypeDefSequentially(def, visitors);
    }
  }

  private TypeDef visitTypeDefSequentially(TypeDef def, List<Visitor<TypeDefBuilder>> visitors) {
    TypeDefBuilder builder = new TypeDefBuilder(def);
    for (Visitor<TypeDefBuilder> visitor : visitors) {
      builder.accept(visitor);
    }
    return builder.build();
  }

  private TypeDef visitTypeDefInParallel(TypeDef def, List<Visitor<TypeDefBuilder>> visitors) {
    final ExecutorService executorService = Executors.newFixedThreadPool(
        Math.min(visitors.size(), Runtime.getRuntime().availableProcessors()));
    try {
      List<CompletableFuture<Void>> futures = new ArrayList<>();
      for (Visitor<TypeDefBuilder> visitor : visitors) {
        futures.add(CompletableFuture.runAsync(() -> {
          // in this case we're not building a new typedef,
          // instead we just need to traverse the object graph.
          TypeDefBuilder builder = new TypeDefBuilder(def);
          builder.accept(visitor);
        }, executorService));
      }
      try {
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
      } catch (InterruptedException interruptedException) {
        Thread.currentThread().interrupt();
      } catch (ExecutionException ex) {
        if (ex.getCause() instanceof RuntimeException) {
          throw (RuntimeException) ex.getCause();
        }
        throw new RuntimeException(ex.getCause());
      }
    } finally {
      executorService.shutdown();
    }
    return def;
  }

  private boolean maybeAddPrinterColumnDecoratorsFromSingleAnnotation(
    String name,
    String version,
    TypeDef def) {
    List<AnnotationRef> annotationRefs = def
      .getAnnotations()
      .stream()
      .filter(annotationRef -> annotationRef
          .getClassRef()
          .getName()
          .equals(AdditionalPrinterColumn.class.getSimpleName()))
      .collect(Collectors.toList());

    if (annotationRefs.isEmpty()) {
      return false;
    }

    addPrinterColumnDecoratorsFromAnnotationRefs(
      name,
      version,
      annotationRefs);

    return true;
  }

  private void addPrinterColumnDecoratorsFromAnnotationRefs(
    String name,
    String version,
    List<AnnotationRef> columns) {
    for (AnnotationRef column : columns) {
      Map<String, Object> params = column.getParameters();

      String path = (String) params.get("jsonPath");
      if (Utils.isNullOrEmpty(path)) {
        continue;
      }

      if (!params.containsKey("type")) {
        continue;
      }
      String type = extractEnumValue(
        params.get("type"),
        AdditionalPrinterColumn.Type.class)
        .getValue();

      String nameOfColumn = (String) params.get("name");
      if (Utils.isNullOrEmpty(nameOfColumn)) {
        nameOfColumn = generateColumnNameFromPath(path);
      }

      String description = (!params.containsKey("getDescription"))
        ? getAdditionalPrinterColumnDefault("getDescription", String.class)
        : (String) params.get("getDescription");

      String format = (!params.containsKey("format"))
        ? getAdditionalPrinterColumnDefault(
            "format",
            AdditionalPrinterColumn.Format.class)
            .getValue()
        : extractEnumValue(params.get("format"), AdditionalPrinterColumn.Format.class)
            .getValue();

      int priority = (!params.containsKey("priority"))
        ? getAdditionalPrinterColumnDefault("priority", Integer.class)
        : (int) params.get("priority");

      resources.decorate(
        getPrinterColumnDecorator(
            name,
            version,
            path,
            type,
            nameOfColumn,
            description,
            format,
            priority));
    }
  }

  @SuppressWarnings("unchecked")
  private <T extends Enum<T>> T extractEnumValue(Object enumObject, Class<T> enumClass) {
    if (enumObject instanceof Enum) {
      return (T) enumObject;
    }

    String stringValue;
    try {
      stringValue =
        enumObject.getClass().getMethod("getSimpleName").invoke(enumObject).toString();
    } catch (
      NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
    Optional<T> maybeEnumValue = Arrays
      .stream(enumClass.getEnumConstants())
      .filter(formatValue -> formatValue.name().equals(stringValue))
      .findFirst();

    if (!maybeEnumValue.isPresent()) {
      throw new IllegalArgumentException(
        "Unknown enum value for " + enumClass.getSimpleName() + ": " + stringValue);
    }

    return maybeEnumValue.get();
  }

  @SuppressWarnings("unchecked")
  private <T> T getAdditionalPrinterColumnDefault(
    String methodName,
    Class<T> returnType) {
    try {
      return (T) AdditionalPrinterColumn.class.getMethod(methodName).getDefaultValue();
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }

  private String generateColumnNameFromPath(String path) {
    String[] splitPath = path.split("\\.");
    return splitPath[splitPath.length - 1].replaceAll("\\W+", "_").toUpperCase();
  }

  private void maybeAddPrinterColumnDecoratorsFromRepeatableAnnotation(
    String name,
    String version,
    TypeDef def) {
    Optional<AnnotationRef> maybeRepeatableAnnotationRef = def
      .getAnnotations()
      .stream()
      .filter(annotationRef -> annotationRef
          .getClassRef()
          .getName()
          .equals(AdditionalPrinterColumns.class.getSimpleName()))
      .findFirst();

    if (!maybeRepeatableAnnotationRef.isPresent()) {
      return;
    }

    Object innerAnnotationsObject = maybeRepeatableAnnotationRef
      .get()
      .getParameters()
      .get("value");

    if (innerAnnotationsObject instanceof AdditionalPrinterColumn[]) {
      addPrinterColumnDecoratorsFromAdditionalPrinterColumns(
          name,
          version,
          Arrays.asList((AdditionalPrinterColumn[]) innerAnnotationsObject));
    } else {
      addPrinterColumnDecoratorsFromAnnotationRefs(
        name,
        version,
        Arrays.asList((AnnotationRef[]) innerAnnotationsObject));
    }
  }

  private void addPrinterColumnDecoratorsFromAdditionalPrinterColumns(
    String name,
    String version,
    List<AdditionalPrinterColumn> columns) {
    for (AdditionalPrinterColumn column : columns) {
      String path = column.jsonPath();
      if (Utils.isNullOrEmpty(path)) {
        continue;
      }

      String nameOfColumn = column.name();
      if (Utils.isNullOrEmpty(nameOfColumn)) {
        nameOfColumn = generateColumnNameFromPath(path);
      }

      resources.decorate(
        getPrinterColumnDecorator(
            name,
            version,
            path,
            column.type().getValue(),
            nameOfColumn,
            column.getDescription(),
            column.format().getValue(),
            column.priority()));
    }
  }

  /**
   * Provides the decorator implementation associated with the CRD generation version.
   *
   * @param name the resource name
   * @param version the associated version
   * @param path the path from which the printer column is extracted
   * @param type the data type of the printer column
   * @param column the name of the column
   * @param description the description of the column
   * @param format the format of the printer column
   * @return the concrete decorator implementing the addition of a printer column to the currently built CRD
   */
  protected abstract Decorator<?> getPrinterColumnDecorator(String name, String version, String path,
      String type, String column, String description, String format, int priority);

  /**
   * Adds all the necessary decorators to build the specific CRD version. For optional paths, see
   * https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.20/#customresourcesubresourcescale-v1-apiextensions-k8s-io
   * These paths
   *
   * @param config the gathered {@link CustomResourceInfo} used as basis for the CRD generation
   * @param def the {@link TypeDef} associated with the {@link io.fabric8.kubernetes.client.CustomResource} from which the CRD
   *        is generated
   * @param specReplicasPath an optionally detected path of field defining spec replicas
   * @param statusReplicasPath an optionally detected path of field defining status replicas
   * @param labelSelectorPath an optionally detected path of field defining `status.selector`
   */
  protected abstract void addDecorators(CustomResourceInfo config, TypeDef def,
      Optional<String> specReplicasPath, Optional<String> statusReplicasPath,
      Optional<String> labelSelectorPath);
}
