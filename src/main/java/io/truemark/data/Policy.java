package io.truemark.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.sql.Timestamp;

/**
 * Holds the Policy data.
 *
 * @author Abhijeet C Kale
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Policy {

  private Integer id;
  @JsonProperty("incident_preference")
  private String incidentPreference;
  private String name;
  @JsonProperty("created_at")
  private Timestamp createdAt;
  @JsonProperty("updated_at")
  private Timestamp updatedAt;
}
