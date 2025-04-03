package home.controllers;

import home.models.organizations.UserOrganization;
import home.models.projects.CoreProject;
import home.models.User;
import home.records.Priority;
import javafx.animation.TranslateTransition;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.List;
import java.util.Map;

public class Home {
    @FXML private GridPane gridContainer;
    @FXML private Label welcome, userAge, userName;
    @FXML private ListView<Priority> userPriorities;
    @FXML private ListView<CoreProject> userCoreProjects;
    @FXML private Button minimizeButton, maximizeButton, closeButton;
    @FXML private Map<Integer, UserOrganization> userOrganizations;

    public VBox profileSubContainer, userOrganizationsSubContainer, favoriteProjectsSubContainer, userBranchesSubContainer, welcomeSubContainer;

    private Stage stage;
    private boolean isMaximized = false;

    public void setUser(User user) {
        double cellHeight = 24;
        double padding = 2;
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

    public void setUserOrganizations(User user) {
        this.userOrganizations = user.getOrganizations();
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
        // Wait for scene to be available
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