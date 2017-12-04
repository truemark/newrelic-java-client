package io.truemark.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Holds the Synthetic Condition data.
 *
 * @author Abhijeet C Kale
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SyntheticCondition {

  private Integer id;
  @JsonProperty("monitor_id")
  private String monitorId;
  private String name;
  @JsonProperty("runbook_url")
  private String runbookUrl;
  private Boolean enabled;
}
