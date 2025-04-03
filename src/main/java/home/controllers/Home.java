package home.controllers;

import java.util.Comparator;
import java.util.Map;
import java.util.List;
import home.models.User;
import home.models.branchs.Branch;
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
    @FXML private GridPane gridContainer;
    @FXML private Label welcome, userAge, userName;
    @FXML private ListView<Priority> userPriorities;
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

    private Stage stage;
    private boolean isMaximized = false;

    public void setUser(User user) {
        double cellHeight = 24;
        double padding = 5;
        if (user != null) {
            welcome.setText("How are you feeling today " + user.getPreferredName());
            userName.setText("Name: " + user.getName());
            userAge.setText("Age: " + user.getAge());
            List<Priority> priorities = user.getPriorities();
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

            List<CoreProject> coreProjects = user.getCoreProjects();
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

    private HBox createOrganizationRow(UserOrganization org) {
        HBox container = new HBox(15);
        container.getStyleClass().add("org-container");

        // 1. Email Label Setup
        Label emailLabel = new Label(org.getName());
        emailLabel.getStyleClass().add("org-name");
        emailLabel.setWrapText(false);
        emailLabel.setEllipsisString("");

        // Calculate exact width needed for the email text
        Text textMeasure = new Text(org.getName());
        textMeasure.setFont(emailLabel.getFont());
        textMeasure.applyCss();
        double textWidth = textMeasure.getLayoutBounds().getWidth();

        // Set fixed width with padding compensation
        emailLabel.setMinWidth(textWidth * 3 / 2); // Right padding
        emailLabel.setPrefWidth(textWidth * 3 / 2);
        emailLabel.setMaxWidth(Double.MAX_VALUE); // Allows growth if needed

        // 2. Branches Flow Pane Setup
        FlowPane branchesPane = new FlowPane(8, 5);
        branchesPane.getStyleClass().add("branches-flow");

        // Dynamic binding for responsive wrapping
        branchesPane.prefWrapLengthProperty().bind(
                organizationsContainer.widthProperty()
                        .subtract(emailLabel.getMinWidth()) // Use fixed min width
                        .subtract(30) // Account for container padding
        );

        // 3. Add Branches
        org.getBranches().values().stream()
                .sorted(Comparator.comparingInt(Branch::getId))
                .forEach(branch -> {
                    Label branchLabel = createBranchLabel(branch);
                    branchesPane.getChildren().add(branchLabel);
                });

        container.getChildren().addAll(emailLabel, branchesPane);
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

        // Wrap in StackPane for proper sizing
        StackPane iconContainer = new StackPane(branchIcon);
        iconContainer.setPadding(new Insets(0, 3, 0, 0));

        label.setGraphic(iconContainer);
        label.setContentDisplay(ContentDisplay.LEFT);
        label.setGraphicTextGap(5);

        return label;
    }

    public void setUserOrganizations(User user) {
        this.userOrganizations = user.getOrganizations();
        populateOrganizations();
    }

    public Map<Integer, UserOrganization> getUserOrganizations() {
        return this.userOrganizations;
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

}