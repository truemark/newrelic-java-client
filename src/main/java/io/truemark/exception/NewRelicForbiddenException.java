package io.truemark.exception;

import lombok.NoArgsConstructor;

/**
 * @author Erik R. Jensen
 */
@NoArgsConstructor
public class NewRelicForbiddenException extends NewRelicException {

  public NewRelicForbiddenException(String message) {
    super(message);
  }

  public NewRelicForbiddenException(String message, Throwable t) {
    super(message, t);
  }

  public NewRelicForbiddenException(Throwable t) {
    super(t);
  }

}
