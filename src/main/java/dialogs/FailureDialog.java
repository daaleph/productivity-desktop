package dialogs;

import home.MainUser;
import javafx.beans.property.BooleanProperty;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import records.Failure;
import records.Triplet;

import static dialogs.Questions.PROJECT_NAME;

public class FailureDialog extends Entity<Failure> {
    private static FailureDialog instance;
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
        grid.setHgap(10);
        grid.setVgap(10);

        BooleanProperty reasonValid = FieldConfigurator.forText(reasonField, "Reason", PROJECT_NAME, 1, 2000);
        BooleanProperty solutionValid = FieldConfigurator.forText(solutionField, "Solution", PROJECT_NAME, 1, 2000);
        BooleanProperty descValid = FieldConfigurator.forText(descriptionField, "Description", PROJECT_NAME, 1, 4000);

        grid.addRow(0, new Label("Reason*:"), reasonField);
        grid.addRow(1, new Label("Solution*:"), solutionField);
        grid.addRow(2, new Label("Description*:"), descriptionField);

        submitButton.disableProperty().bind(
                reasonValid.not().or(solutionValid.not()).or(descValid.not())
        );
    }

    @Override
    protected void createAlphanumericValidations() {}

    @Override
    protected Failure validateAndCreate() {
        return new Failure(new Triplet<>(
                reasonField.getText().trim(),
                solutionField.getText().trim(),
                descriptionField.getText().trim()
        ));
    }

    @Override
    protected void cleanup() {
        instance = null;
    }
}