package bluej.editor.stride;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import bluej.utility.javafx.binding.ConcatListBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import bluej.Config;
import bluej.editor.stride.FXTabbedEditor.CodeCompletionState;
import bluej.stride.framedjava.ast.Loader;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.frames.CodeFrame;
import bluej.stride.framedjava.frames.GreenfootFrameCategory;
import bluej.stride.generic.CanvasParent;
import bluej.stride.generic.ExtensionDescription;
import bluej.stride.generic.Frame;
import bluej.stride.generic.FrameCursor;
import bluej.stride.generic.FrameDictionary;
import bluej.stride.generic.FrameDictionary.Entry;
import bluej.stride.generic.FrameFactory;
import bluej.utility.Utility;
import bluej.utility.javafx.FXBiConsumer;
import bluej.utility.javafx.FXRunnable;
import bluej.utility.javafx.JavaFXUtil;

/**
 * The pop-out catalogue displayed on the right-hand side with frames and shortcuts
 */
public class FrameCatalogue extends VBox
{
    /**
     * A callback to update an entry (or entries) in the frame catalogue
     */
    private static interface Updater
    {
        /**
         * Called to update the frame catalogue entries based on the current editing state.
         *
         * @param c The currently focused frame cursor (or null if focus is not on a frame cursor)
         * @param codeCompletion Whether code completion is possible, impossible, or currently showing
         * @param hasFrameSelection Whether there is a frame selection
         * @param inBirdseye Whether we are in bird's eye view
         */
        public void update(FrameCursor c, CodeCompletionState codeCompletion, boolean hasFrameSelection, Frame.View viewMode);
    }

    /**
     * A list of update callbacks.  Will be called in order.
     */
    private final List<Updater> catalogueUpdate = new ArrayList<>();
    /**
     * A list of extension key items for frames.  Updated when frame cursor focus changes.
     */
    private final ObservableList<Node> extensionItems = FXCollections.observableArrayList();
    /**
     * A list of hints (not commands, but example code) to show.
     */
    private final ObservableList<Node> hintItems = FXCollections.observableArrayList();
    /**
     * The current callback to cancel the next update of the frame catalogue.
     * See the scheduleUpdateCatalogue() method
     */
    private FXRunnable cancelUpdateCatalogue;
    /**
     * Keep track of which frame cursor is focused, if any.
     */
    private FrameCursor currentCursor;
    /**
     * Whether the catalogue has been initialised.
     * See the fillCatalogue method.
     */
    private boolean filled = false;

    private final static double CATALOGUE_FRAME_WIDTH = 200.0;


    // package-visible
    FrameCatalogue()
    {
        JavaFXUtil.addStyleClass(this, "catalogue");
        setFocusTraversable(false);
    }

