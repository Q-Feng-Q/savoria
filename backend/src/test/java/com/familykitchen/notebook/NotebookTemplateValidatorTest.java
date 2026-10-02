package com.familykitchen.notebook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.familykitchen.common.error.BusinessException;
import com.familykitchen.notebook.model.NotebookField;
import com.familykitchen.notebook.model.NotebookFieldInput;
import com.familykitchen.notebook.service.NotebookTemplateValidator;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Field schema validation and stable server assigned keys. */
class NotebookTemplateValidatorTest {
  private final NotebookTemplateValidator validator = new NotebookTemplateValidator();

  @Test void supportsAllElevenTypesAndAssignsKeys() {
    List<String> types = List.of("TEXT", "LONG_TEXT", "NUMBER", "DATE", "TIME", "DATETIME",
        "SINGLE_SELECT", "MULTI_SELECT", "BOOLEAN", "RATING", "IMAGE");
    var inputs = types.stream().map(type -> input(null, type, type,
        type.endsWith("SELECT") ? List.of("A", "B") : List.of())).toList();
    var published = validator.publish(inputs, List.of());
    assertThat(published).hasSize(11);
    assertThat(published).extracting(NotebookField::key).doesNotHaveDuplicates();
    assertThat(published).allSatisfy(field -> assertThat(field.key()).startsWith("field_"));
  }

  @Test void labelChangePreservesKeyButTypeChangeGetsNewKey() {
    var first = validator.publish(List.of(input(null, "TEXT", "Name", List.of())), List.of());
    var renamed = validator.publish(List.of(input(first.get(0).key(), "TEXT", "New name", List.of())), first);
    assertThat(renamed.get(0).key()).isEqualTo(first.get(0).key());
    var changed = validator.publish(List.of(input(first.get(0).key(), "NUMBER", "New name", List.of())), first);
    assertThat(changed.get(0).key()).isNotEqualTo(first.get(0).key());
    assertThat(first.get(0).label()).isEqualTo("Name");
  }

  @Test void rejectsInvalidOptionsRequiredAndUnknownKeys() {
    assertThatThrownBy(() -> validator.publish(List.of(input(null, "SINGLE_SELECT", "Choice", List.of())), List.of()))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> validator.publish(List.of(input(null, "TEXT", "Name", List.of("extra"))), List.of()))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> validator.publish(List.of(new NotebookFieldInput(null, "TEXT", "Name", null,
        List.of(), null)), List.of())).isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> validator.publish(List.of(input("field_unknown", "TEXT", "Name", List.of())), List.of()))
        .isInstanceOf(BusinessException.class);
  }

  private static NotebookFieldInput input(String key, String type, String label, List<String> options) {
    return new NotebookFieldInput(key, type, label, true, options, null);
  }
}
