package dev.ylp.shared.error.business;

/**
 * A typed reason distinguishing the cause of a {@link BusinessException}, implemented by an enum
 * and never by an exception subclass.
 */
public interface BusinessErrorReason {

  /** Returns a stable, machine-readable code, satisfied by {@code Enum.name()}. */
  String name();

  /** Returns a short, actionable message telling the caller what to do next. */
  String hint();
}