    /**
     * Fills the catalogue.  We can't call this from the constructor because the FrameCatalogue
     * is created with the FXTabbedEditor, but we need to borrow a FrameEditorTab reference in order
     * to mock up the frames for the preview pictures.  So we only fill the catalogue the first time
     * that an update is requested by an editor.
     */
    //package-visible
    private void fillCatalogue(FrameEditorTab editor)
    {
        if (filled || editor == null)
            return;
        filled = true;

        ObservableList<Node> standardItems = FXCollections.observableArrayList();

        Node shortcutHeader = makeSectionHeader(Config.getString("frame.catalogue.shortcuts"));
        standardItems.add(shortcutHeader);

        /*
        Node enterFrame = makeTextItem("\u2192", "Edit frame beneath", false);
        JavaFXUtil.setPseudoclass("bj-catalogue-special", true, enterFrame);
        standardItems.add(enterFrame);
        */
        catalogueUpdate.add((c, code, hasSelection, birdseye) -> {
            //boolean show = c != null && c.getFrameAfter() != null && c.getFrameAfter().getEditableSlotsDirect().findFirst().isPresent();
            //enterFrame.setVisible(show);
            //enterFrame.setManaged(show);

            // We only show "Shortcuts" header when frame cursor is null (and thus we have things like
            // code completion shortcuts to show) or it's non-null and there's no selection -- shortcuts
            // for the selection are short under selectionheader, below
            shortcutHeader.setVisible(c == null /*|| show*/);
            shortcutHeader.setManaged(c == null /*|| show*/);
        });

        Node commandHeader = makeSectionHeader(Config.getString("frame.catalogue.commands"));
        standardItems.add(commandHeader);

        // We rely on the updates being called in order; don't show it by default if there's a selection, may show later on
        // if there is a shortcut available for a selection:
        catalogueUpdate.add((c, code, hasSelection, birdseye) -> {
            commandHeader.setVisible(c != null && !hasSelection);
            commandHeader.setManaged(c != null && !hasSelection);
        });

        // The frames we are creating for snapshot previews are not real.  But they will
        // try to notify the editor of their creation.  So we must wrap all this code in
        // an ignoreEdits block, to avoid the editor responding to all the frame creations
        // by scheduling recompilation:
        editor.ignoreEdits(() -> {

            FrameDictionary<GreenfootFrameCategory> dictionary = editor.getDictionary();
            BorderPane p = new BorderPane();
            p.setMinWidth(CATALOGUE_FRAME_WIDTH);
            p.setPrefWidth(CATALOGUE_FRAME_WIDTH);
            p.setMaxWidth(CATALOGUE_FRAME_WIDTH);
            // We need a scene with the right stylesheets, for doing the previews:
            Scene temp = new Scene(p);
            Config.addEditorStylesheets(temp);

            Comparator<Entry<GreenfootFrameCategory>> comparator = Comparator.<Entry<GreenfootFrameCategory>, GreenfootFrameCategory>comparing(Entry::getCategory).thenComparing(e -> getDisplayShortcut(e.getShortcuts()));

            final SnapshotParameters params = new SnapshotParameters();
            params.setFill(Color.TRANSPARENT);

            for (Entry<GreenfootFrameCategory> e : Utility.iterableStream(dictionary.getAllBlocks().stream().sorted(comparator)))
            {
                Frame f = e.getFactory().createBlock(editor);
                p.setCenter(f.getNode());
                WritableImage image = p.snapshot(params, null);

                AnchorPane item = new AnchorPane();
                JavaFXUtil.addStyleClass(item, "catalogue-item");
                JavaFXUtil.setPseudoclass("bj-catalogue-clickable", true, item);
                item.setMinWidth(CATALOGUE_FRAME_WIDTH);
                item.setMaxWidth(CATALOGUE_FRAME_WIDTH);
                item.setMinHeight(30.0);
                JavaFXUtil.initializeCustomTooltipCatalogue(editor.getParent(), item, "Click, or press \'" + keyTooltipName(e.getShortcuts()) + "\' to insert " + e.getName().toLowerCase() + " frame", Duration.millis(1500));
                ImageView imageView = new ImageView(image);
                //imageView.setFitWidth(Math.min(CATALOGUE_FRAME_WIDTH, image.getWidth()));
                //imageView.setFitHeight(Math.min(30.0, image.getHeight()));
                imageView.setPreserveRatio(true);

                // Try to find a header row to centre the screenshot on it:
                Node header = f.getHeaderItems().findFirst().flatMap(h -> h.getComponents().stream().findFirst()).orElse(null);
                if (header != null)
                {
                    double headerY = header.localToScene(header.getBoundsInLocal()).getMinY();
                    imageView.setViewport(new Rectangle2D(0, headerY, 75, 18));
                } else
                {
                    // Just take the top-left:
                    imageView.setViewport(new Rectangle2D(0, 0, 75, 18));
                }
                imageView.setEffect(new ColorAdjust(0, 0.0, 0.2, 0));
                AnchorPane.setBottomAnchor(imageView, 0.0);
                AnchorPane.setRightAnchor(imageView, 0.0);
                item.getChildren().add(imageView);
                HBox keyAndName = getKeyAndName(getDisplayShortcut(e.getShortcuts()), e.getName(), true);
                item.getChildren().addAll(keyAndName);
                setupClick(item, e.getFactory());
                
                f.cleanup();

                // Start invisible:
                item.setVisible(false);
                item.setManaged(false);
                standardItems.add(item);

                catalogueUpdate.add((c, code, hasSelection, birdseye) -> {
                    boolean show = c != null && c.canInsert() && c.check().canInsert(e.getCategory()) && (!hasSelection || e.validOnSelection());
                    item.setVisible(show);
                    item.setManaged(show);

                    if (hasSelection && show)
                    {
                        commandHeader.setVisible(true);
                        commandHeader.setManaged(true);
                    }
                });

            }

            // Add code completion hint:
            Label ctrl = new Label("Ctrl");
            JavaFXUtil.addStyleClass(ctrl, "catalogue-key");
            Label space = new Label("Space");
            JavaFXUtil.addStyleClass(space, "catalogue-key");
            JavaFXUtil.setPseudoclass("bj-wide", true, ctrl, space);
            Label plus = new Label("+");
            ctrl.setTextOverrun(OverrunStyle.CLIP);
            space.setTextOverrun(OverrunStyle.CLIP);
            plus.setTextOverrun(OverrunStyle.CLIP);
            Label name = new Label(Config.getString("frame.catalogue.codecompletion"));
            name.setWrapText(true);
            name.styleProperty().set("-fx-label-padding: 0 0 0 3");
            HBox hbox = new HBox(ctrl, plus, space, name);
            hbox.setSpacing(1.0);
            hbox.setFillHeight(false);
            hbox.setAlignment(Pos.CENTER_LEFT);
            standardItems.add(hbox);
            catalogueUpdate.add((c, codeCompletion, selection, birdseye) -> {
                hbox.setVisible(codeCompletion == CodeCompletionState.POSSIBLE);
                hbox.setManaged(codeCompletion == CodeCompletionState.POSSIBLE);
            });
            hbox.setVisible(false);
            hbox.setManaged(false);

            FXBiConsumer<String, String> addCodeCompletionShortcut = (shortcut, actionName) -> {
                Node content = makeTextItem(shortcut, actionName, false);
                standardItems.add(content);
                catalogueUpdate.add((c, codeCompletion, selection, birdseye) -> {
                    content.setVisible(codeCompletion == CodeCompletionState.SHOWING);
                    content.setManaged(codeCompletion == CodeCompletionState.SHOWING);
                });
            };
            addCodeCompletionShortcut.accept("Esc", Config.getString("frame.catalogue.codecompletion.hide"));
            addCodeCompletionShortcut.accept("\u2191", Config.getString("frame.catalogue.codecompletion.up"));
            addCodeCompletionShortcut.accept("\u2193", Config.getString("frame.catalogue.codecompletion.down"));
            addCodeCompletionShortcut.accept("\u21B5", Config.getString("frame.catalogue.codecompletion.select"));

            FXBiConsumer<String, String> addBirdseyeShortcut = (shortcut, actionName) -> {
                Node content = makeTextItem(shortcut, actionName, false);
                standardItems.add(content);
                catalogueUpdate.add((c, codeCompletion, selection, view) -> {
                    content.setVisible(view == Frame.View.BIRDSEYE);
                    content.setManaged(view == Frame.View.BIRDSEYE);
                });
            };
            addBirdseyeShortcut.accept("Esc", Config.getString("frame.catalogue.birdseye.exit"));
            addBirdseyeShortcut.accept("\u2191", Config.getString("frame.catalogue.birdseye.up"));
            addBirdseyeShortcut.accept("\u2193", Config.getString("frame.catalogue.birdseye.down"));
            addBirdseyeShortcut.accept("\u21B5", Config.getString("frame.catalogue.birdseye.select"));

            Node content = makeTextItem("Esc", Config.getString("frame.catalogue.birdseye.exit"), false);
            standardItems.add(content);
            catalogueUpdate.add((c, codeCompletion, selection, view) -> {
                content.setVisible(view == Frame.View.JAVA_PREVIEW);
                content.setManaged(view == Frame.View.JAVA_PREVIEW);
            });
        });

        ConcatListBinding.bind(getChildren(), FXCollections.observableArrayList(standardItems, extensionItems, hintItems));
    }

