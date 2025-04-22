package dialogs;

import home.models.MainUser;
import home.models.projects.EssentialInfo;
import home.models.projects.Project;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import records.Priority;
import records.Tuple;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ProjectDialog extends Entity<Project> {
    private static final Logger LOGGER = Logger.getLogger(ProjectDialog.class.getName());

    private final TextField nameField = new TextField();
    private final ListView<Priority> priorityList = new ListView<>();
    private final ComboBox<Tuple<Integer, String>> projectType = new ComboBox<>(
        FXCollections.observableArrayList(
            List.of(
                new Tuple<>(1, "Personal"),
                new Tuple<>(2, "Organizational")
            )
        )
    );

    private static final Color SELECTED_COLOR = Color.rgb(100, 149, 237, 0.8);
    private static final Color UNSELECTED_COLOR = Color.TRANSPARENT;
    private static final Color TEXT_COLOR_SELECTED = Color.WHITE;
    private static final Color TEXT_COLOR_UNSELECTED = Color.BLACK;
    private static final BackgroundFill SELECTED_BACKGROUND_FILL =
            new BackgroundFill(SELECTED_COLOR, new CornerRadii(3), Insets.EMPTY);
    private static final Background SELECTED_BACKGROUND = new Background(SELECTED_BACKGROUND_FILL);
    private static final Background UNSELECTED_BACKGROUND = Background.EMPTY;

    // Custom PseudoClass to potentially help JavaFX track state if needed, although direct styling is primary here
    private static final PseudoClass SELECTED_PSEUDO_CLASS = PseudoClass.getPseudoClass("custom-selected");

    public ProjectDialog(MainUser mainUser) {
        super("New Project", mainUser);
        if (mainUser == null) {
            LOGGER.log(Level.SEVERE, "MainUser is null in ProjectDialog constructor!");
            throw new IllegalArgumentException("MainUser cannot be null.");
        }
        initializeForm();
        setupDynamicBehaviors();
        validateForm();
    }

    @Override
    protected void addFormFields() {}

    @Override
    protected Project validateAndCreate() throws ValidationException {
        String projectName = nameField.getText().trim();
        if (projectName.isEmpty()) throw new ValidationException("Project name cannot be empty.");

        ObservableList<Priority> currentlySelected = priorityList.getSelectionModel().getSelectedItems();
        if (currentlySelected.isEmpty()) throw new ValidationException("Must select at least one priority.");

        LOGGER.log(Level.INFO, "Creating project with priorities: {0}", currentlySelected);

        EssentialInfo essentialInfo = new EssentialInfo(
                projectName, 0, false, ZonedDateTime.now()
        );

        Project.ProjectInfo projectInfo = new Project.ProjectInfo(
                essentialInfo,
                List.copyOf(currentlySelected),
                List.of(), null, List.of(), List.of()
        );

        Project project = new Project(UUID.randomUUID());
        project.setInfo(projectInfo);
        return project;
    }

    private void addFormRow(String label, Node field, int row) {
        grid.add(new Label(label), 0, row);
        grid.add(field, 1, row);
    }

    private void initializeForm() {
        grid.setStyle("-fx-padding: 20; -fx-vgap: 15; -fx-hgap: 10;");

        nameField.setPromptText("Enter project name");
        nameField.setPrefWidth(250);
        nameField.textProperty().addListener(
            (obs, oldVal, newVal) -> validateNotEmpty(newVal, nameField)
        );

        priorityList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        priorityList.setPrefWidth(250);
        priorityList.setMinHeight(150);
        priorityList.setFocusTraversable(true);

        Map<Integer, Priority> userPriorities = mainUser.getPriorities();
        if (userPriorities != null && !userPriorities.isEmpty()) {
            priorityList.setItems(FXCollections.observableArrayList(userPriorities.values()));
            LOGGER.log(Level.INFO, "Populated priority list with {0} items.", userPriorities.size());
        } else {
            priorityList.setPlaceholder(new Label("No priorities available."));
            LOGGER.log(Level.WARNING, "No priorities found for user.");
        }

        addFormRow("Project Name:", nameField, 0);
        addFormRow("Priority (Click to select/deselect):", priorityList, 1);
    }

    private void setupDynamicBehaviors() {
        priorityList.setCellFactory(this::createPriorityCellWithManualStylingAndClick);
        priorityList.getSelectionModel().getSelectedItems().addListener(
            (ListChangeListener<Priority>) change -> {
                validatePrioritySelection();
            }
        );
    }

    private ListCell<Priority> createPriorityCellWithManualStylingAndClick(ListView<Priority> listView) {
        ListCell<Priority> cell = new ListCell<>() {

            {
                addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
                    if (isEmpty() || getItem() == null) return;
                    ListView<Priority> lv = getListView();
                    SelectionModel<Priority> sm = lv.getSelectionModel();
                    int index = getIndex();
                    if (sm.isSelected(index)) {
                        sm.clearSelection(index);
                    } else {
                        sm.select(index);
                    }
                    event.consume();
                });
            }

            @Override
            protected void updateItem(Priority priority, boolean empty) {
                super.updateItem(priority, empty);

                if (empty || priority == null) {
                    setText(null);
                    setGraphic(null);
                    applyCellStyle(this, false);
                    pseudoClassStateChanged(SELECTED_PSEUDO_CLASS, false);
                } else {
                    setText(priority.descriptionEs());
                    setGraphic(null);
                    boolean isSelected = getListView().getSelectionModel().isSelected(getIndex());
                    applyCellStyle(this, isSelected);
                    pseudoClassStateChanged(SELECTED_PSEUDO_CLASS, isSelected);
                }
            }
        };

        return cell;
    }

    private void applyCellStyle(ListCell<Priority> cell, boolean isSelected) {
        cell.setBackground(isSelected ? SELECTED_BACKGROUND : UNSELECTED_BACKGROUND);
        cell.setTextFill(isSelected ? TEXT_COLOR_SELECTED : TEXT_COLOR_UNSELECTED);
    }

    private void validatePrioritySelection() {
        boolean isValid = !priorityList.getSelectionModel().getSelectedItems().isEmpty();
        priorityList.setStyle(isValid ? null : "-fx-border-color: red; -fx-border-width: 1px;");
        validateForm();
    }

    private void validateNotEmpty(String value, TextField field) {
        field.setStyle(value == null || value.trim().isEmpty() ? "-fx-border-color: red; -fx-border-width: 1px;" : null);
        validateForm();
    }

    private void validateForm() {
        boolean nameIsValid = nameField.getText() != null && !nameField.getText().trim().isEmpty();
        boolean priorityIsValid = !priorityList.getSelectionModel().getSelectedItems().isEmpty();
        boolean formIsValid = nameIsValid && priorityIsValid;
        submitButton.setDisable(!formIsValid);
    }
}