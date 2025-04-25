package dialogs;

import home.MainUser;
import model.projects.EssentialInfo;
import model.projects.Project;
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
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;

import java.time.ZonedDateTime;
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

//    private final ObservableList<MsrdGoal> msrdGoals = FXCollections.observableArrayList();
//    private final ListView<MsrdGoal> msrdGoalsList = new ListView<>(msrdGoals);
//    private final Button addMsrdGoalButton = new Button("Add MSRD Goal");

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