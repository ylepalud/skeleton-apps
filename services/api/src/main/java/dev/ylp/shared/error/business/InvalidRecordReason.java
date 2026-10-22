package dev.ylp.shared.error.business;

/** The generic reason a record rejected its components, shared by every record of every module. */
public enum InvalidRecordReason implements BusinessErrorReason {
  CONSTRAINT_VIOLATION("Check that the submitted value satisfies the required format.");

  private final String hint;

  InvalidRecordReason(String hint) {
    this.hint = hint;
  }

  @Override
  public String hint() {
    return hint;
  }
}
