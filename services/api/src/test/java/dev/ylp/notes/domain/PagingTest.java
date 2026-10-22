package dev.ylp.notes.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.ylp.shared.error.business.BusinessException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PagingTest {

  @Test
  @DisplayName("A page request skips the items of the previous pages")
  void pageRequestSkipsItemsOfPreviousPages() {
    // When / Then
    assertThat(new PageRequest(0, 20).offset()).isZero();
    assertThat(new PageRequest(3, 20).offset()).isEqualTo(60);
  }

  @Test
  @DisplayName("A page request accepts sizes from 1 to its maximum")
  void pageRequestAcceptsSizesFromOneToItsMaximum() {
    // When / Then
    assertThat(new PageRequest(0, 1).size()).isEqualTo(1);
    assertThat(new PageRequest(0, PageRequest.MAX_SIZE).size()).isEqualTo(PageRequest.MAX_SIZE);
  }

  @ParameterizedTest
  @CsvSource({"-1, 20", "0, 0", "0, 101"})
  @DisplayName("A negative page or a size out of range is rejected")
  void negativePageOrSizeOutOfRangeIsRejected(int page, int size) {
    // When / Then
    assertThatThrownBy(() -> new PageRequest(page, size)).isInstanceOf(BusinessException.class);
  }

  @ParameterizedTest
  @CsvSource({"0, 2, 5, true", "2, 1, 5, false", "1, 2, 4, false", "0, 0, 0, false"})
  @DisplayName("A page knows whether another one follows")
  void pageKnowsWhetherAnotherOneFollows(int page, int itemsOnPage, long total, boolean hasNext) {
    // Given
    List<String> items = new ArrayList<>();
    for (int i = 0; i < itemsOnPage; i++) {
      items.add("item-" + i);
    }

    // When
    Page<String> result = new Page<>(items, new PageRequest(page, 2), total);

    // Then
    assertThat(result.hasNext()).isEqualTo(hasNext);
  }

  @Test
  @DisplayName("A page keeps its own copy of the items")
  void pageKeepsItsOwnCopyOfTheItems() {
    // Given
    List<String> items = new ArrayList<>(List.of("a"));
    Page<String> page = new Page<>(items, new PageRequest(0, 2), 1);

    // When
    items.add("b");

    // Then
    assertThat(page.items()).containsExactly("a");
  }
}
