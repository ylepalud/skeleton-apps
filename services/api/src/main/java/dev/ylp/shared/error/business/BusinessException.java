package dev.ylp.shared.error.business;

import java.io.Serial;

/**
 * A violated business rule. A single, non-extensible type: the cause is carried by a typed {@link
 * BusinessErrorReason}, never by a subclass.
 */
public final class BusinessException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  // Reasons are enums, hence serializable in practice, but the interface type is not.
  @SuppressWarnings("serial")
  private final BusinessErrorReason reason;

  /** Creates the exception for the given reason. */
  public BusinessException(BusinessErrorReason reason, String message) {
    super(message);
    this.reason = reason;
  }

  /** Returns the reason this exception was raised for. */
  public BusinessErrorReason reason() {
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
