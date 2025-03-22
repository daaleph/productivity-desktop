package home.controllers;

import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

public class Home {

    @FXML private GridPane containerGrid;
    @FXML private Button minimizeButton, maximizeButton, closeButton;

    private Stage stage;
    private boolean isMaximized = false;

    public void setStage(Stage stage) {
        this.stage = stage;
        initializeWindowControls();
        initializeLayoutListener(); // Now called AFTER stage has scene
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
        adjustLayout(); // Initial layout setup
    }

    private void adjustLayout() {
        boolean isWide = stage.getScene().getWidth() > stage.getScene().getHeight();

        containerGrid.getChildren().forEach(node -> {
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

        // Calculate target position based on layout
        int[] target = calculateTargetPosition(id, isWide);
        GridPane.setConstraints(node, target[0], target[1], target[2], target[3]);

        // Force layout to update positions before animating
        containerGrid.layout();

        // Animate to new position
        tt.setToX(node.getLayoutX() - node.getBoundsInParent().getMinX());
        tt.setToY(node.getLayoutY() - node.getBoundsInParent().getMinY());
        tt.play();
    }

    private int[] calculateTargetPosition(String id, boolean isWide) {
        int[] result = new int[4]; // col, row, colspan, rowspan
        switch (id) {
            case "container1":
                result = isWide ? new int[]{0, 0, 2, 1} : new int[]{0, 0, 1, 1};
                break;
            case "subContainer1":
                result = isWide ? new int[]{0, 1, 1, 1} : new int[]{0, 1, 1, 1};
                break;
            case "subContainer2":
                result = isWide ? new int[]{1, 1, 1, 1} : new int[]{0, 2, 1, 1};
                break;
            case "subContainer3":
                result = isWide ? new int[]{0, 2, 1, 1} : new int[]{0, 3, 1, 1};
                break;
            case "subContainer4":
                result = isWide ? new int[]{1, 2, 1, 1} : new int[]{0, 4, 1, 1};
                break;
        }
        return result;
    }

}