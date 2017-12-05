package io.truemark.exception;

import lombok.NoArgsConstructor;

import java.io.IOException;

/**
 * Base exception class for New Relic Java Client.
 *
 * @author Abhijeet Kale
 */
@NoArgsConstructor
public class NewRelicException extends IOException {

  private static final long serialVersionUID = -8458422627763003734L;

  public NewRelicException(String message) {
    super(message);
  }

  public NewRelicException(String message, Throwable t) {
    super(message, t);
  }

  public NewRelicException(Throwable t) {
    super(t);
  }
}
