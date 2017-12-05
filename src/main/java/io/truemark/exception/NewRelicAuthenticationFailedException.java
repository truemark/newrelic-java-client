package io.truemark.exception;

import lombok.NoArgsConstructor;

/**
 * Thrown in case there is a problem authenticating to Rest API.
 *
 * @author Abhijeet Kale
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
