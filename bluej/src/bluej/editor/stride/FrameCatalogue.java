/*
 This file is part of the BlueJ program. 
 Copyright (C) 2015,2016,2017 Michael Kölling and John Rosenberg
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.editor.stride;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import bluej.collect.StrideEditReason;
import bluej.stride.framedjava.frames.StrideCategory;
import bluej.stride.generic.ExtensionDescription.ExtensionSource;
import bluej.stride.generic.InteractionManager;
import bluej.utility.javafx.FXPlatformRunnable;
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
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import bluej.Config;
import bluej.editor.stride.FXTabbedEditor.CodeCompletionState;
import bluej.stride.framedjava.ast.Loader;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.frames.CodeFrame;
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
import threadchecker.OnThread;
import threadchecker.Tag;

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
         * @param viewMode The current view mode (Normal, Java, Birdseye w/o documentation)
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
    private FXPlatformRunnable cancelUpdateCatalogue;
    /**
     * Keep track of which frame cursor is focused, if any.
     */
    private FrameCursor currentCursor;
    /**
     * Whether the catalogue has been initialised.
     * See the fillCatalogue method.
     */
    private boolean filled = false;

    public final static double CATALOGUE_FRAME_WIDTH = 220.0;

    /**
     * An enum to list all the cases in which the frame catalogue will be expanded.
     * This is used for the DataCollection.
     */
    public enum ShowReason {
        // The user presses unknown command twice.
        UNKNOWN_FRAME_COMMAND("unknown_frame_command"),
        // The check menu, its accelerator or the keyboard shortcut is used to show/hide the catalogue.
        // They are merged as we don't have a straightforward way to actually differentiate between
        // the accelerator and actual clicking on the menu. Also, there is no big need to differentiate.
        MENU_OR_SHORTCUT("menu_or_shortcut"),
        // The user clicks on the catalogue's arrow to show/hide it.
        ARROW("arrow"),
        // When loading the project, bluej tries to retrieve the previous catalogue state.
        PROPERTIES("properties");

        private final String text;

        ShowReason(String text)
        {
            this.text = text;
        }

        public String getText()
        {
            return text;
        }
    }

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
    @OnThread(Tag.FXPlatform)
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

            FrameDictionary<StrideCategory> dictionary = editor.getDictionary();
            BorderPane p = new BorderPane();
            p.setMinWidth(CATALOGUE_FRAME_WIDTH);
            p.setPrefWidth(CATALOGUE_FRAME_WIDTH);
            p.setMaxWidth(CATALOGUE_FRAME_WIDTH);
            // We need a scene with the right stylesheets, for doing the previews:
            Scene temp = new Scene(p);
            Config.addEditorStylesheets(temp);

            Comparator<Entry<StrideCategory>> comparator = Comparator.<Entry<StrideCategory>, StrideCategory>comparing(Entry::getCategory).thenComparing(e -> getDisplayShortcut(e.getShortcuts()));

            final SnapshotParameters params = new SnapshotParameters();
            params.setFill(Color.TRANSPARENT);

            for (Entry<StrideCategory> e : Utility.iterableStream(dictionary.getAllBlocks().stream().filter(b -> b.isShowingInCatalogue()).sorted(comparator)))
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
                Pane keyAndName = getKeyAndName(Collections.singletonList(getDisplayShortcut(e.getShortcuts())), e.getName(), true);
                item.getChildren().addAll(keyAndName);
                setupClick(item, e.getFactory());
                
                f.cleanup();

                // Start invisible:
                item.setVisible(false);
                item.setManaged(false);
                standardItems.add(item);

                catalogueUpdate.add((c, code, hasSelection, birdseye) -> {
                    boolean show = c != null && c.canInsert() && c.check().canInsert(e.getCategory()) && (!hasSelection || e.isValidOnSelection());
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
            Node hbox = makeTextItem(Arrays.asList("Ctrl", "Space"), Config.getString("frame.catalogue.codecompletion"), false);
            JavaFXUtil.setPseudoclass("bj-catalogue-special", true, hbox);
            standardItems.add(hbox);
            catalogueUpdate.add((c, codeCompletion, selection, birdseye) -> {
                hbox.setVisible(codeCompletion == CodeCompletionState.POSSIBLE);
                hbox.setManaged(codeCompletion == CodeCompletionState.POSSIBLE);
            });
            hbox.setVisible(false);
            hbox.setManaged(false);

            FXBiConsumer<String, String> addCodeCompletionShortcut = (shortcut, actionName) -> {
                Node content = makeTextItem(Collections.singletonList(shortcut), actionName, false);
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
                Node content = makeTextItem(Collections.singletonList(shortcut), actionName, false);
                standardItems.add(content);
                catalogueUpdate.add((c, codeCompletion, selection, view) -> {
                    content.setVisible(view.isBirdseye());
                    content.setManaged(view.isBirdseye());
                });
            };
            addBirdseyeShortcut.accept("Esc", Config.getString("frame.catalogue.birdseye.exit"));
            addBirdseyeShortcut.accept("\u2191", Config.getString("frame.catalogue.birdseye.up"));
            addBirdseyeShortcut.accept("\u2193", Config.getString("frame.catalogue.birdseye.down"));
            addBirdseyeShortcut.accept("\u21B5", Config.getString("frame.catalogue.birdseye.select"));

            Node content = makeTextItem(Collections.singletonList("Esc"), Config.getString("frame.catalogue.birdseye.exit"), false);
            standardItems.add(content);
            catalogueUpdate.add((c, codeCompletion, selection, view) -> {
                content.setVisible(view == Frame.View.JAVA_PREVIEW);
                content.setManaged(view == Frame.View.JAVA_PREVIEW);
            });
        });

        ConcatListBinding.bind(getChildren(), FXCollections.observableArrayList(standardItems, extensionItems, hintItems));
    }

    private Pane getKeyAndName(List<String> shortcutKeys, String title, boolean showingPreview)
    {
        HBox keysHBox = new HBox(shortcutKeys.stream().map(shortcut -> {
            Label keyLabel = new Label(shortcut);
            keyLabel.setMouseTransparent(true);
            JavaFXUtil.addStyleClass(keyLabel, "catalogue-key");
            if (shortcut.length() > 1)
            {
                JavaFXUtil.setPseudoclass("bj-wide", true, keyLabel);
                keyLabel.setTextOverrun(OverrunStyle.CLIP);
            }
            return (Node)keyLabel;
        }).collect(Utility.intersperse(() -> (Node)new Label("+"))).toArray(new Node[0]));
        keysHBox.setSpacing(1.0);
        keysHBox.setFillHeight(false);
        keysHBox.setAlignment(Pos.CENTER_LEFT);
        Label name = new Label(title);
        name.setWrapText(true);
        name.setMaxWidth(showingPreview ? 100.0 : 160.0);
        name.setMouseTransparent(true);
        Pane keyAndName;
        if (shortcutKeys.size() == 1)
        {
            keyAndName = new HBox(keysHBox, name) {
                {
                    setSpacing(5.0);
                    setFillHeight(false);
                    setAlignment(Pos.CENTER_LEFT);
                }
            };
        }
        else
        {
            keyAndName = new VBox(keysHBox, name) {
                {
                    setSpacing(5.0);
                }
            };
            name.setStyle("-fx-padding: 0 0 0 15;");
        }
        AnchorPane.setLeftAnchor(keyAndName, 0.0);
        AnchorPane.setTopAnchor(keyAndName, 0.0);
        AnchorPane.setBottomAnchor(keyAndName, 0.0);
        return keyAndName;
    }

    private void setupClick(Node item, FrameFactory<? extends Frame> factory)
    {
        item.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
            if (currentCursor != null)
            {
                InteractionManager editor = currentCursor.getEditor();
                editor.recordEdits(StrideEditReason.FLUSH);
                FrameSelection selection = currentCursor.getEditor().getSelection();
                if (selection.getSelected().isEmpty())
                {
                    Frame f = factory.createBlock(currentCursor.getEditor());
                    currentCursor.insertBlockAfter(f);
                    f.markFresh();
                    f.focusWhenJustAdded();
                    editor.recordEdits(StrideEditReason.SINGLE_FRAME_INSERTION_CHEAT);
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
                    newFrame.markFresh();
                    newFrame.focusWhenJustAdded();
                    editor.recordEdits(StrideEditReason.SELECTION_WRAP_CHEAT);
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
    @OnThread(Tag.FXPlatform)
    void scheduleUpdateCatalogue(FrameEditorTab editor, FrameCursor c, CodeCompletionState codeCompletion, boolean selection, Frame.View viewMode, List<ExtensionDescription> altExtensions, List<Hint> hints)
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
                updateExtensions(selection ? null : c, altExtensions);
                replaceHints(hints);
            }
        });
    }

    private void updateExtensions(FrameCursor c, List<ExtensionDescription> altExtensions)
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
                for (ExtensionDescription ext : parent.getAvailableExtensions(c.getParentCanvas(), c))
                {
                    if (!keysAlreadyUsed.contains(ext.getShortcutKey()) && ext.validFor(frameBefore == null ? ExtensionSource.INSIDE_FIRST : ExtensionSource.INSIDE_LATER) && ext.showInCatalogue())
                    {
                        Node item = makeTextItem(Collections.singletonList(getDisplayShortcut("" + ext.getShortcutKey())), ext.getDescription(), true);
                        setupClick(item, c, ext::activate);
                        extensionItems.add(item);
                        keysAlreadyUsed.add(ext.getShortcutKey());
                    }
                }
            }

            if (frameAfter != null && frameAfter.isFrameEnabled())
            {
                for (ExtensionDescription ext : frameAfter.getAvailableExtensions(null, null))
                {
                    if (!keysAlreadyUsed.contains(ext.getShortcutKey()) && ext.validFor(ExtensionSource.BEFORE) && ext.showInCatalogue())
                    {
                        Node item = makeTextItem(Collections.singletonList(getDisplayShortcut("" + ext.getShortcutKey())), ext.getDescription(), true);
                        setupClick(item, c, ext::activate);
                        extensionItems.add(item);
                        keysAlreadyUsed.add(ext.getShortcutKey());
                    }
                }
            }

            if (frameBefore != null && frameBefore.isFrameEnabled())
            {
                for (ExtensionDescription ext : frameBefore.getAvailableExtensions(null, null))
                {
                    if (!keysAlreadyUsed.contains(ext.getShortcutKey()) && ext.validFor(ExtensionSource.AFTER) && ext.showInCatalogue())
                    {
                        Node item = makeTextItem(Collections.singletonList(getDisplayShortcut("" + ext.getShortcutKey())), ext.getDescription(), true);
                        setupClick(item, c, ext::activate);
                        extensionItems.add(item);
                        keysAlreadyUsed.add(ext.getShortcutKey());
                    }
                }
            }
        }

        for (ExtensionDescription ext : altExtensions)
        {
            if (ext.validFor(ExtensionSource.MODIFIER) && ext.showInCatalogue())
            {
                Node item = makeTextItem(Arrays.asList("Ctrl", "Shift", getDisplayShortcut("" + ext.getShortcutKey())), ext.getDescription(), true);
                setupClick(item, c, ext::activate);
                extensionItems.add(item);
            }
        }
    }

    private Node makeTextItem(List<String> shortcut, String description, boolean clickable)
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
