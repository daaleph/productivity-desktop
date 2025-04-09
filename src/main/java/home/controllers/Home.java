package home.controllers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.List;
import home.models.User;
import home.models.branchs.Branch;
import home.models.branchs.UserBranch;
import home.models.projects.Project;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import home.records.Priority;
import javafx.scene.control.*;
import home.models.projects.CoreProject;
import javafx.collections.FXCollections;
import javafx.animation.TranslateTransition;
import home.models.organizations.UserOrganization;

public class Home {
    private User user;
    double padding = 5;
    double cellHeight = 24;
    @FXML private GridPane gridContainer;
    @FXML private FlowPane userFavoriteProjects;
    @FXML private Label welcome, userAge, userName;
    @FXML private ListView<Priority> userPriorities;
    @FXML private Map<Integer, UserBranch> userBranches;
    @FXML private ListView<CoreProject> userCoreProjects;
    @FXML private Map<Integer, UserOrganization> userOrganizations;
    @FXML private Button minimizeButton, maximizeButton, closeButton;

    public VBox leftColumn,
            organizationsContainer,
            profileSubContainer,
            userOrganizationsSubContainer,
            favoriteProjectsSubContainer,
            userBranchesSubContainer,
            welcomeSubContainer;
    public FlowPane branchesContainer;

    private Stage stage;
    private boolean isMaximized = false;

    public void setUser(User user) {
        this.user = user;
        if (user != null) {
            welcome.setText("How are you feeling today " + user.getPreferredName());
            userName.setText("Name: " + user.getName());
            userAge.setText("Age: " + user.getAge());
        }
    }

    public void setUserOrganizations() {
        this.userOrganizations = user.getOrganizations();
        populateOrganizations();
    }

    public void setUserBranches() {
        this.userBranches = user.getBranches();
        populateBranches();
    }

