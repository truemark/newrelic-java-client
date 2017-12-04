package io.truemark.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Holds the Policies data.
 *
 * @author Abhijeet C Kale
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Policies {

  Policy policies [];

}
