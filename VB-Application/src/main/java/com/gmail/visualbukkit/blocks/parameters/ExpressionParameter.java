package com.gmail.visualbukkit.blocks.parameters;

import com.gmail.visualbukkit.VisualBukkitApp;
import com.gmail.visualbukkit.blocks.*;
import com.gmail.visualbukkit.gui.StyleableHBox;
import com.gmail.visualbukkit.plugin.BuildContext;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import org.controlsfx.control.PopOver;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Predicate;

public class ExpressionParameter extends VBox implements BlockParameter {

    private static Map<Class<?>, PopOver> expressionSelectors = new HashMap<>();
    private static ExpressionParameter currentExpressionParameter;

    private Class<?> type;
    private Expression.Block expression;
    private Button button;

    public ExpressionParameter(Class<?> type) {
        this.type = type;
        button = new Button("<" + TypeHandler.getUserFriendlyName(type) + ">");

        getChildren().add(button);
        getStyleClass().add("expression-parameter");

        PopOver selector = expressionSelectors.computeIfAbsent(type, k -> {
            ObservableList<Expression> expressions = FXCollections.observableArrayList();
            TreeSet<String> categories = new TreeSet<>();

            for (Expression expr : BlockRegistry.getExpressions()) {
                if (TypeHandler.canConvert(expr.getReturnType(), type)) {
                    expressions.add(expr);
                    if (expr.getCategory() != null) {
                        categories.add(expr.getCategory());
                    }
                }
            }

            FilteredList<Expression> expressionList = new FilteredList<>(expressions);
            ListView<Expression> listView = new ListView<>(expressionList);

            TextField searchField = new TextField();
            ComboBox<String> categoryBox = new ComboBox<>();
            categories.add(VisualBukkitApp.getString("label.all"));
            categoryBox.getItems().addAll(categories);

            Predicate<Expression> filter = expr ->
                    expr.getTitle().toLowerCase().contains(searchField.getText().toLowerCase())
                    && (categoryBox.getSelectionModel().getSelectedIndex() == 0 || categoryBox.getValue().equals(expr.getCategory()));

            searchField.textProperty().addListener((o, oldValue, newValue) -> expressionList.setPredicate(filter::test));
            categoryBox.valueProperty().addListener((o, oldValue, newValue) -> expressionList.setPredicate(filter::test));

            PopOver popOver = new PopOver(new VBox(new StyleableHBox(new Label(VisualBukkitApp.getString("label.search")), searchField, categoryBox), listView));
            popOver.setAnimated(false);
            popOver.setOnShowing(e -> {
                searchField.clear();
                categoryBox.getSelectionModel().selectFirst();
                listView.getSelectionModel().clearSelection();
            });

            listView.setPlaceholder(new Label(VisualBukkitApp.getString("label.empty_list")));
            listView.getSelectionModel().selectedItemProperty().addListener((o, oldValue, newValue) -> {
                if (newValue != null && currentExpressionParameter != null) {
                    UndoManager.run(currentExpressionParameter.setExpression(newValue.createBlock()));
                    popOver.hide();
                }
            });

            return popOver;
        });

        button.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && !selector.isShowing()) {
                currentExpressionParameter = this;
                selector.setArrowLocation(2 * e.getSceneY() > VisualBukkitApp.getInstance().getScene().getHeight() ?
                        PopOver.ArrowLocation.BOTTOM_LEFT :
                        PopOver.ArrowLocation.TOP_LEFT);
                selector.show(this);
            }
        });

        ContextMenu contextMenu = new ContextMenu();
        button.setContextMenu(contextMenu);

        if (type == boolean.class) {
            MenuItem booleanItem = new MenuItem(VisualBukkitApp.getString("context_menu.insert_boolean"));
            MenuItem equalsItem = new MenuItem(VisualBukkitApp.getString("context_menu.insert_equals"));
            contextMenu.getItems().addAll(booleanItem, equalsItem);
            booleanItem.setOnAction(e -> UndoManager.run(setExpression(BlockRegistry.getExpression("expr-boolean").createBlock())));
            equalsItem.setOnAction(e -> UndoManager.run(setExpression(BlockRegistry.getExpression("expr-is-equal").createBlock())));
        } else if (type == String.class) {
            MenuItem stringItem = new MenuItem(VisualBukkitApp.getString("context_menu.insert_string"));
            contextMenu.getItems().add(stringItem);
            stringItem.setOnAction(e -> UndoManager.run(setExpression(BlockRegistry.getExpression("expr-string").createBlock())));
        } else if (type == List.class) {
            MenuItem newListItem = new MenuItem(VisualBukkitApp.getString("context_menu.insert_list"));
            contextMenu.getItems().add(newListItem);
            newListItem.setOnAction(e -> UndoManager.run(setExpression(BlockRegistry.getExpression("expr-new-list").createBlock())));
        } else if (TypeHandler.isNumber(type)) {
            MenuItem numberItem = new MenuItem(VisualBukkitApp.getString("context_menu.insert_number"));
            contextMenu.getItems().add(numberItem);
            numberItem.setOnAction(e -> UndoManager.run(setExpression(BlockRegistry.getExpression("expr-number").createBlock())));
        }

        MenuItem pasteItem = new MenuItem(VisualBukkitApp.getString("context_menu.paste"));
        pasteItem.setOnAction(e -> UndoManager.run(setExpression(CopyPasteManager.pasteExpression())));

        contextMenu.getItems().add(pasteItem);

        contextMenu.setOnShowing(e -> {
            selector.hide();
            pasteItem.setDisable(!(CopyPasteManager.peek() instanceof Expression) || !TypeHandler.canConvert(((Expression) CopyPasteManager.peek()).getReturnType(), type));
        });
    }

    public UndoManager.RevertableAction setExpression(Expression.Block newExpression) {
        if (newExpression == null) {
            return clear();
        }
        UndoManager.RevertableAction clearAction = clear();
        return new UndoManager.RevertableAction() {
            @Override
            public void run() {
                clearAction.run();
                expression = newExpression;
                getChildren().setAll(newExpression);
            }
            @Override
            public void revert() {
                expression = null;
                getChildren().setAll(button);
                clearAction.revert();
            }
        };
    }

    public UndoManager.RevertableAction clear() {
        if (expression == null) {
            return UndoManager.EMPTY_ACTION;
        }
        Expression.Block oldExpression = expression;
        return new UndoManager.RevertableAction() {
            @Override
            public void run() {
                expression = null;
                getChildren().setAll(button);
            }
            @Override
            public void revert() {
                expression = oldExpression;
                getChildren().setAll(oldExpression);
            }
        };
    }

    @Override
    public void update() {
        if (expression != null) {
            expression.update();
        }
    }

    @Override
    public void prepareBuild(BuildContext buildContext) {
        if (expression != null) {
            expression.prepareBuild(buildContext);
        }
    }

    @Override
    public String toJava() {
        return expression != null ?
                TypeHandler.convert(expression.getDefinition().getReturnType(), type, expression.toJava()) :
                "((" + type.getCanonicalName() + ")" + (type.isPrimitive() ? "(Object) null" : "null") + ")";
    }

    @Override
    public Object serialize() {
        return expression != null ? expression.serialize() : null;
    }

    @Override
    public void deserialize(Object obj) {
        if (obj instanceof JSONObject) {
            JSONObject json = (JSONObject) obj;
            Expression expression = BlockRegistry.getExpression(json.optString("="));
            if (expression != null) {
                setExpression(expression.createBlock(json)).run();
            }
        }
    }

    public Class<?> getType() {
        return type;
    }

    public Expression.Block getExpression() {
        return expression;
    }
}