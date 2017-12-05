package io.truemark.exception;

import io.truemark.error.NewRelicError;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Bad Request Exception.
 *
 * @author Abhijeet Kale
 */
@Data
@NoArgsConstructor
public class NewRelicBadRequestException extends NewRelicException {

  private static final long serialVersionUID = 4575363667276218487L;

  protected List<NewRelicError> errors = new ArrayList<>();

  public NewRelicBadRequestException(String message) {
    super(message);
  }

  public NewRelicBadRequestException(String message, List<NewRelicError> errors) {
    super(message);
    this.errors = errors;
  }
}
