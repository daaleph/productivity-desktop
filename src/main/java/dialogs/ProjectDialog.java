package dialogs;

import home.MainUser;
import javafx.scene.layout.*;
import model.projects.EssentialInfo;
import model.projects.Project;
import records.MeasuredGoal;
import records.MeasuredSet;
import records.Priority;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import records.Triplet;
import records.Failure;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import static dialogs.Questions.*;

public class ProjectDialog extends Entity<Project> {
    private static final Logger LOGGER = Logger.getLogger(ProjectDialog.class.getName());

    private final BooleanProperty nameValid = new SimpleBooleanProperty(false);
    private final BooleanProperty daysValid = new SimpleBooleanProperty(false);
    private final BooleanProperty weeksValid = new SimpleBooleanProperty(false);
    private final BooleanProperty monthsValid = new SimpleBooleanProperty(false);
    private final BooleanProperty yearsValid = new SimpleBooleanProperty(false);
    private final BooleanProperty priorityValid = new SimpleBooleanProperty(false);
    private final BooleanProperty descriptionValid = new SimpleBooleanProperty(false);
    private final ObservableList<MeasuredGoal> measuredGoals = FXCollections.observableArrayList();
    private final ListView<MeasuredGoal> measuredGoalsList = new ListView<>(measuredGoals);
    private final Button addGoalButton = new Button("Add Measured Goal");

    private final TextField nameField = new TextField(PROJECT_NAME.get());
    private final TextField completingDays = new TextField(COMPLETING_DAYS.get());
    private final TextField completingWeeks = new TextField(COMPLETING_WEEKS.get());
    private final TextField completingMonths = new TextField(COMPLETING_MONTHS.get());
    private final TextField completingYears = new TextField(COMPLETING_YEARS.get());
    private final ToggleGroup projectTypeGroup = new ToggleGroup();
    private final RadioButton personalRadio = new RadioButton("Personal");
    private final RadioButton organizationalRadio = new RadioButton("Organizational");
    private final ListView<Priority> priorityList = new ListView<>();
    private final ListView<Project> parentProjects = new ListView<>();
    private final TextField descriptionField = new TextField(PROJECT_DESCRIPTION.get());
    private final CheckBox isFavorite = new CheckBox();

    private static final Color SELECTED_COLOR = Color.rgb(100, 149, 237, 0.8);
    private static final Color UNSELECTED_COLOR = Color.TRANSPARENT;
    private static final Color TEXT_COLOR_SELECTED = Color.WHITE;
    private static final Color TEXT_COLOR_UNSELECTED = Color.BLACK;
    private static final BackgroundFill SELECTED_BACKGROUND_FILL =
            new BackgroundFill(SELECTED_COLOR, new CornerRadii(3), Insets.EMPTY);
    private static final Background SELECTED_BACKGROUND = new Background(SELECTED_BACKGROUND_FILL);
    private static final Background UNSELECTED_BACKGROUND = Background.EMPTY;
    private static final PseudoClass SELECTED_PSEUDO_CLASS = PseudoClass.getPseudoClass("custom-selected");

    public ProjectDialog(MainUser mainUser) {
        super("New Project", mainUser);
        if (mainUser == null) {
            LOGGER.log(Level.SEVERE, "MainUser is null in ProjectDialog constructor!");
            throw new IllegalArgumentException("MainUser cannot be null.");
        }
        initializeForm();
        setupDynamicBehaviors();
    }

    @Override
    protected void addFormFields() {}

