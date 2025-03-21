package home;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.text.Text;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;

import home.model.Branch;
import home.model.Organization;
import home.model.User;
import java.net.URL;
import java.util.stream.IntStream;


public class Main extends Application {

    private static final String FXML_FILE = "/home.fxml"; // Name of your FXML file
    private MyController controller;

    @Override
    public void start(Stage primaryStage) {
        try {
            URL url = getClass().getResource(FXML_FILE);
            if (url == null) {
                System.err.println("Cannot find FXML file at: " + FXML_FILE);
                Platform.exit(); // Exit if FXML file not found
                return;
            }
            FXMLLoader loader = new FXMLLoader(url);
            javafx.scene.layout.BorderPane root = loader.load(); // Load the FXML

            controller = loader.getController(); // Get the controller instance

            Scene scene = new Scene(root, 1200, 800);
            primaryStage.setScene(scene);
            primaryStage.setTitle("Productivity App");
            primaryStage.show();

            //simulateOriginalProgramOutput(); // Simulate original text output Moved after controller injection
            Platform.runLater(this::simulateOriginalProgramOutput); //Call the original text simulation

        } catch (IOException e) {
            e.printStackTrace();
            // Handle the exception (show an error message, etc.)
        }
    }

    private void simulateOriginalProgramOutput() {
        // Directly access UI components through the controller
        Text motivationalPhraseText = controller.getMotivationalPhraseText();

        // Simulate output to your existing TextArea using Platform.runLater
        new Thread(() -> {
            try {
                Platform.runLater(() -> motivationalPhraseText.setText("Hello and welcome!\n"));
                Thread.sleep(1000); // Pause for 1 second

                IntStream.rangeClosed(1, 5).forEach(i -> {
                    Platform.runLater(() -> motivationalPhraseText.setText(motivationalPhraseText.getText() + "i = " + i + "\n"));
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

    //Define the Controller
    public static class MyController {

        @FXML
        private Label nameLabel;
        @FXML
        private Label ageLabel;
        @FXML
        private Label pref1Label;
        @FXML
        private Label pref2Label;
        @FXML
        private ListView<Organization> organizationList;
        @FXML
        private VBox branchContainer;
        @FXML
        private Text motivationalPhraseText;
        @FXML
        private Button minimizeBtn;
        @FXML
        private Button maximizeBtn;
        @FXML
        private Button closeBtn;


        @FXML
        public void initialize() {
            // Initialize application
            // Populate organizationList (assuming Organization has a getName() method)
            ObservableList<Organization> organizations = FXCollections.observableArrayList(getOrganizationsFromDatabase());
            organizationList.setItems(organizations);

            organizationList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null) {
                    updateBranchDisplay(newValue);
                }
            });

            //Initial user data
            User user = getUserData();
            nameLabel.textProperty().bind(user.nameProperty());
            ageLabel.textProperty().bind(user.ageProperty().toString());
            pref1Label.setText(user.getPriority(0));
            pref2Label.setText(user.getPriority(0));

            //Initial motivational phrase
            motivationalPhraseText.setText(getMotivationalPhrase());

            //Attach controls
            attachWindowControls();
        }

        private void updateBranchDisplay(Organization organization) {
            branchContainer.getChildren().clear(); // Clear previous branches
            List<Branch> branches = getBranchesForOrganization(organization);

            if (branches.size() == 1) {
                // Simple Label for a single branch
                Label branchLabel = new Label(branches.get(0).getName());
                branchContainer.getChildren().add(branchLabel);
            } else if (branches.size() > 1) {
                // FlowPane for multiple branches
                FlowPane branchFlowPane = new FlowPane(5, 5);
                for (Branch branch : branches) {
                    Label branchLabel = new Label(branch.getName()); // Or Buttons if you want interaction
                    branchFlowPane.getChildren().add(branchLabel);
                }
                branchContainer.getChildren().add(branchFlowPane);
            }
            // If there are no branches for organization, the container will remain empty
        }

        //Mock data
        private ObservableList<Organization> getOrganizationsFromDatabase(){
            ObservableList<Organization> orgs = FXCollections.observableArrayList();
            orgs.add(new Organization(1,"Org 1"));
            orgs.add(new Organization(2,"Org 2"));
            return orgs;
        }

        //Mock branch data
        private List<Branch> getBranchesForOrganization(Organization organization) {
            if (organization.getId() == 1) {
                return List.of(new Branch(1, "Branch A"), new Branch(2, "Branch B"), new Branch(3, "Branch C"));
            } else {
                return List.of(new Branch(4, "Branch D"));
            }
        }

        private User getUser(){
            String[] priorities = {"Preference 1", "Preference 2"};
            return new User("John Doe", 30, "nicalcoca@gmail.com", priorities);
        }

        public String getMotivationalPhrase(){
            return "The only way to do great work is to love what you do.";
        }

        // Getter for accessing motivationalPhraseText from Main class
        public Text getMotivationalPhraseText() {
            return motivationalPhraseText;
        }

        @FXML
        private void attachWindowControls() {
            minimizeBtn.setOnAction(e -> {
                Stage stage = (Stage) minimizeBtn.getScene().getWindow();
                stage.setIconified(true);
            });

            maximizeBtn.setOnAction(e -> {
                Stage stage = (Stage) maximizeBtn.getScene().getWindow();
                stage.setMaximized(!stage.isMaximized());
            });

            closeBtn.setOnAction(e -> Platform.exit());

            // Style buttons (moved from Main.java)
            minimizeBtn.setStyle("-fx-font-size: 12; -fx-padding: 2 8;");
            maximizeBtn.setStyle("-fx-font-size: 12; -fx-padding: 2 8;");
            closeBtn.setStyle("-fx-font-size: 12; -fx-padding: 2 8; -fx-background-color: #ff5555;");
        }
    }
}