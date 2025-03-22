package home;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import home.controllers.Home;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/home.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);

        // Set scene FIRST before passing stage to controller
        primaryStage.setScene(scene);
        primaryStage.initStyle(StageStyle.TRANSPARENT);

        Home controller = loader.getController();
        controller.setStage(primaryStage); // Now safe to pass stage

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}