    public void setUserPriorities() {
        List<Priority> priorities = user.getPriorities().values().stream().toList();
        userPriorities.setItems(FXCollections.observableArrayList(priorities));
        userPriorities.setCellFactory(lvp -> new ListCell<>() {
            @Override
            protected void updateItem(Priority item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.descriptionEs());
            }
        });
        userPriorities.setPrefHeight(priorities.size() * cellHeight + padding);
        userPriorities.setMaxHeight(Control.USE_PREF_SIZE);
    }

    public void setUserCoreProjects() {
        List<CoreProject> coreProjects = user.getCoreProjects().values().stream().toList();
        userCoreProjects.setItems(FXCollections.observableArrayList(coreProjects));
        userCoreProjects.setCellFactory(lvcp -> new ListCell<>() {
            @Override
            protected void updateItem(CoreProject item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getName());
            }
        });
        userCoreProjects.setPrefHeight(coreProjects.size() * cellHeight + padding);
        userCoreProjects.setMaxHeight(Control.USE_PREF_SIZE);
    }

    public void setUserFavoriteProjects() {
        List<Project> favoriteProjects = user.getFavoriteProjects().values().stream().toList();
        userFavoriteProjects.getChildren().clear();
        favoriteProjects.forEach(project -> {
            Label projectLabel = new Label(project.getName());
            projectLabel.getStyleClass().add("favorite-project-label");
            userFavoriteProjects.getChildren().add(projectLabel);
        });
    }

    public void setStage(Stage stage) {
        this.stage = stage;
        initializeWindowControls();
        initializeLayoutListener();
    }

    private void initializeWindowControls() {
        minimizeButton.setOnAction(e -> stage.setIconified(true));
        maximizeButton.setOnAction(e -> toggleMaximize());
        closeButton.setOnAction(e -> stage.close());
    }

    private void toggleMaximize() {
        isMaximized = !isMaximized;
        stage.setMaximized(isMaximized);
    }

    private void initializeLayoutListener() {
        if (stage.getScene() == null) {
            stage.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) setupResponsiveListener();
            });
        } else {
            setupResponsiveListener();
        }
    }

    private void setupResponsiveListener() {
        stage.getScene().widthProperty().addListener((obs, oldVal, newVal) -> {
            adjustLayout();
        });
        adjustLayout();
    }

    private void adjustLayout() {
        boolean isPortrait = stage.getScene().getHeight() > stage.getScene().getWidth();

        gridContainer.getChildren().forEach(node -> {
            if (node instanceof VBox) {
                String id = node.getId();
                animateLayoutChange((VBox) node, id, isPortrait);
            }
        });
    }

    private void animateLayoutChange(VBox node, String id, boolean isWide) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(300), node);
        tt.setFromX(node.getTranslateX());
        tt.setFromY(node.getTranslateY());

        int[] target = calculateTargetPosition(id, isWide);
        GridPane.setConstraints(node, target[0], target[1], target[2], target[3]);

        gridContainer.layout();

        tt.setToX(node.getLayoutX() - node.getBoundsInParent().getMinX());
        tt.setToY(node.getLayoutY() - node.getBoundsInParent().getMinY());
        tt.play();
    }

    private int[] calculateTargetPosition(String id, boolean isPortrait) {
        return switch (id) {
            case "welcomeSubContainer" -> new int[]{0, 0, 2, 1};
            case "profileSubContainer" -> isPortrait ?
                    new int[]{0, 1, 2, 1} :
                    new int[]{0, 1, 1, 1};
            case "userOrganizationsSubContainer" -> isPortrait ?
                    new int[]{0, 2, 2, 1} :
                    new int[]{1, 1, 1, 1};
            case "favoriteProjectsSubContainer" -> isPortrait ?
                    new int[]{0, 3, 2, 1} :
                    new int[]{0, 2, 1, 1};
            case "userBranchesSubContainer" -> isPortrait ?
                    new int[]{0, 4, 2, 1} :
                    new int[]{1, 2, 1, 1};
            default -> new int[]{0, 0, 1, 1};
        };
    }

    private void populateOrganizations() {
        organizationsContainer.getChildren().clear();
        userOrganizations
                .values()
                .stream()
                .sorted(Comparator.comparingInt(UserOrganization::getId))
                .forEach(org -> {
                    HBox orgRow = createOrganizationRow(org);
                    organizationsContainer.getChildren().add(orgRow);
                });
    }

    private void populateBranches() {
        branchesContainer.getChildren().clear();
        userBranches.values().stream()
                .sorted(Comparator.comparingInt(UserBranch::getId))
                .forEach(branch -> {
                    VBox branchTile = createBranchTile(branch);
                    branchesContainer.getChildren().add(branchTile);
                });
    }

    private VBox createBranchTile(UserBranch branch) {
        VBox container = new VBox(15);
        container.getStyleClass().add("branch-tile");
        Label branchLabel = new Label(branch.getName());
        branchLabel.getStyleClass().add("branch-name");

        double labelWidth = branchLabel.prefWidth(-1);
        double wrapWidth = labelWidth; // initial wrap width

        List<HBox> projectRows = new ArrayList<>();
        HBox currentRow = new HBox(5); // assuming hgap is 5
        double currentWidth = 0;

        for (Project project : branch.getUserBelonging().projects().stream()
                .sorted(Comparator.comparing(Project::getDateToStart))
                .toList()) {
            Label projectLabel = createProjectLabel(project);
            double projectWidth = projectLabel.prefWidth(-1);
            if (currentWidth + projectWidth > wrapWidth && !currentRow.getChildren().isEmpty()) {
                projectRows.add(currentRow);
                currentRow = new HBox(5);
                currentWidth = 0;
            }
            currentRow.getChildren().add(projectLabel);
            currentWidth += projectWidth + 5; // hgap
        }
        if (!currentRow.getChildren().isEmpty()) {
            projectRows.add(currentRow);
        }

        // Calculate max row width
        double maxRowWidth = projectRows.stream()
                .mapToDouble(row -> row.getChildren().stream()
                        .mapToDouble(node -> node.prefWidth(-1))
                        .sum() + (row.getChildren().size() - 1) * 5)
                .max()
                .orElse(0);

        // Set container prefWidth
        double padding = container.getPadding().getLeft() + container.getPadding().getRight();
        container.setPrefWidth(Math.max(labelWidth, maxRowWidth) + padding);

        // Add branchLabel and projectRows to container
        container.getChildren().add(branchLabel);
        container.getChildren().addAll(projectRows);

        return container;
    }

    private HBox createOrganizationRow(UserOrganization userOrg) {
        HBox container = new HBox(15);
        container.getStyleClass().add("organization-container");
        Label orgLabel = new Label(userOrg.getName());
        orgLabel.getStyleClass().add("organization-name");
        orgLabel.setWrapText(false);
        orgLabel.setEllipsisString("");
        Text textMeasure = new Text(userOrg.getName());
        textMeasure.setFont(orgLabel.getFont());
        textMeasure.applyCss();
        double textWidth = textMeasure.getLayoutBounds().getWidth();
        orgLabel.setMinWidth(textWidth * 3 / 2);
        orgLabel.setPrefWidth(textWidth * 3 / 2);
        orgLabel.setMaxWidth(Double.MAX_VALUE);
        FlowPane branchesPane = new FlowPane(8, 5);
        branchesPane.getStyleClass().add("branches-flow");

        branchesPane.prefWrapLengthProperty().bind( // Dynamic binding for responsive wrapping
                organizationsContainer.widthProperty()
                        .subtract(orgLabel.getMinWidth()) // Use fixed min width
                        .subtract(30) // Account for container padding
        );

        userOrg.getBranches().values().stream() // 3. Add Branches
                .sorted(Comparator.comparingInt(Branch::getId))
                .forEach(branch -> {
                    Label branchLabel = createBranchLabel(branch);
                    branchesPane.getChildren().add(branchLabel);
                });

        container.getChildren().addAll(orgLabel, branchesPane);
        return container;
    }

    private Label createBranchLabel(Branch branch) {
        Label label = new Label(branch.getName());
        label.getStyleClass().add("branch-label");
        Path branchIcon = new Path(
                new MoveTo(4, 0),
                new LineTo(4, 8),
                new MoveTo(4, 4),
                new LineTo(8, 6),
                new LineTo(4, 8),
                new MoveTo(4, 4),
                new LineTo(0, 6),
                new LineTo(4, 8)
        );
        branchIcon.setStroke(Color.web("#6c757d"));
        branchIcon.setStrokeWidth(1.5);
        branchIcon.setStrokeLineCap(StrokeLineCap.ROUND);
        branchIcon.setStrokeLineJoin(StrokeLineJoin.ROUND);

        StackPane iconContainer = new StackPane(branchIcon); // Wrap in StackPane for proper sizing
        iconContainer.setPadding(new Insets(0, 3, 0, 0));

        label.setGraphic(iconContainer);
        label.setContentDisplay(ContentDisplay.LEFT);
        label.setGraphicTextGap(5);

        return label;
    }

    private Label createProjectLabel(Project project) {
        Label label = new Label(project.getName());
        label.getStyleClass().add("project-label");
        Path branchIcon = new Path(
                new MoveTo(4, 0),
                new LineTo(4, 8),
                new MoveTo(4, 4),
                new LineTo(8, 6),
                new LineTo(4, 8),
                new MoveTo(4, 4),
                new LineTo(0, 6),
                new LineTo(4, 8)
        );
        branchIcon.setStroke(Color.web("#6c757d"));
        branchIcon.setStrokeWidth(1.5);
        branchIcon.setStrokeLineCap(StrokeLineCap.ROUND);
        branchIcon.setStrokeLineJoin(StrokeLineJoin.ROUND);

        StackPane iconContainer = new StackPane(branchIcon); // Wrap in StackPane for proper sizing
        iconContainer.setPadding(new Insets(0, 3, 0, 0));

        label.setGraphic(iconContainer);
        label.setContentDisplay(ContentDisplay.LEFT);
        label.setGraphicTextGap(5);

        return label;
    }

    public Map<Integer, UserOrganization> getUserOrganizations() {
        return this.userOrganizations;
    }
}