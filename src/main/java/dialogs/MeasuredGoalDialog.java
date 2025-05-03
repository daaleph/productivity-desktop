package dialogs;

import home.MainUser;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import records.Failure;
import records.MeasuredGoal;
import records.MeasuredSet;

import java.util.ArrayList;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;

import static dialogs.FormLayoutHelper.addFormRow;
import static dialogs.Questions.*;

public class MeasuredGoalDialog extends Entity<MeasuredGoal> {
    private static MeasuredGoalDialog instance;

    private static final Logger LOGGER = Logger.getLogger(MeasuredGoalDialog.class.getName());

    private final BooleanProperty orderValid = new SimpleBooleanProperty(false);
    private final BooleanProperty itemValid  = new SimpleBooleanProperty(false);
    private final BooleanProperty weightValid  = new SimpleBooleanProperty(false);
    private final BooleanProperty realGoalValid = new SimpleBooleanProperty(false);
    private final BooleanProperty realAdvanceValid = new SimpleBooleanProperty(false);
    private final BooleanProperty discreteGoalValid = new SimpleBooleanProperty(false);
    private final BooleanProperty discreteAdvanceValid = new SimpleBooleanProperty(false);
    private final BooleanProperty failuresValid = new SimpleBooleanProperty(false);

    private final TextField orderField = new TextField("10");
    private final TextField itemField = new TextField("The first item here is ");
    private final TextField weightField = new TextField("10");
    private final TextField realGoalField = new TextField("100");
    private final TextField realAdvanceField = new TextField("10");
    private final TextField discreteGoalField = new TextField("100");
    private final TextField discreteAdvanceField = new TextField("10");
    private final CheckBox finishedCheck = new CheckBox("Finished");
    private final ObservableList<Failure> observableFailures = FXCollections.observableArrayList();
    private final ListView<Failure> failuresList = new ListView<>(observableFailures);
    private final Button addFailureButton = new Button("Add Failure");
    private final Button deleteFailureButton = new Button("Delete Selected Failures");

    public MeasuredGoalDialog(MainUser mainUser) {
        super("New Measured Goal", mainUser);
        initializeForm();
        createAlphanumericValidations();
        configureListViews();
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
        super.initializeForm();
        addFormRows();
        createAlphanumericValidations();
        createListingValidations();
        setupDynamicBehaviors();
    }

    private void configureListViews() {
        failuresList.setMinHeight(100);
        failuresList.setPrefHeight(100);
        failuresList.setItems(observableFailures);
    }

    private ListCell<Failure> createDynamicStyledFailureCell(ListView<Failure> failures) {
        return createStyledListCell(Failure::description);
    }

    @Override
    protected void createAlphanumericValidations() {
        orderValid.bind(FieldConfigurator.forInteger(orderField, "Order", 0, 32767));
        itemValid.bind(FieldConfigurator.forText(itemField, "Description", PROJECT_NAME, 1, 255));
        weightValid.bind(FieldConfigurator.forDouble(weightField, "Relevance (0, 100)%", 1, 100));
        discreteGoalValid.bind(FieldConfigurator.forLong(discreteGoalField, "Discrete goal ((2^63)−1, (2^63)−1)",
                Long.MIN_VALUE, Long.MAX_VALUE));
        discreteAdvanceValid.bind(FieldConfigurator.forLong(discreteAdvanceField, "Discrete advance ((2^63)−1, (2^63)−1)",
                Long.MIN_VALUE, Long.MAX_VALUE));
        realGoalValid.bind(FieldConfigurator.forDouble(realGoalField, "Real goal (1, 100)%", 1, 100));
        realAdvanceValid.bind(FieldConfigurator.forDouble(realAdvanceField, "Real advance (0, 100)%",100));
    }

    private void createListingValidations() {
        failuresValid.bind(FieldConfigurator.forFillableListView(failuresList, "failure", false));
    }

    @Override
    protected void validateAndCreate() {
        MeasuredGoal goal = new MeasuredGoal(
                Integer.parseInt(orderField.getText()),
                itemField.getText(),
                Double.parseDouble(weightField.getText()),
                createMeasuredSet(realGoalField, realAdvanceField, Double.class),
                createMeasuredSet(discreteGoalField, discreteAdvanceField, Integer.class),
                finishedCheck.isSelected(),
                new ArrayList<>(observableFailures)
        );
        setResult(goal);
    }

    @Override
    protected void setupDynamicBehaviors() {
        failuresList.setCellFactory(this::createDynamicStyledFailureCell);
        addFailureButton.setOnAction(e -> showFailureDialog());
        deleteFailureButton.setOnAction((e) -> {
            observableFailures.removeAll(failuresList.getSelectionModel().getSelectedItems());
            childDialogs.getLast().cleanup();
        });
        deleteFailureButton.disableProperty().bind(Bindings.isEmpty(failuresList.getSelectionModel().getSelectedItems()));
        submitButton.disableProperty().bind(orderValid.not().or(itemValid.not()).or(weightValid.not()));
    }

    @Override
    protected void logObjectStructure() {
        LOGGER.info("=== Measured Goals ===");
        this.getResult().logEntity();
        LOGGER.info("=========================");
    }

    @Override
    protected void addFormRows() {
        addFormRow("Order*:", grid, orderField, 0);
        addFormRow("Item*:", grid, itemField, 1);
        addFormRow("Weight*:", grid, weightField, 2);
        addFormRow("Real Goal:", grid, realGoalField, 3);
        addFormRow("Real Advance:", grid, realAdvanceField, 4);
        addFormRow("Discrete Goal:", grid, discreteGoalField, 5);
        addFormRow("Discrete Advance:", grid, discreteAdvanceField, 6);
        addFormRow("Finished:", grid, finishedCheck, 7);
        addFormRow("Failures:", grid, failuresList, 8);
        HBox buttonsContainer = new HBox(10, addFailureButton, deleteFailureButton);
        addFormRow("", grid, buttonsContainer, 9);
    }

    private <T> MeasuredSet<T> createMeasuredSet(TextField goalField, TextField advanceField, Class<T> type) {
        T goal = parseValue(goalField.getText().isEmpty() ? "0" : goalField.getText(), type);
        T advance = parseValue(advanceField.getText().isEmpty() ? "0" : advanceField.getText(), type);
        return new MeasuredSet<>(Map.of("goal", goal, "advance", advance), type);
    }

    private <T> T parseValue(String text, Class<T> type) {
        Function<String, ?> parser = PARSERS.get(type);
        if (parser == null) throw new IllegalArgumentException("Unsupported type");
        return type.cast(parser.apply(text));
    }

    private void showFailureDialog() {
        FailureDialog dialog = FailureDialog.getInstance(mainUser);
        dialog.show();
        dialog.setOnHidden(e -> {
            Failure result = dialog.getResult();
            if (result != null) {
                observableFailures.add(result);
                dialog.cleanup();
            }
        });
        this.addChildDialog(dialog);
    }

    @Override
    protected void cleanup() { instance = null; }

    private static final Map<Class<?>, Function<String, ?>> PARSERS = Map.of(
            Long.class, Long::valueOf,
            Double.class, Double::valueOf,
            Integer.class, Integer::valueOf
    );
}
