// java/home/Main.java
package home;

import java.util.Map;
import home.models.User;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Parent;
import home.controllers.Home;
import javafx.fxml.FXMLLoader;
import javafx.application.Application;
import home.models.organizations.UserOrganization;

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
            controller.setUserOrganizations(user);
        } else {
            System.err.println("Controller not found. Check fx:controller in FXML.");
        }

        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}