// home/Home.java
package home;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import home.controllers.Home;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Load the FXML file
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/home.fxml"));
        Parent root = loader.load();

        // Create the scene
        Scene scene = new Scene(root);

        // Set the scene on the stage first
        primaryStage.setScene(scene);

        // Get the controller and pass the stage
        Home controller = loader.getController();
        if (controller != null) {
            controller.setStage(primaryStage);
        } else {
            System.err.println("Controller not found. Check fx:controller in FXML.");
        }

        // Show the stage
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}