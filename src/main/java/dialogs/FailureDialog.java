package dialogs;

import home.MainUser;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import records.Failure;
import records.Triplet;

import java.util.logging.Logger;

import static dialogs.Questions.PROJECT_NAME;

public class FailureDialog extends Entity<Failure> {
    private static FailureDialog instance;

    private static final Logger LOGGER = Logger.getLogger(FailureDialog.class.getName());

    BooleanProperty descriptionValid = new SimpleBooleanProperty(false);
    BooleanProperty reasonValid = new SimpleBooleanProperty(false);
    BooleanProperty solutionValid = new SimpleBooleanProperty(false);

    private final TextField reasonField = new TextField();
    private final TextField solutionField = new TextField();
    private final TextField descriptionField = new TextField();

    public FailureDialog(MainUser mainUser) {
        super("New Failure Entry", mainUser);
        initializeForm();
    }

    public static synchronized FailureDialog getInstance(MainUser mainUser) {
        if (instance == null) {
            instance = new FailureDialog(mainUser);
            instance.setOnShown(e -> instance.toFront());
        }
        return instance;
    }

    @Override
    protected void initializeForm() {
        super.initializeForm();
        addFormRows();
        setupDynamicBehaviors();
        createAlphanumericValidations();
    }

    @Override
    protected void addFormRows() {
        grid.addRow(0, new Label("Reason*:"), reasonField);
        grid.addRow(1, new Label("Solution*:"), solutionField);
        grid.addRow(2, new Label("Description*:"), descriptionField);
    }

    @Override
    protected void setupDynamicBehaviors() {
        submitButton.disableProperty().bind(
                reasonValid.not().or(solutionValid.not()).or(descriptionValid.not())
        );
    }

    @Override
    protected void logObjectStructure() {
        LOGGER.info("=== Failures ===");
        this.getResult().logEntity();
        LOGGER.info("=========================");
    }

    @Override
    protected void createAlphanumericValidations() {
        reasonValid.bind(FieldConfigurator.forText(reasonField, "Reason", PROJECT_NAME, 1, 2000));
        solutionValid.bind(FieldConfigurator.forText(solutionField, "Solution", PROJECT_NAME, 1, 2000));
        descriptionValid.bind(FieldConfigurator.forText(descriptionField, "Description", PROJECT_NAME, 1, 4000));
    }

    @Override
    protected void validateAndCreate() {
        Failure failure = new Failure(new Triplet<>(
                reasonField.getText().trim(),
                solutionField.getText().trim(),
                descriptionField.getText().trim()
        ));
        setResult(failure);
    }

    @Override
    protected void cleanup() {
        instance = null;
    }
}