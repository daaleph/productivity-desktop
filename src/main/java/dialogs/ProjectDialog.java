package dialogs;

import home.MainUser;
import javafx.beans.binding.Bindings;
import javafx.scene.layout.*;
import model.projects.EssentialInfo;
import model.projects.Project;
import records.MeasuredGoal;
import records.MeasuredSet;
import records.Priority;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import static data.Abbreviations.getAbbreviation;
import static dialogs.FieldConfigurator.configureSelectableListView;
import static dialogs.FormLayoutHelper.addFormRow;
import static dialogs.Questions.*;

public class ProjectDialog extends Entity<Project> {

    private static ProjectDialog instance;
    private static final Logger LOGGER = Logger.getLogger(ProjectDialog.class.getName());

    private final BooleanProperty nameValid = new SimpleBooleanProperty(false);
    private final BooleanProperty daysValid = new SimpleBooleanProperty(false);
    private final BooleanProperty weeksValid = new SimpleBooleanProperty(false);
    private final BooleanProperty monthsValid = new SimpleBooleanProperty(false);
    private final BooleanProperty yearsValid = new SimpleBooleanProperty(false);
    private final BooleanProperty priorityValid = new SimpleBooleanProperty(false);
    private final BooleanProperty descriptionValid = new SimpleBooleanProperty(false);
    private final BooleanProperty measuredGoalsValid = new SimpleBooleanProperty(false);

    private final TextField nameField = new TextField(PROJECT_NAME.get());
    private final TextField completingDays = new TextField(COMPLETING_DAYS.get());
    private final TextField completingWeeks = new TextField(COMPLETING_WEEKS.get());
    private final TextField completingMonths = new TextField(COMPLETING_MONTHS.get());
    private final TextField completingYears = new TextField(COMPLETING_YEARS.get());
    private final ToggleGroup projectTypeGroup = new ToggleGroup();
    private final RadioButton personalRadio = new RadioButton("Personal");
    private final RadioButton organizationalRadio = new RadioButton("Organizational");
    private final HBox typeBox = new HBox(10, personalRadio, organizationalRadio);
    private final ListView<Priority> priorityList = new ListView<>();
    private final ListView<Project> parentProjects = new ListView<>();
    private final ListView<MeasuredGoal> measuredGoals = new ListView<>();
    private final ObservableList<MeasuredGoal> observableMeasuredGoals = FXCollections.observableArrayList();
    private final TextField descriptionField = new TextField(PROJECT_DESCRIPTION.get());
    private final CheckBox isFavorite = new CheckBox();
    private final Button buttonAddMeasuredGoal = new Button("Add Measured Goal");
    private final Button buttonDeleteMeasuredGoal = new Button("Delete Selected Goals");

    public ProjectDialog(MainUser mainUser) {
        super("New Project", mainUser);
        if (mainUser == null) {
            LOGGER.log(Level.SEVERE, "MainUser is null in ProjectDialog constructor!");
            throw new IllegalArgumentException("MainUser cannot be null.");
        }
        initializeForm();
    }

    public static synchronized ProjectDialog getInstance(MainUser mainUser) {
        if (instance == null) {
            instance = new ProjectDialog(mainUser);
            instance.setOnShown(e -> instance.toFront());
        }
        return instance;
    }

    @Override
    protected Project validateAndCreate() throws ValidationException {
        String projectName = nameField.getText().trim();
        if (projectName.isEmpty()) throw new ValidationException("Project name cannot be empty.");

        ObservableList<Priority> currentlySelected = priorityList.getSelectionModel().getSelectedItems();
        if (currentlySelected.isEmpty()) throw new ValidationException("Must select at least one priority.");

        EssentialInfo essentialInfo = new EssentialInfo(
                projectName,
                personalRadio.isSelected() ? 0 : 1,
                isFavorite.isSelected(),
                ZonedDateTime.now()
        );
        Map<String, Integer> necessaryTimeMap = Map.of(
                getAbbreviation("completingDays"), Integer.parseInt(completingDays.getText().trim()),
                getAbbreviation("completingWeeks"), Integer.parseInt(completingWeeks.getText().trim()),
                getAbbreviation("completingMonths"), Integer.parseInt(completingMonths.getText().trim()),
                getAbbreviation("completingYears"), Integer.parseInt(completingYears.getText().trim())
        );
        MeasuredSet<Integer> necessaryTimeSet = MeasuredSet.create(Integer.class, necessaryTimeMap);
        Project.ProjectInfo projectInfo = new Project.ProjectInfo(
                essentialInfo,
                List.copyOf(currentlySelected),
                observableMeasuredGoals,
                necessaryTimeSet,
                List.of(),
                getParentUUIDSFromList()
        );

        Project project = new Project(UUID.randomUUID());
        project.setInfo(projectInfo);
        setResult(project);
        return project;
    }

    @Override
    protected void initializeForm() {
        grid.setStyle("-fx-padding: 20; -fx-vgap: 15; -fx-hgap: 10;");
        addFormRows();
        configureNotListViews();
        configureListViews();
        createListingValidations();
        createAlphanumericValidations();
        setupDynamicBehaviors();
    }

