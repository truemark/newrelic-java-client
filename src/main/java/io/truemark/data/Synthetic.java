package io.truemark.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * Holds the Synthetic data.
 *
 * @author Abhijeet C Kale
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Synthetic {

  private UUID id;
  private String name;
  private String type;
  private String status;
  private String frequency;
  private String uri;
  private Double slaThreshold;
  private List<String> locations;
}
