package io.truemark.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Holds the Alert Conditions on a policy.
 *
 * @author Abhijeet C Kale
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PolicyAlertConditions {

  List<Condition> conditions;
  @JsonProperty("synthetics_conditions")
  List<Condition> syntheticsConditions;
}
