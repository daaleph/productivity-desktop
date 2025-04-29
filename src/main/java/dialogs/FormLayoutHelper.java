package dialogs;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

public class FormLayoutHelper {
    public static void addFormRow(
            String labelText,
            GridPane grid,
            Node field,
            int row
    ) {
        grid.add(new Label(labelText), 0, row);
        grid.add(field, 1, row);
    }
}