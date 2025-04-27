package dialogs;

import home.MainUser;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import services.ApiException;

import java.util.ArrayList;
import java.util.List;

public abstract class Entity<T> extends Stage {
    protected final MainUser mainUser;

    protected final GridPane grid = new GridPane();
    protected final ScrollPane scrollPane = new ScrollPane(grid);
    protected final Button submitButton = new Button("Submit");
    protected final Button cancelButton = new Button("Cancel");

    protected final List<Entity<?>> childDialogs = new ArrayList<>();

    protected Entity(String title, MainUser mainUser) {
        this.mainUser = mainUser;
        initModality(Modality.NONE);
        initializeUI(title);
        setupCloseHandler();
    }

    private void setupCloseHandler() {
        setOnHidden(e -> {
            closeChildDialogs();
            cleanup();
        });
    }

    public void addChildDialog(Entity<?> child) {
        childDialogs.add(child);
    }

    protected void closeChildDialogs() {
        new ArrayList<>(childDialogs).forEach(Entity::close);
        childDialogs.clear();
    }

    private void initializeUI(String title) {
        grid.setVgap(10);
        grid.setHgap(10);
        grid.setPadding(new javafx.geometry.Insets(20));

        var scene = new Scene(scrollPane);
        setScene(scene);
        setTitle(title);

        setupButtons();
    }

    protected abstract void cleanup();
    protected abstract void initializeForm();
    protected abstract void addFormRows();
    protected abstract void setupDynamicBehaviors();
    protected abstract void createAlphanumericValidations();
    protected abstract T validateAndCreate() throws ValidationException;

    private void setupButtons() {
        grid.add(submitButton, 0, 12);
        grid.add(cancelButton, 1, 12);

        submitButton.setOnAction(ae -> {
            try {
                T entity = validateAndCreate();
                close();
            } catch (ValidationException | ApiException e) {
                showError(e.getMessage());
            }
        });

        cancelButton.setOnAction(e -> {
            close();
            onCancel();
        });
    }

    protected void onCancel() {}
    protected void showError(String message) {}
}