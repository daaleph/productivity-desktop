package home.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

public class Home {

    @FXML
    private GridPane containerGrid;

    private Stage stage;

    public void setStage(Stage stage) {
        this.stage = stage;
        initializeLayoutListener();
    }

    @FXML
    public void initialize() {
        // Initial setup can go here if you don't need the stage yet
        if (containerGrid == null) {
            System.err.println("containerGrid is null. Check fx:id in FXML.");
        }
    }

    private void initializeLayoutListener() {
        if (stage == null || containerGrid == null) {
            System.err.println("Cannot initialize listener: stage or containerGrid is null.");
            return;
        }

        stage.getScene().widthProperty().addListener((obs, oldWidth, newWidth) -> {
            double width = newWidth.doubleValue();
            double height = stage.getScene().getHeight();

            if (width > height) {
                // Two-column layout
                containerGrid.getChildren().forEach(node -> {
                    if (node instanceof Label) {
                        String id = ((Label) node).getId();
                        switch (id) {
                            case "container1":
                                GridPane.setConstraints(node, 0, 0, 2, 1);
                                break;
                            case "subContainer1":
                                GridPane.setConstraints(node, 0, 1);
                                break;
                            case "subContainer2":
                                GridPane.setConstraints(node, 1, 1);
                                break;
                            case "subContainer3":
                                GridPane.setConstraints(node, 0, 2);
                                break;
                            case "subContainer4":
                                GridPane.setConstraints(node, 1, 2);
                                break;
                        }
                    }
                });
            } else {
                // One-column layout
                containerGrid.getChildren().forEach(node -> {
                    if (node instanceof Label) {
                        String id = ((Label) node).getId();
                        switch (id) {
                            case "container1":
                                GridPane.setConstraints(node, 0, 0);
                                break;
                            case "subContainer1":
                                GridPane.setConstraints(node, 0, 1);
                                break;
                            case "subContainer2":
                                GridPane.setConstraints(node, 0, 2);
                                break;
                            case "subContainer3":
                                GridPane.setConstraints(node, 0, 3);
                                break;
                            case "subContainer4":
                                GridPane.setConstraints(node, 0, 4);
                                break;
                        }
                    }
                });
            }
        });
    }
}