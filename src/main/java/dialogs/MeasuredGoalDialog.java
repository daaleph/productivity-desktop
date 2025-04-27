package dialogs;

import home.MainUser;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import records.Failure;
import records.MeasuredGoal;
import records.MeasuredSet;

import java.util.ArrayList;
import java.util.Map;

import static dialogs.Questions.*;

public class MeasuredGoalDialog extends Entity<MeasuredGoal> {
    private static MeasuredGoalDialog instance;

    private final BooleanProperty orderValid = new SimpleBooleanProperty(false);
    private final BooleanProperty itemValid  = new SimpleBooleanProperty(false);
    private final BooleanProperty weightValid  = new SimpleBooleanProperty(false);
    private final BooleanProperty discreteGoalValid = new SimpleBooleanProperty(false);
    private final BooleanProperty discreteAdvanceValid = new SimpleBooleanProperty(false);
    private final BooleanProperty realGoalValid = new SimpleBooleanProperty(false);
    private final BooleanProperty realAdvanceValid = new SimpleBooleanProperty(false);

    private final TextField orderField = new TextField();
    private final TextField itemField = new TextField();
    private final TextField weightField = new TextField();
    private final TextField realGoalField = new TextField();
    private final TextField realAdvanceField = new TextField();
    private final TextField discreteGoalField = new TextField();
    private final TextField discreteAdvanceField = new TextField();
    private final CheckBox finishedCheck = new CheckBox("Finished");
    private final ObservableList<Failure> failures = FXCollections.observableArrayList();
    private final ListView<Failure> failuresList = new ListView<>(failures);
    private final Button addFailureBtn = new Button("Add Failure");


    public MeasuredGoalDialog(MainUser mainUser) {
        super("New Measured Goal", mainUser);
        initializeForm();
    }

    public static synchronized MeasuredGoalDialog getInstance(MainUser user) {
        if (instance == null) {
            instance = new MeasuredGoalDialog(user);
            instance.setOnShown(e -> instance.toFront());
        }
        return instance;
    }

    @Override
    protected void initializeForm() {
        grid.setHgap(10);
        grid.setVgap(10);

        grid.addRow(0, new Label("Order*:"), orderField);
        grid.addRow(1, new Label("Item*:"), itemField);
        grid.addRow(2, new Label("Weight*:"), weightField);
        grid.addRow(3, new Label("Real Goal:"), realGoalField);
        grid.addRow(4, new Label("Real Advance:"), realAdvanceField);
        grid.addRow(5, new Label("Discrete Goal:"), discreteGoalField);
        grid.addRow(6, new Label("Discrete Advance:"), discreteAdvanceField);
        grid.addRow(7, new Label("Finished:"), finishedCheck);
        grid.addRow(8, new Label("Failures:"), failuresList);
        grid.addRow(9, addFailureBtn);

        addFailureBtn.setOnAction(e -> handleAddFailure());

        submitButton.disableProperty().bind(
                orderValid.not().or(itemValid.not()).or(weightValid.not())
        );
    }

    @Override
    protected void createAlphanumericValidations() {
        orderValid.bind(FieldConfigurator.forGregorianTimeCategories(orderField, "Order", 0, 32767));
        itemValid.bind(FieldConfigurator.forText(itemField, "Description", PROJECT_NAME, 1, 255));
        weightValid.bind(FieldConfigurator.forGregorianTimeCategories(weightField, "Relevance (0, 100%)",
                1, 100));
        discreteGoalValid.bind(FieldConfigurator.forText(weightField, "Discrete goal ((2^63)−1, (2^63)−1)",
                DISCRETE_GOAL, Long.MIN_VALUE, Long.MAX_VALUE));
        discreteAdvanceValid.bind(FieldConfigurator.forText(weightField, "Discrete advance ((2^63)−1, (2^63)−1)",
                DISCRETE_GOAL, Long.MIN_VALUE, Long.MAX_VALUE));
        realGoalValid.bind(FieldConfigurator.forText(weightField, "Real goal (0, 100%)", REAL_GOAL,100));
        realAdvanceValid.bind(FieldConfigurator.forText(weightField, "Real advance (1, 100%)", REAL_GOAL,
                1, 100));
    }

    @Override
    protected MeasuredGoal validateAndCreate() {
        return new MeasuredGoal(
                Integer.parseInt(orderField.getText()),
                itemField.getText(),
                Double.parseDouble(weightField.getText()),
                createMeasuredSet(realGoalField, realAdvanceField, Double.class),
                createMeasuredSet(discreteGoalField, discreteAdvanceField, Integer.class),
                finishedCheck.isSelected(),
                new ArrayList<>(failures)
        );
    }

    private <T> MeasuredSet<T> createMeasuredSet(TextField goalField, TextField advanceField, Class<T> type) {
        T goal = parseValue(goalField.getText().isEmpty() ? "0" : goalField.getText(), type);
        T advance = parseValue(advanceField.getText().isEmpty() ? "0" : advanceField.getText(), type);
        return new MeasuredSet<>(Map.of("goal", goal, "advance", advance), type);
    }

    private <T> T parseValue(String text, Class<T> type) {
        if (type == Double.class) return type.cast(Double.valueOf(text));
        if (type == Integer.class) return type.cast(Integer.valueOf(text));
        throw new IllegalArgumentException("Unsupported type");
    }

    private void handleAddFailure() {
        FailureDialog dialog = FailureDialog.getInstance(mainUser);
        this.addChildDialog(dialog);
        dialog.show();
    }

    @Override
    protected void cleanup() {
        instance = null;
    }
}
