// home/Home.java
package home;

import home.models.User;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import home.controllers.Home;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        User user = User.getInstance("nicalcoca@gmail.com");

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/home.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        Home controller = loader.getController();

        if (controller != null) {
            controller.setStage(primaryStage);
            controller.setUser(user);
        } else {
            System.err.println("Controller not found. Check fx:controller in FXML.");
        }

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}