    @Override
    protected void setupDynamicBehaviors() {
        priorityList.setCellFactory(this::createDynamicStyledPriorityCell);
        parentProjects.setCellFactory(this::createDynamicStyledProjectCell);
        measuredGoals.setCellFactory(this::createDynamicStyledMeasuredGoalCell);
        buttonAddMeasuredGoal.setOnAction(e -> showMeasuredGoalDialog());

        buttonDeleteMeasuredGoal.setOnAction((e) -> {
            observableMeasuredGoals.removeAll(measuredGoals.getSelectionModel().getSelectedItems());
            childDialogs.getLast().cleanup();
        });
        buttonDeleteMeasuredGoal.disableProperty().bind(Bindings.isEmpty(measuredGoals.getSelectionModel().getSelectedItems()));

        submitButton.disableProperty().bind(
                nameValid.not().or(daysValid.not()).or(monthsValid.not())
                .or(yearsValid.not()).or(priorityValid.not()).or(measuredGoalsValid.not())
        );
    }

    @Override
    protected void logObjectStructure() {
        LOGGER.info("=== Project Structure ===");
        this.getResult(Project.class).logEntity();
        LOGGER.info("=========================");
    }

    @Override
    protected void createAlphanumericValidations() {
        nameValid.bind(FieldConfigurator.forText(nameField, "Enter project name", PROJECT_NAME, 3, 255));
        daysValid.bind(FieldConfigurator.forGregorianTimeCategories(completingDays, "Necessary days", 0, 6));
        weeksValid.bind(FieldConfigurator.forGregorianTimeCategories(completingWeeks, "Necessary weeks",0, 3));
        monthsValid.bind(FieldConfigurator.forGregorianTimeCategories(completingMonths, "Necessary months",0, 11));
        yearsValid.bind(FieldConfigurator.forGregorianTimeCategories(completingYears, "Necessary years",0, 100));
        descriptionValid.bind(FieldConfigurator.forText(descriptionField, "The description", PROJECT_DESCRIPTION,20, 2000));
    }

    private void createListingValidations() {
        priorityValid.bind(FieldConfigurator.forListViewSelector(
            priorityList,
            mainUser.getPriorities(),
            "No priorities available.",
            "Populated priority list with {0} items.",
            "No priorities found for user.")
        );
        measuredGoalsValid.bind(FieldConfigurator.forFillableListView(measuredGoals, "measured goal", true));
    }

    protected void addFormRows() {
        addFormRow("Project Name:", grid, nameField, 0);
        addFormRow("Project Type:", grid, typeBox, 1);
        addFormRow("Priority (Click to select/deselect):", grid, priorityList, 2);
        addFormRow("Parent Projects (Click to select/deselect):", grid, parentProjects, 3);
        addFormRow("Completing years:", grid, completingYears, 4);
        addFormRow("Completing months:", grid, completingMonths, 5);
        addFormRow("Completing weeks:", grid, completingWeeks, 6);
        addFormRow("Completing days:", grid, completingDays, 7);
        addFormRow("The details!", grid, descriptionField, 8);
        addFormRow("Will you enjoy it?", grid, isFavorite, 9);
        addFormRow("Measured Goals:", grid, measuredGoals, 10);
        HBox buttonContainer = new HBox(10, buttonAddMeasuredGoal, buttonDeleteMeasuredGoal);
        addFormRow("", grid, buttonContainer, 11);
    }

    private void configureNotListViews() {
        personalRadio.setToggleGroup(projectTypeGroup);
        organizationalRadio.setToggleGroup(projectTypeGroup);
        personalRadio.setSelected(true);
    }

    private void configureListViews() {
        configureSelectableListView(
            parentProjects,
            mainUser.getProjects(),
            "No parent projects available.",
            "Populated parent projects list to select with {0} items.",
            "No parent projects found for user."
        );
        measuredGoals.setMinHeight(100);
        measuredGoals.setPrefHeight(100);
        measuredGoals.setItems(observableMeasuredGoals);
    }

    private ListCell<Priority> createDynamicStyledPriorityCell(ListView<Priority> listView) {
        return createStyledListCell(Priority::getName);
    }

    private ListCell<Project> createDynamicStyledProjectCell(ListView<Project> listView) {
        return createStyledListCell(Project::getName);
    }

    private ListCell<MeasuredGoal> createDynamicStyledMeasuredGoalCell(ListView<MeasuredGoal> listView) {
        return createStyledListCell(MeasuredGoal::item);
    }

    private void showMeasuredGoalDialog() {
        MeasuredGoalDialog dialog = MeasuredGoalDialog.getInstance(mainUser);
        dialog.show();
        dialog.setOnHidden(e -> {
            MeasuredGoal result = dialog.getResult(MeasuredGoal.class);
            if (result != null) {
                observableMeasuredGoals.add(result);
                dialog.cleanup();
            }
        });
        this.addChildDialog(dialog);
    }

    private ArrayList<UUID> getParentUUIDSFromList() {
        ArrayList<UUID> features = new ArrayList<>();
        List<Project> items = parentProjects.getItems();
        for (Project item : items) {
            features.add(item.getUuid());
        }
        return features;
    }

    @Override
    protected void cleanup() { instance = null; }
}