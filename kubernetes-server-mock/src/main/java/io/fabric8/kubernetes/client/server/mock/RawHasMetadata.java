package io.fabric8.kubernetes.client.server.mock;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonDeserializer.None;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;

// Override the default KubernetesSerializer
@JsonDeserialize(using = None.class)
public class RawHasMetadata implements HasMetadata {

  private String apiVersion;
  private String kind;
  private ObjectMeta metadata;

  /**
   * No args constructor for use in serialization
   *
   */
  public RawHasMetadata() {
  }

  public RawHasMetadata(String apiVersion, String kind, ObjectMeta metadata) {
    this.apiVersion = apiVersion;
    this.kind = kind;
    this.metadata = metadata;
  }


  @Override
  @JsonProperty("apiVersion")
  public String getApiVersion() {
    return apiVersion;
  }

  @Override
  @JsonProperty("apiVersion")
  public void setApiVersion(String apiVersion) {
    this.apiVersion = apiVersion;
  }

  @Override
  @JsonProperty("kind")
  public String getKind() {
    return kind;
  }

  @JsonProperty("kind")
  public void setKind(String kind) {
    this.kind = kind;
  }

  @Override
  @JsonProperty("metadata")
  public ObjectMeta getMetadata() {
    return metadata;
  }

  @Override
  @JsonProperty("metadata")
  public void setMetadata(ObjectMeta metadata) {
    this.metadata = metadata;
  }

}
