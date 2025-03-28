package home.controllers;

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

public class Home {
    @FXML private GridPane gridContainer;
    @FXML private Label welcome, userAge, userName;
    @FXML private ListView<Priority> userPriorities;
    @FXML private Button minimizeButton, maximizeButton, closeButton;

    public VBox profileSubcontainer, userOrganizationsSubcontainer, favoriteProjectsSubcontainer, userBranchesSubcontainer, welcomeSubcontainer;

    private Stage stage;
    private boolean isMaximized = false;

    public void setUser(User user) {
        if (user != null) {
            welcome.setText("How are you feeling today " + user.getPreferredName());
            userName.setText("Name: " + user.getName());
            userAge.setText("Age: " + user.getAge());
            List<Priority> priorities = user.getPriorities();

            userPriorities.setItems(FXCollections.observableArrayList(priorities));
            userPriorities.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(Priority item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "" : item.descriptionEs());
                }
            });

            // Calculate preferred height
            int numItems = priorities.size();
            double cellHeight = 24; // Must match CSS -fx-fixed-cell-size
            double padding = 2; // Account for any listview padding
            userPriorities.setPrefHeight(numItems * cellHeight + padding);
            userPriorities.setMaxHeight(Control.USE_PREF_SIZE); // Prevent expansion
        }
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
        boolean isWide = stage.getScene().getWidth() > stage.getScene().getHeight();

        gridContainer.getChildren().forEach(node -> {
            if (node instanceof VBox) {
                String id = node.getId();
                animateLayoutChange((VBox) node, id, isWide);
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

    private int[] calculateTargetPosition(String id, boolean isWide) {
        int[] result = new int[4]; // col, row, colspan, rowspan
        result = switch (id) {
            case "container1" -> isWide ? new int[]{0, 0, 2, 1} : new int[]{0, 0, 1, 1};
            case "subContainer1" -> new int[]{0, 1, 1, 1};
            case "subContainer2" -> isWide ? new int[]{1, 1, 1, 1} : new int[]{0, 2, 1, 1};
            case "subContainer3" -> isWide ? new int[]{0, 2, 1, 1} : new int[]{0, 3, 1, 1};
            case "subContainer4" -> isWide ? new int[]{1, 2, 1, 1} : new int[]{0, 4, 1, 1};
            default -> result;
        };
        return result;
    }

}