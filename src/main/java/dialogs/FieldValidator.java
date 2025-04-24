package dialogs;

import javafx.scene.control.TextField;
import javafx.beans.property.*;
import javafx.beans.binding.Bindings;

public class FieldValidator {
    private static final String WARNING_STYLE = "-fx-border-color: red; -fx-border-width: 1;";
    private static final String VALID_STYLE = "-fx-border-color: lime; -fx-border-width: 1;";

    public static BooleanProperty configureNumericField(TextField field, String prompt, int... lengths) {
        int[] bounds = parseLengths(lengths);
        return configure(field, prompt, v -> v.matches("\\d+") && validateLength(v, bounds[0], bounds[1]));
    }

    public static BooleanProperty configureTextField(TextField field, String prompt, int... lengths) {
        int[] bounds = parseLengths(lengths);
        return configure(field, prompt, v -> !v.trim().isEmpty() && validateLength(v, bounds[0], bounds[1]));
    }

    private static int[] parseLengths(int... lengths) {
        return switch (lengths.length) {
            case 1 -> new int[]{1, lengths[0]};
            case 2 -> new int[]{lengths[0], lengths[1]};
            default -> new int[]{1, 10};
        };
    }

    private static boolean validateLength(String value, int min, int max) {
        return value.length() >= min && value.length() <= max;
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