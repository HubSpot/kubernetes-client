package io.fabric8.crd.example.joke;

import io.fabric8.crd.generator.annotation.AdditionalPrinterColumn;
import io.fabric8.crd.generator.annotation.AdditionalPrinterColumn.Type;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.ShortNames;
import io.fabric8.kubernetes.model.annotation.Version;

@Group("samples.javaoperatorsdk.io")
@Version("v1alpha1")
@ShortNames("ajr")
@AdditionalPrinterColumn(name = "Age", jsonPath = ".metadata.creationTimestamp", type = Type.DATE)
@AdditionalPrinterColumn(jsonPath = ".metadata.namespace")
public class AnotherJokeRequest extends CustomResource<JokeRequestSpec, JokeRequestStatus> implements Namespaced {
}
