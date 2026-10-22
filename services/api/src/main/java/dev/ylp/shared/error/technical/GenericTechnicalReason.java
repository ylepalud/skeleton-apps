package dev.ylp.shared.error.technical;

/** Technical failure reasons not tied to any module. */
public enum GenericTechnicalReason implements TechnicalErrorReason {
  INFRASTRUCTURE_FAILURE("Retry later. If the issue persists, check the service logs.");

  private final String hint;

  GenericTechnicalReason(String hint) {
    this.hint = hint;
  }

  @Override
  public String hint() {
    return hint;
  }
}
