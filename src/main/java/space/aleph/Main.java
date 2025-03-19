package space.aleph;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.util.stream.IntStream;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Create UI components
        TextArea outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setPrefSize(300, 200);

        // Window control buttons
        Button minimizeBtn = new Button("_");
        Button maximizeBtn = new Button("□");
        Button closeBtn = new Button("X");

        // Button actions
        minimizeBtn.setOnAction(e -> primaryStage.setIconified(true));
        maximizeBtn.setOnAction(e -> primaryStage.setMaximized(!primaryStage.isMaximized()));
        closeBtn.setOnAction(e -> Platform.exit());

        // Style buttons
        minimizeBtn.setStyle("-fx-font-size: 12; -fx-padding: 2 8;");
        maximizeBtn.setStyle("-fx-font-size: 12; -fx-padding: 2 8;");
        closeBtn.setStyle("-fx-font-size: 12; -fx-padding: 2 8; -fx-background-color: #ff5555;");

        // Layout for buttons
        HBox buttonBox = new HBox(5, minimizeBtn, maximizeBtn, closeBtn);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(5));

        // Main layout
        VBox root = new VBox(10, buttonBox, outputArea);
        root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: #f0f0f0;");

        // Scene setup
        Scene scene = new Scene(root, 400, 300);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Productivity App");
        primaryStage.show();

        // Simulate the original program's output with a 1-second delay
        new Thread(() -> {
            try {
                Platform.runLater(() -> outputArea.appendText("Hello and welcome!\n"));
                Thread.sleep(1000); // Pause for 1 second

                IntStream.rangeClosed(1, 5).forEach(i -> {
                    Platform.runLater(() -> outputArea.appendText("i = " + i + "\n"));
                    try {
                        Thread.sleep(1000); // Pause for 1 second per iteration
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static void main(String[] args) {
        launch(args); // Launch the JavaFX application
    }
}