    @Override
    protected Project validateAndCreate() throws ValidationException {
        String projectName = nameField.getText().trim();
        if (projectName.isEmpty()) throw new ValidationException("Project name cannot be empty.");

        ObservableList<Priority> currentlySelected = priorityList.getSelectionModel().getSelectedItems();
        if (currentlySelected.isEmpty()) throw new ValidationException("Must select at least one priority.");

        EssentialInfo essentialInfo = new EssentialInfo(projectName, 0, false, ZonedDateTime.now());
        Project.ProjectInfo projectInfo = new Project.ProjectInfo(
                essentialInfo, List.copyOf(currentlySelected), List.of(), null, List.of(), List.of()
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

        nameValid.bind(FieldConfigurator.configureForText(nameField, "Enter project name", PROJECT_NAME, 3, 255));
        daysValid.bind(FieldConfigurator.configureForGregorianTime(completingDays, "Necessary days", 0, 6));
        weeksValid.bind(FieldConfigurator.configureForGregorianTime(completingWeeks, "Necessary weeks",0, 3));
        monthsValid.bind(FieldConfigurator.configureForGregorianTime(completingMonths, "Necessary months",0, 11));
        yearsValid.bind(FieldConfigurator.configureForGregorianTime(completingYears, "Necessary years",0, 100));
        descriptionValid.bind(FieldConfigurator.configureForText(descriptionField, "The description", PROJECT_DESCRIPTION,20, 2000));

        configureListView(
                priorityList,
                mainUser.getPriorities(),
                "No priorities available.",
                "Populated priority list with {0} items.",
                "No priorities found for user."
        );
        BooleanProperty priorityValid = FieldConfigurator.configureListViewSelection(
                priorityList
                //, "Select at least one priority"
        );
        priorityValid.addListener((obs, oldVal, newVal) -> validateForm());

        configureListView(
                parentProjects,
                mainUser.getProjects(),
                "No parent projects available.",
                "Populated parent projects list to select with {0} items.",
                "No parent projects found for user."
        );

        personalRadio.setToggleGroup(projectTypeGroup);
        organizationalRadio.setToggleGroup(projectTypeGroup);
        personalRadio.setSelected(true);
        HBox typeBox = new HBox(10, personalRadio, organizationalRadio);
        typeBox.setPadding(new Insets(5));

        addFormRow("Project Name:", nameField, 0);
        addFormRow("Project Type:", typeBox, 1);
        addFormRow("Priority (Click to select/deselect):", priorityList, 2);
        addFormRow("Parent Projects (Click to select/deselect):", parentProjects, 3);
        addFormRow("Completing years:", completingYears, 4);
        addFormRow("Completing months:", completingMonths, 5);
        addFormRow("Completing weeks:", completingWeeks, 6);
        addFormRow("Completing days:", completingDays, 7);
        addFormRow("The details!", descriptionField, 8);
        addFormRow("Will you enjoy it?", isFavorite, 9);

        addFormRow("Measured Goals:", measuredGoalsList, 10);
        addFormRow("", addGoalButton, 11);
        addGoalButton.setOnAction(e -> showMeasuredGoalDialog());
    }

    private void setupDynamicBehaviors() {
        priorityList.setCellFactory(this::createPriorityCellWithManualStylingAndClick);
        priorityList.getSelectionModel().getSelectedItems().addListener(
                (ListChangeListener<Priority>) change -> validateForm()
        );
        parentProjects.setCellFactory(this::createProjectCellWithManualStylingAndClick);
        parentProjects.getSelectionModel().getSelectedItems().addListener(
                (ListChangeListener<Project>) change -> validateForm()
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
                    setText(priority.getName());
                    setGraphic(null);
                    boolean isSelected = getListView().getSelectionModel().isSelected(getIndex());
                    applyCellStyle(this, isSelected);
                    pseudoClassStateChanged(SELECTED_PSEUDO_CLASS, isSelected);
                }
            }
        };

