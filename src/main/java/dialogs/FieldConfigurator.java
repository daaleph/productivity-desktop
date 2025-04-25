package dialogs;

import javafx.beans.InvalidationListener;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.beans.property.*;
import javafx.beans.binding.Bindings;
import org.checkerframework.checker.index.qual.IndexOrHigh;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.common.value.qual.MinLen;
import records.BoundedPair;

public class FieldConfigurator {
    private static final String WARNING_STYLE = "-fx-border-color: red; -fx-border-width: 1;";
    private static final String VALID_STYLE = "-fx-border-color: lime; -fx-border-width: 1;";

    public static BooleanProperty configureForGregorianTime(
            TextField field,
            String prompt,
            @MinLen(1) @IndexOrHigh("2") int @NonNull @NonNegative ... lengths
    ) {
        int[] bounds = parseGregorianTimeCategories(lengths);
        BoundedPair boundaries = new BoundedPair(bounds[0], bounds[1]);
        return configure(field, prompt, v -> v.matches("\\d+") && validateGregorianTimes(v, boundaries));
    }

    public static BooleanProperty configureForText(
            TextField field,
            String prompt,
            Questions question,
            @MinLen(1) @IndexOrHigh("2") int @NonNull @NonNegative ... lengths
    ) {
        int[] bounds = parseLengths(lengths);
        BoundedPair boundaries = new BoundedPair(bounds[0], bounds[1]);
        return configure(field, prompt, v -> !v.trim().isEmpty() && !v.contains(question.get()) && validateLength(v, boundaries));
    }

    public static BooleanProperty configureListViewSelection(
            ListView<?> listView
    ) {
        BooleanProperty isValid = new SimpleBooleanProperty();
        isValid.set(!listView.getSelectionModel().isEmpty());
        listView.setStyle(isValid.get() ? VALID_STYLE : WARNING_STYLE);
        listView.getSelectionModel().getSelectedItems().addListener(
                (InvalidationListener) change -> {
                    boolean hasSelection = !listView.getSelectionModel().isEmpty();
                    isValid.set(hasSelection);
                    listView.setStyle(hasSelection ? VALID_STYLE : WARNING_STYLE);
                }
        );
        return isValid;
    }

    private static int[] parseLengths(int... lengths) {
        return switch (lengths.length) {
            case 1 -> new int[]{1, lengths[0]};
            case 2 -> new int[]{lengths[0], lengths[1]};
            default -> new int[]{1, 10};
        };
    }

    private static int[] parseGregorianTimeCategories(int... lengths) {
        if (lengths.length == 0) return new int[]{1, 32767};
        if (lengths.length == 1) return new int[]{1, lengths[0]};
        return new int[]{lengths[0], lengths[1]};
    }

    private static boolean validateLength(String value, BoundedPair pair) {
        return value.length() >= pair.minimum() && value.length() <= pair.maximum();
    }

    private static boolean validateGregorianTimes(String value, BoundedPair pair) {
        return Integer.parseInt(value) >= pair.minimum() && Integer.parseInt(value) <= pair.maximum();
    }

    private static BooleanProperty configure(TextField field, String prompt, ValidationRule rule) {
        BooleanProperty isValid = new SimpleBooleanProperty();
        field.setPromptText(prompt);
        field.setPrefWidth(250);
        field.textProperty().addListener((o, oldVal, newVal) -> isValid.set(rule.validate(newVal)));
        field.styleProperty().bind(Bindings.when(isValid).then(VALID_STYLE).otherwise(WARNING_STYLE));
        return isValid;
    }

    @FunctionalInterface
    private interface ValidationRule {
        boolean validate(String value);
    }
}