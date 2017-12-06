package io.truemark.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Holds the Alert Conditions on a policy.
 *
 * @author Abhijeet C Kale
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PolicyAlertCondition {

  Condition condition;
  @JsonProperty("synthetics_condition")
  Condition syntheticsCondition;
  @JsonProperty("nrql_condition")
  Condition nrqlCondition;
}