        return cell;
    }

    private ListCell<Project> createProjectCellWithManualStylingAndClick(ListView<Project> listView) {
        ListCell<Project> cell = new ListCell<>() {

            {
                addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
                    if (isEmpty() || getItem() == null) return;
                    ListView<Project> lv = getListView();
                    SelectionModel<Project> sm = lv.getSelectionModel();
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
            protected void updateItem(Project Project, boolean empty) {
                super.updateItem(Project, empty);

                if (empty || Project == null) {
                    setText(null);
                    setGraphic(null);
                    applyCellStyle(this, false);
                    pseudoClassStateChanged(SELECTED_PSEUDO_CLASS, false);
                } else {
                    setText(Project.getName());
                    setGraphic(null);
                    boolean isSelected = getListView().getSelectionModel().isSelected(getIndex());
                    applyCellStyle(this, isSelected);
                    pseudoClassStateChanged(SELECTED_PSEUDO_CLASS, isSelected);
                }
            }
        };

        return cell;
    }

    private <T> void configureListView(
            ListView<T> listView,
            Map<?, T> items,
            String placeholderText,
            String successLogTemplate,
            String warningLogMessage) {

        listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        listView.setPrefWidth(250);
        listView.setMinHeight(150);
        listView.setFocusTraversable(true);

        if (items != null && !items.isEmpty()) {
            listView.setItems(FXCollections.observableArrayList(items.values()));
            LOGGER.log(Level.INFO, successLogTemplate, items.size());
        } else {
            listView.setPlaceholder(new Label(placeholderText));
            LOGGER.log(Level.WARNING, warningLogMessage);
        }
    }

    private void showMeasuredGoalDialog() {
        Dialog<MeasuredGoal> dialog = new Dialog<>();
        dialog.setTitle("New Measured Goal");

        // Fields
        TextField orderField = new TextField();
        TextField itemField = new TextField();
        TextField weightField = new TextField();
        TextField realGoalField = new TextField();
        TextField realAdvanceField = new TextField();
        TextField discreteGoalField = new TextField();
        TextField discreteAdvanceField = new TextField();
        CheckBox finishedCheck = new CheckBox("Finished");
        ObservableList<Failure> failures = FXCollections.observableArrayList();
        ListView<Failure> failuresList = new ListView<>(failures);
        Button addFailureBtn = new Button("Add Failure");

        // Validation
        BooleanProperty orderValid = FieldConfigurator.configureForGregorianTime(orderField, "Order (0-32767)", 0, 32767);
        BooleanProperty itemValid = FieldConfigurator.configureForText(itemField, "Item", PROJECT_NAME, 1, 255);
        BooleanProperty weightValid = FieldConfigurator.configureForGregorianTime(weightField, "Weight", 0, 1000);
        // Add similar validation for real/discrete fields...

        // Layout
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
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

        // Failure addition handler
        addFailureBtn.setOnAction(e -> showFailureDialog(failures));

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
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
            return null;
        });

        dialog.showAndWait().ifPresent(measuredGoals::add);
    }

    private void showFailureDialog(ObservableList<Failure> targetList) {
        Dialog<Failure> dialog = new Dialog<>();
        dialog.setTitle("New Failure Entry");

        TextField reasonField = new TextField();
        TextField solutionField = new TextField();
        TextField descriptionField = new TextField();

        // Validation
        BooleanProperty reasonValid = FieldConfigurator.configureForText(reasonField, "Reason", PROJECT_NAME, 1, 2000);
        BooleanProperty solutionValid = FieldConfigurator.configureForText(solutionField, "Solution", PROJECT_NAME, 1, 2000);
        BooleanProperty descValid = FieldConfigurator.configureForText(descriptionField, "Description", PROJECT_NAME, 1, 4000);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.addRow(0, new Label("Reason*:"), reasonField);
        grid.addRow(1, new Label("Solution*:"), solutionField);
        grid.addRow(2, new Label("Description*:"), descriptionField);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                return new Failure(new Triplet<>(
                        reasonField.getText(),
                        solutionField.getText(),
                        descriptionField.getText()
                ));
            }
            return null;
        });

        dialog.showAndWait().ifPresent(targetList::add);
    }

    private <T> MeasuredSet<T> createMeasuredSet(TextField goalField, TextField advanceField, Class<T> type) {
        return new MeasuredSet<>(
                Map.of(
                        "goal", parseValue(goalField.getText(), type),
                        "advance", parseValue(advanceField.getText(), type)
                ),
                type
        );
    }

    @SuppressWarnings("unchecked")
    private <T> T parseValue(String text, Class<T> type) {
        if (type == Double.class) return (T) Double.valueOf(text);
        if (type == Integer.class) return (T) Integer.valueOf(text);
        throw new IllegalArgumentException("Unsupported type");
    }

    private <T> void applyCellStyle(ListCell<T> cell, boolean isSelected) {
        cell.setBackground(isSelected ? SELECTED_BACKGROUND : UNSELECTED_BACKGROUND);
        cell.setTextFill(isSelected ? TEXT_COLOR_SELECTED : TEXT_COLOR_UNSELECTED);
    }

    private void validateForm() {
        boolean formIsValid = nameValid.get() &&
                daysValid.get() &&
                monthsValid.get() &&
                yearsValid.get() &&
                priorityValid.get();

        submitButton.setDisable(!formIsValid);
    }
}