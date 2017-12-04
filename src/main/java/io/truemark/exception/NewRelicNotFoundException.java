package io.truemark.exception;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Not found exception.
 *
 * @author Dilip S Sisodia
 */
@Data
@NoArgsConstructor
public class NewRelicNotFoundException extends NewRelicException {

  private static final long serialVersionUID = -1018398852945241952L;

  private String detailMessage;

  public NewRelicNotFoundException(String message) {
    super(message);
    this.detailMessage = message;
  }

  public NewRelicNotFoundException(String message, Throwable t) {
    super(message, t);
  }

  public NewRelicNotFoundException(Throwable t) {
    super(t);
  }
}
