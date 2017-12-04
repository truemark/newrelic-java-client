package io.truemark.exception;

import lombok.NoArgsConstructor;

/**
 * @author Erik R. Jensen
 */
@NoArgsConstructor
public class NewRelicAuthenticationFailedException extends NewRelicException {

  public NewRelicAuthenticationFailedException(String message) {
    super(message);
  }

  public NewRelicAuthenticationFailedException(String message, Throwable t) {
    super(message, t);
  }

  public NewRelicAuthenticationFailedException(Throwable t) {
    super(t);
  }
}
