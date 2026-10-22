package dev.ylp.shared.error.business;

/**
 * What kind of failure a business error is, independently of any protocol. Adapters translate it:
 * the REST adapter turns it into an HTTP status.
 */
public enum ErrorCategory {
  /** The target does not exist, or the caller may not know it exists. */
  NOT_FOUND,
  /** A submitted value does not satisfy its format or constraints. */
  INVALID,
  /** The operation clashes with the current state of the target. */
  CONFLICT,
  /** The operation breaks a business rule. */
  RULE_VIOLATION
}
