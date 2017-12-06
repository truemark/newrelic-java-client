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
  @JsonProperty("metric_description")
  private String metricDescription;
  @JsonProperty("monitor_id")
  private String monitorId;
  @JsonProperty("condition_scope")
  private String conditionScope;
  private List<Term> terms;
  private String [] entities;
  @JsonProperty("violation_close_timer")
  private String violationCloseTimer;
  @JsonProperty("external_service_url")
  private String externalServiceUrl;
  @JsonProperty("runbook_url")
  private String runbookUrl;
  @JsonProperty("value_function")
  private String valueFunction;
  private Plugin plugin;
  @JsonProperty("user_defined")
  private List<UserDefined> userDefined;
  private Nrql nrql;

}
