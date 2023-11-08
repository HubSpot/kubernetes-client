package io.fabric8.crd.example.map;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Immutable implementation of {@link RiskLevelSettingsIF}.
 * <p>
 * Use the builder to create immutable instances:
 * {@code RiskLevelSettings.builder()}.
 */
@SuppressWarnings({"all"})
public final class RiskLevelSettings
  implements RiskLevelSettingsIF {
  private final int concurrency;

  private RiskLevelSettings(RiskLevelSettings.Builder builder) {
    this.concurrency = builder.concurrencyIsSet()
      ? builder.concurrency
      : RiskLevelSettingsIF.super.getConcurrency();
  }

  private RiskLevelSettings(int concurrency) {
    this.concurrency = concurrency;
  }

  /**
   * @return The value of the {@code concurrency} attribute
   */
  @JsonProperty
  @Override
  public int getConcurrency() {
    return concurrency;
  }

  /**
   * Copy the current immutable object by setting a value for the {@link RiskLevelSettingsIF#getConcurrency() concurrency} attribute.
   * A value equality check is used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for concurrency
   * @return A modified copy of the {@code this} object
   */
  public final RiskLevelSettings withConcurrency(int value) {
    if (this.concurrency == value) return this;
    return new RiskLevelSettings(value);
  }

  /**
   * This instance is equal to all instances of {@code RiskLevelSettings} that have equal attribute values.
   * @return {@code true} if {@code this} is equal to {@code another} instance
   */
  @Override
  public boolean equals(Object another) {
    if (this == another) return true;
    return another instanceof RiskLevelSettings
      && equalTo((RiskLevelSettings) another);
  }

  private boolean equalTo(RiskLevelSettings another) {
    return concurrency == another.concurrency;
  }

  /**
   * Computes a hash code from attributes: {@code concurrency}.
   * @return hashCode value
   */
  @Override
  public int hashCode() {
    int h = 5381;
    h += (h << 5) + concurrency;
    return h;
  }

  /**
   * Prints the immutable value {@code RiskLevelSettings} with attribute values.
   * @return A string representation of the value
   */
  @Override
  public String toString() {
    return "RiskLevelSettings{"
      + "concurrency=" + concurrency
      + "}";
  }

  /**
   * Utility type used to correctly read immutable object from JSON representation.
   * @deprecated Do not use this type directly, it exists only for the <em>Jackson</em>-binding infrastructure
   */
  @Deprecated
  @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE)
  static final class Json
    implements RiskLevelSettingsIF {
    int concurrency;
    boolean concurrencyIsSet;
    @JsonProperty
    public void setConcurrency(int concurrency) {
      this.concurrency = concurrency;
      this.concurrencyIsSet = true;
    }
    @Override
    public int getConcurrency() { throw new UnsupportedOperationException(); }
  }

  /**
   * @param json A JSON-bindable data structure
   * @return An immutable value type
   * @deprecated Do not use this method directly, it exists only for the <em>Jackson</em>-binding infrastructure
   */
  @Deprecated
  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  static RiskLevelSettings fromJson(Json json) {
    RiskLevelSettings.Builder builder = RiskLevelSettings.builder();
    if (json.concurrencyIsSet) {
      builder.setConcurrency(json.concurrency);
    }
    return builder.build();
  }

  /**
   * Creates an immutable copy of a {@link RiskLevelSettingsIF} value.
   * Uses accessors to get values to initialize the new immutable instance.
   * If an instance is already immutable, it is returned as is.
   * @param instance The instance to copy
   * @return A copied immutable RiskLevelSettings instance
   */
  public static RiskLevelSettings copyOf(RiskLevelSettingsIF instance) {
    if (instance instanceof RiskLevelSettings) {
      return (RiskLevelSettings) instance;
    }
    return RiskLevelSettings.builder()
      .from(instance)
      .build();
  }

  /**
   * Creates a builder for {@link RiskLevelSettings RiskLevelSettings}.
   * <pre>
   * RiskLevelSettings.builder()
   *    .setConcurrency(int) // optional {@link RiskLevelSettingsIF#getConcurrency() concurrency}
   *    .build();
   * </pre>
   * @return A new RiskLevelSettings builder
   */
  public static RiskLevelSettings.Builder builder() {
    return new RiskLevelSettings.Builder();
  }

  /**
   * Builds instances of type {@link RiskLevelSettings RiskLevelSettings}.
   * Initialize attributes and then invoke the {@link #build()} method to create an
   * immutable instance.
   * <p><em>{@code Builder} is not thread-safe and generally should not be stored in a field or collection,
   * but instead used immediately to create instances.</em>
   */
  public static final class Builder {
    private static final long OPT_BIT_CONCURRENCY = 0x1L;
    private long optBits;

    private int concurrency;

    private Builder() {
    }

    /**
     * Fill a builder with attribute values from the provided {@code RiskLevelSettingsIF} instance.
     * Regular attribute values will be replaced with those from the given instance.
     * Absent optional values will not replace present values.
     * @param instance The instance from which to copy values
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder from(RiskLevelSettingsIF instance) {
      Objects.requireNonNull(instance, "instance");
      setConcurrency(instance.getConcurrency());
      return this;
    }

    /**
     * Initializes the value for the {@link RiskLevelSettingsIF#getConcurrency() concurrency} attribute.
     * <p><em>If not set, this attribute will have a default value as returned by the initializer of {@link RiskLevelSettingsIF#getConcurrency() concurrency}.</em>
     * @param concurrency The value for concurrency
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder setConcurrency(int concurrency) {
      this.concurrency = concurrency;
      optBits |= OPT_BIT_CONCURRENCY;
      return this;
    }

    /**
     * Builds a new {@link RiskLevelSettings RiskLevelSettings}.
     * @return An immutable instance of RiskLevelSettings
     * @throws com.hubspot.immutables.validation.InvalidImmutableStateException if any required attributes are missing
     */
    public RiskLevelSettings build() {
      return new RiskLevelSettings(this);
    }

    private boolean concurrencyIsSet() {
      return (optBits & OPT_BIT_CONCURRENCY) != 0;
    }
  }
}

