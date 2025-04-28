package dialogs;

import javafx.beans.InvalidationListener;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.beans.property.*;
import javafx.beans.binding.Bindings;
import org.checkerframework.checker.index.qual.IndexOrHigh;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.common.value.qual.MinLen;
import records.BoundedPair;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;

public class FieldConfigurator {
    private static final String WARNING_STYLE = "-fx-border-color: red; -fx-border-width: 1;";
    private static final String VALID_STYLE = "-fx-border-color: lime; -fx-border-width: 1;";

    public static BooleanProperty forGregorianTimeCategories(
            TextField field,
            String prompt,
            @MinLen(1) @IndexOrHigh("2") int @NonNull @NonNegative ... lengths
    ) {
        int[] bounds = parseGregorianTimeCategories(lengths);
        BoundedPair<Integer> boundaries = new BoundedPair<>(bounds[0], bounds[1]);
        return configure(field, prompt, v -> v.matches("\\d+") && validateGregorianTimes(v, boundaries));
    }

    public static BooleanProperty forText(
            TextField field,
            String prompt,
            Questions question,
            @MinLen(1) @IndexOrHigh("2") @NonNegative Number... lengths
    ) {

        Number[] bounds = parseLengths(lengths);
        BoundedPair<Integer> boundaries = new BoundedPair<>(
                bounds[0].intValue(),
                bounds[1].intValue()
        );

        return configure(
                field,
                prompt,
                v -> !v.trim().isEmpty()
                        && !v.contains(question.get())
                        && validateLength(v, boundaries)
        );
    }

    @SafeVarargs
    public static <T extends Number & Comparable<T>> BooleanProperty forNumber(
            TextField field,
            String prompt,
            Function<String, T> parser,
            @MinLen(1) @IndexOrHigh("2") @NonNegative T... bounds) {

        Objects.requireNonNull(field, "TextField cannot be null");
        Objects.requireNonNull(parser, "Parser function cannot be null");

        T[] validatedBounds = parseNumericBounds(parser, bounds);
        T min = validatedBounds[0];
        T max = validatedBounds[1];

        return configure(field, prompt, v -> {
            String trimmed = v.trim();
            if (trimmed.isEmpty()) return false;

            try {
                T value = parser.apply(trimmed);
                return value.compareTo(min) >= 0 && value.compareTo(max) <= 0;
            } catch (NumberFormatException e) {
                return false;
            }
        });
    }

    @SafeVarargs
    private static <T extends Number & Comparable<T>> T[] parseNumericBounds(Function<String, T> parser, T... bounds) {
        if (bounds == null) throw new IllegalArgumentException("At least one bound must be specified");

        return switch (bounds.length) {
            case 0 -> {
                T min = parser.apply("0");
                T max = parser.apply("10");
                @SuppressWarnings("unchecked")
                T[] result = (T[]) new Number[]{min, max};
                yield result;
            }
            case 1 -> {
                T min = parser.apply("0");
                @SuppressWarnings("unchecked")
                T[] result = (T[]) Array.newInstance(bounds.getClass().getComponentType(), 2);
                result[0] = min;
                result[1] = bounds[0];
                yield result;
            }
            case 2 -> bounds;
            default -> throw new IllegalArgumentException("Maximum 2 bounds allowed (min, max)");
        };
    }

    public static BooleanProperty forInteger(TextField field, String prompt, int... bounds) {
        return forNumber(field, prompt, Integer::parseInt, Arrays.stream(bounds).boxed().toArray(Integer[]::new));
    }

    public static BooleanProperty forLong(TextField field, String prompt, long... bounds) {
        return forNumber(field, prompt, Long::parseLong, Arrays.stream(bounds).boxed().toArray(Long[]::new));
    }

    public static BooleanProperty forDouble(TextField field, String prompt, double... bounds) {
        return forNumber(field, prompt, Double::parseDouble, Arrays.stream(bounds).boxed().toArray(Double[]::new));
    }

    public static BooleanProperty forListViewSelector(ListView<?> listView) {
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

    public static BooleanProperty forFillableListView(ListView<?> listView) {
        BooleanProperty isValid = new SimpleBooleanProperty();
        isValid.set(!listView.getSelectionModel().isEmpty());
        Label placeholderLabel = new Label();
        placeholderLabel.textProperty().bind(Bindings.when(isValid.not())
                .then("At least one measured goal is required!")
                .otherwise("No items available."));
        placeholderLabel.styleProperty().bind(Bindings.when(isValid.not())
                .then("-fx-text-fill: red; -fx-font-style: italic;")
                .otherwise("-fx-text-fill: gray;"));
        listView.setPlaceholder(placeholderLabel);
        return isValid;
    }

    private static Number[] parseLengths(Number... lengths) {
        if (lengths == null || lengths.length == 0) return new Number[]{1, 10};
        return switch (lengths.length) {
            case 1 -> new Number[]{1, lengths[0]};
            case 2 -> new Number[]{lengths[0], lengths[1]};
            default -> throw new IllegalArgumentException("Too many arguments. Use 1 or 2 lengths.");
        };
    }

    private static int[] parseGregorianTimeCategories(int... lengths) {
        if (lengths.length == 0) return new int[]{1, 32767};
        if (lengths.length == 1) return new int[]{1, lengths[0]};
        return new int[]{lengths[0], lengths[1]};
    }

    private static boolean validateLength(String value, BoundedPair<Integer> pair) {
        return value.length() >= pair.minimum() && value.length() <= pair.maximum();
    }

    private static boolean validateGregorianTimes(String value, BoundedPair<Integer> pair) {
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