package dialogs;

import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionModel;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;

import java.util.function.Function;

public class UIComponentFactory {

    protected static final Color SELECTED_COLOR = Color.rgb(100, 149, 237, 0.8);
    protected static final Color SELECTED_TEXT_COLOR = Color.WHITE;
    protected static final Color UNSELECTED_TEXT_COLOR = Color.BLACK;
    protected static final BackgroundFill SELECTED_BACKGROUND_FILL =
            new BackgroundFill(SELECTED_COLOR, new CornerRadii(3), Insets.EMPTY);
    protected static final Background SELECTED_BACKGROUND = new Background(SELECTED_BACKGROUND_FILL);
    protected static final Background UNSELECTED_BACKGROUND = Background.EMPTY;
    protected static final PseudoClass SELECTED_PSEUDO_CLASS =
            PseudoClass.getPseudoClass("custom-selected");

    public static <S> ListCell<S> createStyledListCell(
            Function<S, String> textExtractor
    ) {
        return new ListCell<>() {
            {
                addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
                    if (isEmpty() || getItem() == null) return;
                    ListView<S> lv = getListView();
                    SelectionModel<S> sm = lv.getSelectionModel();
                    int index = getIndex();

                    if (sm.isSelected(index)) {
                        sm.clearSelection(index);
                    } else {
                        sm.select(index);
                    }
                    event.consume();
                });
            }

            @Override
            protected void updateItem(S item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    applyCellStyle(this, false);
                    pseudoClassStateChanged(SELECTED_PSEUDO_CLASS, false);
                } else {
                    setText(textExtractor.apply(item));
                    setGraphic(null);
                    boolean isSelected = getListView().getSelectionModel().isSelected(getIndex());
                    applyCellStyle(this, isSelected);
                    pseudoClassStateChanged(SELECTED_PSEUDO_CLASS, isSelected);
                }
            }
        };
    }

    public static <S> void applyCellStyle(
            ListCell<S> cell,
            boolean isSelected
    ) {
        cell.setBackground(isSelected ? SELECTED_BACKGROUND : UNSELECTED_BACKGROUND);
        cell.setTextFill(isSelected ? SELECTED_TEXT_COLOR : UNSELECTED_TEXT_COLOR);
    }
}