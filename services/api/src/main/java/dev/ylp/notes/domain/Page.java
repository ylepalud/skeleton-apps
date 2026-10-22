package dev.ylp.notes.domain;

import java.util.List;

/** One page of results, with the total needed to navigate the others. */
public record Page<T>(List<T> items, PageRequest request, long totalItems) {

  /** Keeps an immutable copy of the items. */
  public Page {
    items = List.copyOf(items);
  }

  /** Returns whether a page follows this one. */
  public boolean hasNext() {
    return request.offset() + items.size() < totalItems;
  }
}
