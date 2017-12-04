package io.truemark.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Holds the Term data.
 *
 * @author Abhijeet C Kale
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Term {

  private String duration;
  private String operator;
  private String priority;
  private String threshold;
  @JsonProperty("time_function")
  private String timeFunction;
}
