package com.gmail.visualbukkit.blocks;

import com.gmail.visualbukkit.VisualBukkitApp;
import com.gmail.visualbukkit.blocks.definitions.CompEventListener;
import com.gmail.visualbukkit.blocks.parameters.BlockParameter;
import com.gmail.visualbukkit.gui.NotificationManager;
import com.gmail.visualbukkit.gui.StyleableHBox;
import com.gmail.visualbukkit.plugin.BuildContext;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public abstract class CodeBlock<T extends BlockDefinition<?>> extends VBox {

    private static final PseudoClass INVALID_STYLE_CLASS = PseudoClass.getPseudoClass("invalid");
    private static final PseudoClass SELECTED_STYLE_CLASS = PseudoClass.getPseudoClass("selected");
    public static CodeBlock<?> currentSelected;

    private final T definition;
    private List<BlockParameter> parameters = new ArrayList<>();
    private VBox syntaxBox = new VBox();
    private ContextMenu contextMenu = new ContextMenu();
    private String invalidReason;

    public CodeBlock(T definition) {
        this.definition = definition;

        syntaxBox.getStyleClass().add("code-block");
        getChildren().add(syntaxBox);

        parentProperty().addListener((o, oldValue, newValue) -> {
            if (newValue != null) {
                update();
            }
        });

        getSyntaxBox().setOnContextMenuRequested(e -> {
            contextMenu.show(VisualBukkitApp.getInstance().getScene().getWindow(), e.getScreenX(), e.getScreenY());
            VisualBukkitApp.getInstance().getScene().getWindow().requestFocus();
            e.consume();
        });

        getSyntaxBox().setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                if (currentSelected != this) {
                    select();
                } else {
                    unselect();
                }
            }
            e.consume();
        });

        VisualBukkitApp.getInstance().getScene().addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (currentSelected != null && currentSelected.getParent() != null) {
                currentSelected.handleSelectedAction(e);
            }
        });
    }

    protected void addHeaderNode(Node node) {
        node.getStyleClass().add("code-block-header");
        syntaxBox.getChildren().add(0, node);
        if (node instanceof BlockParameter) {
            parameters.add((BlockParameter) node);
        }
        if (node instanceof Parent) {
            for (Node child : ((Parent) node).getChildrenUnmodifiable()) {
                if (child instanceof BlockParameter) {
                    parameters.add((BlockParameter) child);
                }
            }
        }
    }

    public void addParameterLines(BlockParameter... parameters) {
        for (int i = 0; i < parameters.length; i++) {
            addParameterLine(definition.getParameterNames()[i], parameters[i]);
        }
    }

    public HBox addParameterLine(String name, BlockParameter parameter) {
        HBox hBox = new StyleableHBox(new Label(name), (Node) parameter);
        syntaxBox.getChildren().add(hBox);
        parameters.add(parameter);
        return hBox;
    }

    public void select() {
        if (currentSelected != null) {
            currentSelected.unselect();
        }
        currentSelected = this;
        syntaxBox.pseudoClassStateChanged(SELECTED_STYLE_CLASS, true);
        if (invalidReason != null) {
            NotificationManager.displayError(VisualBukkitApp.getString("error.invalid_block"), invalidReason);
        }
    }

    public void unselect() {
        currentSelected = null;
        syntaxBox.pseudoClassStateChanged(SELECTED_STYLE_CLASS, false);
    }

    protected void handleSelectedAction(KeyEvent e) {}

    protected void setValid() {
        syntaxBox.pseudoClassStateChanged(INVALID_STYLE_CLASS, false);
        invalidReason = null;
    }

    protected void setInvalid(String reason) {
        syntaxBox.pseudoClassStateChanged(INVALID_STYLE_CLASS, true);
        invalidReason = reason;
    }

    protected void checkForPluginComponent(String componentID) {
        PluginComponent.Block block = getPluginComponentBlock();
        if (block == null || !block.getDefinition().getID().equals(componentID)) {
            setInvalid(String.format(VisualBukkitApp.getString("error.invalid_placement"), BlockRegistry.getPluginComponent(componentID).getTitle()));
        }
    }

    protected void checkForEvent(Class<?> clazz) {
        PluginComponent.Block block = getPluginComponentBlock();
        if (!(block instanceof CompEventListener.EventBlock) || !clazz.isAssignableFrom(((CompEventListener.EventBlock) block).getEvent())) {
            setInvalid(String.format(VisualBukkitApp.getString("error.invalid_placement"), clazz.getSimpleName()));
        }
    }

    protected void checkForContainer(String containerID) {
        Parent parent = getParent();
        while (parent != null) {
            if (parent instanceof Container.ChildConnector) {
                if ((((Container.ChildConnector) parent).getOwner().getDefinition().getID().equals(containerID))) {
                    return;
                }
            }
            parent = parent.getParent();
        }
        setInvalid(String.format(VisualBukkitApp.getString("error.invalid_placement"), BlockRegistry.getStatement(containerID)));
    }

    public void update() {
        setValid();
        for (BlockParameter parameter : parameters) {
            parameter.update();
        }
    }

    public void prepareBuild(BuildContext buildContext) {
        for (BlockParameter parameter : parameters) {
            parameter.prepareBuild(buildContext);
        }
    }

    public JSONObject serialize() {
        JSONObject json = new JSONObject();
        json.put("=", definition.getID());
        for (BlockParameter parameter : parameters) {
            json.append("parameters", parameter.serialize());
        }
        return json;
    }

    public void deserialize(JSONObject json) {
        JSONArray parameterArray = json.optJSONArray("parameters");
        if (parameterArray != null) {
            int len = Math.min(parameters.size(), parameterArray.length());
            for (int i = 0; i < len; i++) {
                Object obj = parameterArray.opt(i);
                if (obj != null) {
                    parameters.get(i).deserialize(obj);
                }
            }
        }
    }

    public String arg(int i) {
        return parameters.get(i).toJava();
    }

    public PluginComponent.Block getPluginComponentBlock() {
        Parent node = this;
        while (node != null) {
            if (node instanceof PluginComponent.Block) {
                return (PluginComponent.Block) node;
            }
            node = node.getParent();
        }
        return null;
    }

    public final T getDefinition() {
        return definition;
    }

    public List<BlockParameter> getParameters() {
        return parameters;
    }

    public VBox getSyntaxBox() {
        return syntaxBox;
    }

    public ContextMenu getContextMenu() {
        return contextMenu;
    }
}