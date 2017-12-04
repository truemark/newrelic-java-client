package io.truemark.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Holds the Condition data.
 *
 * @author Abhijeet C Kale
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Condition {

  private Integer id;
  private String type;
  private String name;
  private Boolean enabled;
  private String metric;
  @JsonProperty("condition_scope")
  private String conditionScope;
  private List<Term> terms;
  private String [] entities;
}
