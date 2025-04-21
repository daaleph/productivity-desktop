package dialogs;

import home.models.MainUser;
import home.models.projects.EssentialInfo;
import home.models.projects.Project;
import home.records.Priority;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// Assuming Entity<T> is an abstract class that ProjectDialog extends
// and that Entity's constructor calls addFormFields()
// IMPORTANT: This fix assumes the 'grid' field in the Entity base class is accessible
// (e.g., declared as 'protected GridPane grid;' in Entity).
public class ProjectDialog extends Entity<Project> {

    // These fields are declared here and will be initialized *after* the super() call returns.
    private final TextField nameField = new TextField();
    private final ComboBox<Priority> priorityCombo = new ComboBox<>();

    public ProjectDialog(MainUser mainUser) {
        // 1. super() is called. The Entity constructor runs.
        //    The Entity constructor likely calls initializeUI() which calls addFormFields() here.
        //    At this point, nameField and priorityCombo are null from the perspective of the base constructor flow.
        //    The overridden addFormFields() in THIS class (ProjectDialog) is called.
        super("New Project", mainUser);

        // 2. After super() returns, the instance variable initializers in ProjectDialog run.
        //    nameField = new TextField();
        //    priorityCombo = new ComboBox<>();
        //    At this point, nameField and priorityCombo are valid, non-null objects.

        // 3. initializeForm() is called. This is the correct place to configure, populate,
        //    and *add* the fields to the grid now that they are initialized.
        initializeForm();
    }

    // This method is called by the superclass constructor (Entity).
    // Since our fields (nameField, priorityCombo) are not initialized when this is called,
    // we must NOT try to add them to the grid here, as that would cause the NPE.
    // Make this method empty in the derived class.
    @Override
    protected void addFormFields() {
        // Do NOTHING here. The actual adding to the grid is moved to initializeForm().
        // If you were to call addFormRow("...", nameField, ...); here, nameField is still null.
    }

    @Override
    protected Project validateAndCreate() throws ValidationException {
        // This method is called later, typically when the dialog is submitted,
        // long after initialization is complete, so nameField and priorityCombo are not null.
        if (nameField.getText().isEmpty()) {
            throw new ValidationException("Project name cannot be empty");
        }
        if (priorityCombo.getValue() == null) {
            throw new ValidationException("Must select a priority");
        }

        // Create ProjectInfo first
        Project.ProjectInfo projectInfo = new Project.ProjectInfo(
                new EssentialInfo(
                        nameField.getText(),
                        0, // Default type
                        false,
                        ZonedDateTime.now()
                ),
                List.of(priorityCombo.getValue()), // priorityCombo has items and a selected value by now
                List.of(), // Empty measured goals
                null, // No completion time
                List.of(), // No underlying categories
                List.of()  // No parent projects
        );

        // Create and return Project with the generated UUID
        Project project = new Project(UUID.randomUUID());
        project.setInfo(projectInfo);
        return project;
    }

    // Helper method to add a labeled field to the grid.
    // This method can be used in initializeForm() now.
    // This assumes the 'grid' field is accessible (e.g., protected) from the base class.
    private void addFormRow(String label, javafx.scene.Node field, int row) {
        // Add the label and the initialized field to the grid
        grid.add(new Label(label), 0, row);
        grid.add(field, 1, row); // field is non-null when this is called from initializeForm()
    }


    private void initializeForm() {
        // Set up form styling and component properties
        // Assumes 'grid' is accessible from the base class
        grid.setStyle("-fx-padding: 20; -fx-vgap: 15; -fx-hgap: 10;");

        // Configure nameField
        nameField.setPromptText("Enter project name");
        nameField.setPrefWidth(250);
        // Add listener here, after nameField is initialized
        nameField.textProperty().addListener((obs, oldVal, newVal) ->
                validateNotEmpty(newVal, nameField));

        // ********************************************************
        // CONFIGURE, POPULATE, AND ADD priorityCombo HERE!
        // This is called *after* priorityCombo is initialized by `new ComboBox<>()`
        // ********************************************************
        Map<Integer, Priority> userPriorities = mainUser.getPriorities();

        // Add items to the ComboBox
        if (userPriorities != null) {
            priorityCombo.getItems().addAll(userPriorities.values());
        } else {
            // Handle case where getPriorities() might return null if necessary
            System.err.println("Warning: User priorities map is null.");
        }

        // Configure the ComboBox properties
        priorityCombo.setPrefWidth(250);
        priorityCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Priority priority) {
                // Use descriptionEs for display as in your original initializeForm
                return priority != null ? priority.descriptionEs() : "";
            }

            @Override
            public Priority fromString(String string) {
                // Find the Priority object based on the Spanish description
                return priorityCombo.getItems().stream()
                        .filter(p -> p.descriptionEs().equals(string))
                        .findFirst()
                        .orElse(null); // Return null if not found
            }
        });

        // Add listener here, after priorityCombo is initialized
        priorityCombo.valueProperty().addListener((obs, oldVal, newVal) ->
                validateComboSelection(priorityCombo));

        // Optional: Set an initial selection if needed
        // ... (code from previous example if you want a default selection) ...
        // For now, we rely on validation requiring a selection.

        // ********************************************************

        // NOW, ADD the initialized fields to the grid.
        // This is safe because nameField and priorityCombo are initialized now.
        // This assumes the 'grid' field from the Entity base class is accessible.
        addFormRow("Project Name:", nameField, 0);
        addFormRow("Priority:", priorityCombo, 1);


        // Initial validation check
        // Call validateForm *after* all fields are set up and potentially populated,
        // as it checks their initial state.
        validateForm();
    }

    // Helper method for form validation
    private void validateForm() {
        // Check if name field is not empty and a priority is selected
        // submitButton (presumably also from Entity) must be accessible.
        // Assuming submitButton is protected in Entity.
        if (submitButton != null) {
            submitButton.setDisable(
                    nameField.getText().trim().isEmpty() ||
                            priorityCombo.getValue() == null // This check is now safe
            );
        }
    }

    // Validation utilities (keep as they are)
    private void validateNotEmpty(String value, TextField field) {
        if (value.trim().isEmpty()) {
            field.setStyle("-fx-border-color: #ff4444;"); // Highlight red
        } else {
            field.setStyle(""); // Remove highlight
        }
        validateForm(); // Re-validate form state
    }

    private void validateComboSelection(ComboBox<?> combo) {
        if (combo.getValue() == null) {
            combo.setStyle("-fx-border-color: #ff4444;"); // Highlight red
        } else {
            combo.setStyle(""); // Remove highlight
        }
        validateForm(); // Re-validate form state
    }
}