    private HBox getKeyAndName(String shortcut, String title, boolean showingPreview)
    {
        Label keyLabel = new Label(shortcut);
        keyLabel.setMouseTransparent(true);
        JavaFXUtil.addStyleClass(keyLabel, "catalogue-key");
        Label name = new Label(title);
        name.setWrapText(true);
        name.setMaxWidth(showingPreview ? 100.0 : 160.0);
        name.setMouseTransparent(true);
        HBox keyAndName = new HBox(keyLabel, name);
        keyAndName.setSpacing(5.0);
        AnchorPane.setLeftAnchor(keyAndName, 0.0);
        AnchorPane.setTopAnchor(keyAndName, 0.0);
        AnchorPane.setBottomAnchor(keyAndName, 0.0);
        keyAndName.setFillHeight(false);
        keyAndName.setAlignment(Pos.CENTER_LEFT);
        return keyAndName;
    }

    private void setupClick(Node item, FrameFactory<? extends Frame> factory)
    {
        item.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
            if (currentCursor != null)
            {
                FrameSelection selection = currentCursor.getEditor().getSelection();
                if (selection.getSelected().isEmpty())
                {
                    Frame f = factory.createBlock(currentCursor.getEditor());
                    currentCursor.insertBlockAfter(f);
                    f.focusWhenJustAdded();
                }
                else
                {
                    List<Frame> selected = selection.getSelected();
                    // We must add the new frame before removing the old ones because removing the old
                    // ones may remove us as a cursor!
                    List<Frame> selectedCopy = Utility.mapList(selected, f -> Loader.loadElement(((CodeFrame<CodeElement>)f).getCode().toXML()).createFrame(currentCursor.getEditor()));
                    Frame newFrame = factory.createBlock(currentCursor.getEditor(), selectedCopy);
                    currentCursor.insertBlockBefore(newFrame);
                    selected.forEach(f -> f.getParentCanvas().removeBlock(f));
                    selection.clear();
                    newFrame.focusWhenJustAdded();
                }
            }
            e.consume();
        });
    }

    private void setupClick(Node item, FrameCursor c, FXRunnable action)
    {
        item.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
            if (currentCursor == c)
            {
                action.run();
            }
            e.consume();
        });
    }

    private String getDisplayShortcut(String shortcut)
    {
        if (shortcut.equals("\n"))
            return "\u21B5";
        else if (shortcut.equals("\b"))
            return "\u21A4";
        else if (shortcut.equals(" "))
            return "\u02FD";
        else
            return shortcut;
    }

    private String keyTooltipName(String shortcut)
    {
        switch (shortcut)
        {
            case " ": return "space";
            case "\n": return "return";
            case "\b": return "backspace";
            default: return shortcut;
        }
    }

    public static class Hint
    {
        public final String exampleCode;
        public final String explanation;

        public Hint(String exampleCode, String explanation)
        {
            this.exampleCode = exampleCode;
            this.explanation = explanation;
        }
    }

    // Pass null if there is no currently focused cursor
    // package-visible
    void scheduleUpdateCatalogue(FrameEditorTab editor, FrameCursor c, CodeCompletionState codeCompletion, boolean selection, Frame.View viewMode, List<Hint> hints)
    {
        currentCursor = c;
        // Schedule an update to happen in half a second:
        if (cancelUpdateCatalogue != null)
        {
            cancelUpdateCatalogue.run();
        }
        
        cancelUpdateCatalogue = JavaFXUtil.runAfter(Duration.millis(500), () -> {
            // If the whole window has lost focus, for reasons other than code-competlion, just leave catalogue as-is until focus returns:
            if (getScene().getWindow().isFocused() || codeCompletion == CodeCompletionState.SHOWING)
            {
                fillCatalogue(editor);
                catalogueUpdate.forEach(updater -> updater.update(c, codeCompletion, selection, viewMode));
                updateExtensions(selection ? null : c);
                replaceHints(hints);
            }
        });
    }

    private void updateExtensions(FrameCursor c)
    {
        extensionItems.clear();

        if (c != null)
        {
            final Frame frameBefore = c.getFrameBefore();
            final Frame frameAfter = c.getFrameAfter();

            Set<Character> keysAlreadyUsed = new HashSet<>();

            CanvasParent parent = c.getParentCanvas().getParent();
            if (parent != null && c.canInsert())
            {
                for (ExtensionDescription ext : parent.getAvailableInnerExtensions(c.getParentCanvas(), c))
                {
                    if (!keysAlreadyUsed.contains(ext.getShortcutKey()) && (frameBefore == null || ext.worksThroughout()) && ext.showInCatalogue())
                    {
                        Node item = makeTextItem(getDisplayShortcut("" + ext.getShortcutKey()), ext.getDescription(), true);
                        setupClick(item, c, ext::activate);
                        extensionItems.add(item);
                        keysAlreadyUsed.add(ext.getShortcutKey());
                    }
                }
            }



            if (frameAfter != null && frameAfter.isFrameEnabled())
            {
                for (ExtensionDescription ext : frameAfter.getAvailablePrefixes())
                {
                    if (!keysAlreadyUsed.contains(ext.getShortcutKey()) && ext.showInCatalogue())
                    {
                        Node item = makeTextItem(getDisplayShortcut("" + ext.getShortcutKey()), ext.getDescription(), true);
                        setupClick(item, c, ext::activate);
                        extensionItems.add(item);
                        keysAlreadyUsed.add(ext.getShortcutKey());
                    }
                }
            }

            if (frameBefore != null && frameBefore.isFrameEnabled())
            {
                for (ExtensionDescription ext : frameBefore.getAvailableExtensions())
                {
                    if (!keysAlreadyUsed.contains(ext.getShortcutKey()) && ext.showInCatalogue())
                    {
                        Node item = makeTextItem(getDisplayShortcut("" + ext.getShortcutKey()), ext.getDescription(), true);
                        setupClick(item, c, ext::activate);
                        extensionItems.add(item);
                        keysAlreadyUsed.add(ext.getShortcutKey());
                    }
                }
            }
        }
    }

    private Node makeTextItem(String shortcut, String description, boolean clickable)
    {
        AnchorPane item = new AnchorPane();
        JavaFXUtil.addStyleClass(item, "catalogue-item");
        JavaFXUtil.setPseudoclass("bj-catalogue-clickable", clickable, item);
        item.setMaxWidth(CATALOGUE_FRAME_WIDTH);
        item.setMinHeight(30.0);
        item.getChildren().add(getKeyAndName(shortcut, description, false));
        return item;
    }

    private Node makeHint(Hint h)
    {
        VBox item = new VBox();
        Label example = new Label(h.exampleCode);
        Label explanation = new Label(h.explanation);
        example.setWrapText(true);
        explanation.setWrapText(true);
        JavaFXUtil.addStyleClass(example, "catalogue-example-code");
        JavaFXUtil.addStyleClass(explanation, "catalogue-example-description");
        VBox.setMargin(explanation, new Insets(3, 0, 0, 20));
        item.getChildren().addAll(example, explanation);
        item.setMaxWidth(CATALOGUE_FRAME_WIDTH);
        item.setMinHeight(30.0);
        JavaFXUtil.addStyleClass(example, "catalogue-example");
        return item;
    }

    private final Node exampleHeader = makeSectionHeader(Config.getString("frame.catalogue.examples"));

    private void replaceHints(List<Hint> hints)
    {
        hintItems.setAll(Utility.mapList(hints, h -> makeHint(h)));
        if (!hintItems.isEmpty())
            hintItems.add(0, exampleHeader);
    }

    private Node makeSectionHeader(String title)
    {
        Label l = new Label(title);
        JavaFXUtil.addStyleClass(l, "catalogue-header");
        VBox pane = new VBox(l);
        JavaFXUtil.addStyleClass(pane, "catalogue-item");
        JavaFXUtil.setPseudoclass("bj-catalogue-header", true, pane);
        pane.setMinWidth(CATALOGUE_FRAME_WIDTH);
        return pane;

    }
}
