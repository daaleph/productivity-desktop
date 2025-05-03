// java/home/Main.java
package home;

import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Parent;
import home.controllers.Home;
import javafx.fxml.FXMLLoader;
import javafx.application.Application;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {

        MainUser mainUser = MainUser.getInstance("nicalcoca@gmail.com");

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/home.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        Home controller = loader.getController();

        if (controller != null) {
            controller.setMainUser(mainUser);
            controller.setUserPriorities();
            controller.setUserCoreProjects();
            controller.setUserFavoriteProjects();
            controller.setUserProjects();
            controller.setUserOrganizations();
            controller.setUserBranches();
            controller.setStage(primaryStage);
        } else {
            System.err.println("Controller not found. Check fx:controller in FXML.");
        }

        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}