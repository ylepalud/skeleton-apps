package dev.ylp.shared.error.technical;

import java.io.Serial;

/**
 * An infrastructure failure. A single, non-extensible type: the cause is carried by a typed {@link
 * TechnicalErrorReason}, never by a subclass.
 */
public final class TechnicalException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  // Reasons are enums, hence serializable in practice, but the interface type is not.
  @SuppressWarnings("serial")
  private final TechnicalErrorReason reason;

  /** Creates the exception for the given reason and underlying cause. */
  public TechnicalException(TechnicalErrorReason reason, String message, Throwable cause) {
    super(message, cause);
    this.reason = reason;
  }

  /** Returns the reason this exception was raised for. */
  public TechnicalErrorReason reason() {
    return reason;
  }

  /** Returns the stable code of the reason. */
  public String errorCode() {
    return reason.name();
  }

  /** Returns the actionable hint of the reason. */
  public String hint() {
    return reason.hint();
  }
}
