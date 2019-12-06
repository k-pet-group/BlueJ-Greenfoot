/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016,2017,2018,2019 Michael Kölling and John Rosenberg
 
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
package bluej.utility.javafx;

import bluej.Config;
import bluej.editor.stride.CodeOverlayPane.WidthLimit;
import bluej.editor.stride.FXTabbedEditor;
import bluej.editor.stride.WindowOverlayPane;
import bluej.stride.generic.InteractionManager;
import bluej.utility.Debug;
import bluej.utility.Utility;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.*;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.*;
import javafx.css.*;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.*;
import javafx.scene.input.KeyCombination.Modifier;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import threadchecker.OnThread;
import threadchecker.Tag;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseListener;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * JavaFXUtil is a collection of static utility methods for JavaFX-related code
 * (effectively, the JavaFX sibling of Utility).
 */
public class JavaFXUtil
{
    /**
     * Sets the given CSS pseudo-class (bit after the colon in CSS selector) to given
     * state for given nodes
     * @param pseudoClassName The name of the CSS pseudo-class, e.g. "bj-pinned".  Must begin with "bj-" to avoid confusion with built in FX classes
     * @param enabled Should the class state be set to on (true) or off (false)
     * @param nodes A list of nodes so add/remove the pseudoclass from (based on the "enabled" parameter)
     */
    public static void setPseudoclass(String pseudoClassName, boolean enabled, Node... nodes)
    {
        if (!pseudoClassName.startsWith("bj-") && !pseudoClassName.startsWith("gf-"))
            throw new IllegalArgumentException("Our pseudoclasses should begin with bj- or gf- to avoid confusion with JavaFX's pseudo classes");
        
        for (Node node : nodes)
            node.pseudoClassStateChanged(PseudoClass.getPseudoClass(pseudoClassName), enabled);        
    }

    /**
     * Checks if the given pseudo-class is currently enabled on the given node
     * @param node The JavaFX node
     * @param pseudoClassName The name of the pseudo-class.
     * @return Whether the class is currently enabled on that node or not.
     */
    public static boolean hasPseudoclass(Styleable node, String pseudoClassName)
    {
        return node.getPseudoClassStates().stream().filter(p -> p.getPseudoClassName().equals(pseudoClassName)).count() > 0;
    }
    
    /**
     * Turns on the pseudoclass with the given index, and turns off all others in the list.
     * 
     * @param node Node to apply to
     * @param index Which index (zero-based) of pseudoClasses to turn on (-1 if none)
     * @param pseudoClasses The list of pseudo-classes to turn off (except for index, which is turned on)
     */
    public static void selectPseudoClass(Node node, int index, String... pseudoClasses)
    {
        for (int i = 0; i < pseudoClasses.length; i++)
        {
            JavaFXUtil.setPseudoclass(pseudoClasses[i], i == index, node);
        }
    }

    /**
     * Adds the given CSS style-class[es] to the node.
     *
     * @param node The node to apply to
     * @param styleClasses The CSS style-classes to be added.
     */
    public static void addStyleClass(Styleable node, String... styleClasses)
    {
        addStyleClass(node, FXCollections.observableArrayList(styleClasses));
    }

    /**
     * Adds the given CSS style-class[es] to the node.
     *
     * JavaFX doesn't usually care if you add the same class many times to a node.  In contrast, this method
     * makes sure that a class is only applied once (effectively models the classes on a node as a
     * set, not a list).  Generally, you should try to avoid dynamically turning classes on
     * or off, anyway, and use pseudo-classes instead.
     *
     * @param node The node to apply to
     * @param styleClasses The list of CSS style-classes to add.
     */
    public static void addStyleClass(Styleable node, ObservableList<String> styleClasses)
    {
        for (String styleClass : styleClasses)
        {
            if (!node.getStyleClass().contains(styleClass))
                node.getStyleClass().add(styleClass);
        }
    }

    /**
     * Like addStyleClass but also returns the node.  Useful as part of a builder
     * pattern, when you want to add a style-class for a new node, but you don't
     * otherwise need to store it in a variable before passing it to a method, e.g
     * pane.setTop(JavaFXUtil.withStyleClass(new Label("Information:"), "label-info"));
     */
    public static <T extends Styleable> T withStyleClass(T node, String... styleClasses)
    {
        addStyleClass(node, styleClasses);
        return node;
    }

    /**
     * Removes the given CSS style-class to the node.
     * 
     * JavaFX doesn't care if you add the same class many times to a node, and if you're not
     * careful when removing a style-class, you may only remove one instance and not all.  This method
     * makes sure that all copies of a class are removed (effectively models the classes on a node as a 
     * set, not a list). 
     * 
     * Generally, you should try to avoid dynamically turning classes on or off, anyway,
     * and use pseudo-classes instead. Therefore, eventually we should remove this method.
     * It has been kept for now as one JavaFX control (Tab) doesn't accept pseudo-classes.
     * 
     * @param node The node to apply to.
     * @param styleClasses The style-classes to remove.
     */
    public static void removeStyleClass(Styleable node, String... styleClasses)
    {
        //removeAll gets rid of all instances of all of styleClasses:
        node.getStyleClass().removeAll(styleClasses);
    }

    /**
     * Since JavaFX has no font metrics, we make our own.  The only way to
     * measure a JavaFX string is to render an off-screen text.  To avoid the repeated
     * expense, we keep a cache of font-size & text-content to width.
     * We do this for up to 3 font sizes, and up to 10k strings per font size
     * (This may seem a lot, but we usually measure substrings too, so we end up with
     * (num strings * avg string length) strings in cache.  See FXCache for more
     * details on cache behaviour.
     */
    private static final FXCache<Font, FXCache<String, Double>> measured =
        new FXCache<>(f -> new FXCache<>(s -> {
            if (s == null || s.length() == 0)
                return 0.0;

            //Not a very elegant way to get the size of the text, but only way to really do it
            //See e.g. http://stackoverflow.com/questions/13015698/
            Text text = new Text(s);
            text.setFont(f);
            return text.getLayoutBounds().getWidth();
        }, 10000), 3);

    /**
     * Measure the given string content using the given node's font.
     * 
     * @return Width of the string, when rendered in that font, in [logical] pixels.
     *         Does not have to be integer.
     */
    public static double measureString(TextInputControl node, String str)
    {
        return measureString(node, str, true, true);
    }
    /**
     * Measure the given string content using the given node's font.
     *
     * @param includeLeftInset Whether to include node.getInsets().getLeft() in the width
     * @param includeRightInset Whether to include node.getInsets().getRight() in the width
     * @return Width of the string, when rendered in that font, in [logical] pixels.
     *         Does not have to be integer.
     */
    public static double measureString(TextInputControl node, String str, boolean includeLeftInset, boolean includeRightInset)
    {
        return measureString(node, str, node.getFont(), includeLeftInset, includeRightInset);
    }

    /**
     * Measure the given string content using the given font.
     *
     * @param node The node from which to take the insets, but NOT the font
     * @param str The string to measure the size of
     * @param overrideFont The font to use for measuring
     * @param includeLeftInset Whether to include node.getInsets().getLeft() in the width
     * @param includeRightInset Whether to include node.getInsets().getRight() in the width
     * @return Width of the string, when rendered in that font, in [logical] pixels.
     *         Does not have to be integer.
     */
    public static double measureString(TextInputControl node, String str, Font overrideFont, boolean includeLeftInset, boolean includeRightInset)
    {
        return measured.get(overrideFont).get(str) + (includeLeftInset ? node.getInsets().getLeft() : 0.0) + (includeRightInset ? node.getInsets().getRight() : 0.0);
    }

    /**
     * Measure the given string content using the font of the given node.
     * @return Width of the string, when rendered in that font, in [logical] pixels.
     *         Does not have to be integer.
     */
    public static double measureString(Labeled node, String str)
    {
        return measured.get(node.getFont()).get(str) + node.getLabelPadding().getLeft() + node.getLabelPadding().getRight() + node.getPadding().getLeft() + node.getPadding().getRight();
    }

    /** Gets a new CssMetaData instance for the given property of the class.
     *  Assumes the property is always settable.  Thus do not pass a property that might be
     *  set or bound by other code.
     *  
     *  @param propertyName The name, in CSS, of the property (e.g. "-bj-indent-width")
     *  @param propGetter An accessor on type T for the correspoding JavaFX property
     */
    public static <T extends Styleable> CssMetaData<T, Number> cssSize(String propertyName, Function<T, SimpleStyleableDoubleProperty> propGetter)
    {
        return new CssMetaData <T, Number >(propertyName, StyleConverter.getSizeConverter()) {
            @Override
            public boolean isSettable(T node)
            {
                return true;
            }

            @Override
            public StyleableProperty <Number > getStyleableProperty(T node) {
                return propGetter.apply(node);
            }
        };
    }

    /** Gets a new CssMetaData instance for the given property of the class.
     *  Assumes the property is always settable.  Thus do not pass a property that might be
     *  set or bound by other code.
     *
     *  @param propertyName The name, in CSS, of the property (e.g. "-bj-highlight-color")
     *  @param propGetter An accessor on type T for the correspoding JavaFX property
     */
    public static <T extends Styleable> CssMetaData<T, Color> cssColor(String propertyName, Function<T, SimpleStyleableObjectProperty<Color>> propGetter)
    {
        return new CssMetaData <T, Color >(propertyName, StyleConverter.getColorConverter()) {
            @Override
            public boolean isSettable(T node)
            {
                return true;
            }

            @Override
            public StyleableProperty <Color> getStyleableProperty(T node) {
                return propGetter.apply(node);
            }
        };
    }

    
    /** Gets a new CssMetaData instance for the given property of the class.
     *  Assumes the property is always settable.  Thus do not pass a property that might be
     *  set or bound by other code.
     *  
     *  WARNING: Using this at the moment will encounter bug RT-38723:
     *  https://javafx-jira.kenai.com/browse/RT-38723
     */
    public static <T extends Styleable> CssMetaData<T, Insets> cssInsets(String propertyName, Function<T, StyleableObjectProperty<Insets>> propGetter)
    {
        return new CssMetaData <T, Insets >(propertyName, StyleConverter.getInsetsConverter(), Insets.EMPTY) {
            @Override
            public boolean isSettable(T node)
            {
                return true;
            }

            @Override
            public StyleableProperty <Insets> getStyleableProperty(T node) {
                return propGetter.apply(node);
            }
        };
    }

    /**
     * Writes the given image to the given filename as a PNG
     */
    public static void writeImageTo(WritableImage image, String filename)
    {
        File file = new File(filename);
        RenderedImage renderedImage = SwingFXUtils.fromFXImage(image, null);
        try
        {
            ImageIO.write(
                renderedImage,
                "png",
                file);
        } catch (IOException e)
        {
            Debug.reportError(e);
        }
    }

    /**
     * Works around a problem where key-presses of function keys in text fields
     * do not call accelerators.  See comments in method.
     * 
     * @param field The field on which to work around the issue.
     */
    public static void workAroundFunctionKeyBug(TextField field)
    {
        // Work around taken from http://stackoverflow.com/questions/28239019/javafx-accelerator-not-working-when-textfield-has-focus
        // to work about this bug https://bugs.openjdk.java.net/browse/JDK-8089884 due to be fixed in JDK 9
        field.addEventHandler(KeyEvent.KEY_RELEASED, e -> {
            switch (e.getCode())
            {
                case F1:
                case F2:
                case F3:
                case F4:
                case F5:
                case F6:
                case F7:
                case F8:
                case F9:
                case F10:
                case F11:
                case F12:
                    //handle function key shortcut  if defined
                    Runnable r = field.getScene().getAccelerators().get(new KeyCodeCombination(e.getCode()));
                    if (r != null)
                    {
                        r.run();
                        e.consume();
                    }
                    break;
                default:
                    break;
            }
        });
    }

    /**
     * Makes a clone of the given label by copying its style and content.
     * The copying uses binding, so it will keep up to date with the original afterwards.
     * 
     * @param l The label to copy
     * @param fontSize The font size property (e.g. "12pt") to use for font size 
     */
    public static Label cloneLabel(Label l, ObservableValue<String> fontSize)
    {
        Label copy = new Label();
        copy.textProperty().bind(l.textProperty());
        bindList(copy.getStyleClass(), l.getStyleClass());
        copy.styleProperty().bind(l.styleProperty().concat(fontSize));
        bindPseudoclasses(copy, l.getPseudoClassStates());
        return copy;
    }

    /**
     * Helper method to copy the source image to the destination X, Y position in the dest image
     */
    public static void blitImage(WritableImage dest, int xOffset, int yOffset, Image src)
    {
        dest.getPixelWriter().setPixels(xOffset, yOffset, (int)(Math.ceil(src.getWidth())), (int)(Math.ceil(src.getHeight())), src.getPixelReader(), 0, 0);
    }

    /**
     * Given an ObservableValue with a source object, and a function to get an ObservableValue property of the source object,
     * forms a binding that watches for updates of either the source object or the property of the current
     * source object, to form a virtual observable value for object.property
     *
     * If at any time object becomes null, the returned observable will use the default value.  Null will never be passed
     * to property.
     *
     * @param object The source object on which to apply the property
     * @param property The property to apply to the current source object
     * @param def The default value to use when source is null
     * @param <S> The type of the source object
     * @param <T> The type inside the property
     * @return An observable corresponding to object.property
     */
    @OnThread(Tag.FXPlatform)
    public static <S, T> ObservableValue<T> applyPlatform(ObservableValue<S> object, FXPlatformFunction<S, ObservableValue<T>> property, T def)
    {
        // Easier to create new property and use change listeners to update the binding than try
        // to make an all-in-one binding:
        ObjectProperty<T> r = new SimpleObjectProperty<>(object.getValue() == null ? def : property.apply(object.getValue()).getValue());

        addChangeListenerPlatform(object, value -> {
            r.unbind();

            if (value == null)
            {
                r.setValue(def);
                // r is left unbound;
            }
            else
            {
                // Bind r to the inner property, until object changes:
                r.bind(property.apply(value));
            }
        });

        return r;
    }

    /**
     * Creates a Boolean property which tracks the value of the given source property, but adds
     * delays to copying the true and/or false values.
     *
     * It is possible, if the value keeps flipping backwards and forwards during the delay windows,
     * that you may not see the change copied for a long time.
     *
     * @param source The boolean value to copy into the returned property
     * @param delayToTrue The delay to wait before copying a true value.  Pass Duration.ZERO for no delay.
     * @param delayToFalse The delay to wait before copying a false value.  Pass Duration.ZERO for no delay.
     * @return A property as described.
     */
    public static ReadOnlyBooleanProperty delay(ObservableBooleanValue source, Duration delayToTrue, Duration delayToFalse)
    {
        SimpleBooleanProperty delayed = new SimpleBooleanProperty(source.get());

        source.addListener(new ChangeListener<Boolean>()
        {
            // The task to cancel the previous copy:
            private FXPlatformRunnable cancel = null;
            @Override
            // Assume we are on FX thread (could be dangerous)
            // Tag lets us circumvent thread checker:
            @OnThread(value = Tag.FXPlatform, ignoreParent = true)
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue)
            {
                if (cancel != null)
                    cancel.run();
                if (newValue.booleanValue())
                    cancel = runAfter(delayToTrue, () -> delayed.set(true));
                else
                    cancel = runAfter(delayToFalse, () -> delayed.set(false));
            }
        });

        return delayed;
    }

    @OnThread(Tag.FX)
    public static void ifOnPlatform(FXPlatformRunnable action)
    {
        if (Platform.isFxApplicationThread())
        {
            // Circumvent thread checker:
            ((Runnable)action::run).run();
        }
    }

    public static void addFocusListener(Stage focusTarget, FXPlatformConsumer<Boolean> listener)
    {
        // We know focused property can only be altered on FX thread:
        focusTarget.focusedProperty().addListener((obs, oldVal, newVal) ->
                // Circumvent thread checker:
                ((FXConsumer<Boolean>)listener::accept).accept(newVal));
    }

    public static void addFocusListener(Node focusTarget, FXPlatformConsumer<Boolean> listener)
    {
        // We know focused property can only be altered on FX thread:
        focusTarget.focusedProperty().addListener((obs, oldVal, newVal) ->
                // Circumvent thread checker:
                ((FXConsumer<Boolean>)listener::accept).accept(newVal));
    }

    /**
     * Run an action on the FX platform thread - either now (if called on the platform thread) or
     * at some indeterminate time (if called from a different thread).
     * <p>
     * This method is inherently dangerous and its use should usually be avoided.
     * <p>
     * Calling this from a method tagged with @OnThread(Tag.FX) will potentially run the action
     * simultaneously with the calling thread, which may well cause concurrency bugs. Furthermore
     * if on the "FX" (loading) thread then the design should generally not require swapping to the
     * FX platform thread at all.
     * <p>
     * Calling this from a method tagged with @OnThread(Tag.FXPlatform) is redundant. Instead, just
     * run the action directly. 
     */
    @OnThread(Tag.Any)
    @SuppressWarnings("threadchecker")
    public static void runNowOrLater(FXPlatformRunnable action)
    {
        if (Platform.isFxApplicationThread())
        {
            action.run();
        }
        else
        {
            Platform.runLater(action::run);
        }
    }

     /**
     * Shows a dialog to the user (with OK/Cancel buttons) asking them to confirm an action
     * 
     * @param titleLabel The string to look up in the labels file for the title of the dialog
     * @param messageLabel The string to look up in the labels file for the message of the dialog
     * @param parent The parent window to block with the modal dialog.  Can be null.
     * @param bringToFront If true, should specially execute code to bring app and window to front.
     * @return True if the user clicked OK, false if the user clicked Cancel or otherwise closed the dialog.
     */
    @OnThread(Tag.FXPlatform)
    public static boolean confirmDialog(String titleLabel, String messageLabel, Window parent, boolean bringToFront)
    {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, Config.getString(messageLabel), ButtonType.OK, ButtonType.CANCEL);
        alert.setTitle(Config.getString(titleLabel));
        alert.setHeaderText(alert.getTitle());
        alert.initOwner(parent);
        alert.initModality(Modality.WINDOW_MODAL);
        if (bringToFront)
        {
            listenOnce(alert.showingProperty(), showing -> {
                    if (showing) {
                        Utility.bringToFrontFX(alert.getDialogPane().getScene().getWindow());
                    }
            });
        }
        // Without this line, text will be truncated not wrapped:
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        Optional<ButtonType> pressed = alert.showAndWait();
        return ButtonType.OK == pressed.orElse(ButtonType.CANCEL);
    }

    /**
     * Shows an error dialog to the user, with an OK button
     * 
     * 
     */
    @OnThread(Tag.FXPlatform)
    public static void errorDialog(String titleLabel, String messageLabel)
    {
        Alert alert = new Alert(Alert.AlertType.ERROR, Config.getString(messageLabel), ButtonType.OK);
        alert.setTitle(Config.getString(titleLabel));
        alert.setHeaderText(alert.getTitle());
        alert.showAndWait();
    }

    /**
     * Binds the on/off state of a pseudo-class to the given binding (true = pseudo-class present).
     * 
     * Note that if no reference is maintained to the BooleanBinding, it can get GC-ed,
     * in which case this method will no longer update the pseudo-class.  This is unlikely
     * to be what you want, so you should retain a reference to the exact binding passed
     * to this method for as long as you want the update to occur.
     * 
     * @param node
     * @param pseudoClass
     * @param on
     */
    public static void bindPseudoclass(Node node, String pseudoClass, BooleanExpression on)
    {
        setPseudoclass(pseudoClass, on.get(), node);
        addChangeListener(on, b -> setPseudoclass(pseudoClass, b, node));
    }

    public static void scrollTo(ScrollPane scrollPane, Node target)
    {
        double scrollWidth = scrollPane.getContent().getBoundsInLocal().getWidth();
        double scrollHeight = scrollPane.getContent().getBoundsInLocal().getHeight();
        
        Bounds b = scrollPane.getContent().sceneToLocal(target.localToScene(target.getBoundsInLocal()));
        
        if (scrollPane.getHbarPolicy() != ScrollBarPolicy.NEVER && scrollWidth != 0)
        {
            scrollPane.setHvalue(b.getMinX() / scrollWidth);
        }
        if (scrollPane.getVbarPolicy() != ScrollBarPolicy.NEVER && scrollHeight != 0)
        {
            scrollPane.setVvalue(b.getMinY() / scrollHeight);
        }
    }

    /**
     * Runs some FX initialisation on any thread.
     *
     * We probably need to revisit the threading model.  Nowadays, FXPlatform means actually
     * on the FX Platform thread, and FX means messing with FX, but this can happen in a background
     * thread to support faster loading, as long as you don't add it to the scene.  But really,
     * this means Tag.FX code should be executable on the Swing event thread, because
     * there's no problem if you're just loading.  But if we let Tag.Swing call Tag.FX
     * then we're going to mask all sorts of problems in existing code at the moment.
     * So I've made this method for now, which lets you initialise FX items on
     * the Swing thread, but at least makes it explicit that you're meaning to do it.
     *
     * @param initCode
     * @param <T>
     * @return
     */
    @OnThread(Tag.Any)
    public static <T> T initFX(FXSupplier<T> initCode)
    {
        // Defeat thread checker:
        return ((Supplier<T>)initCode::get).get();
    }

    /**
     * Scales the points by the given scale, and snaps them to nearest integer
     */
    public static void scalePolygonPoints(javafx.scene.shape.Polygon polygon, double scale, boolean rotate90)
    {
        for (int i = 0; i < polygon.getPoints().size(); i += 2)
        {
            if (rotate90)
            {
                // Swapping, so we need a temporary:
                double t = polygon.getPoints().get(i + 1) * scale;
                polygon.getPoints().set(i + 1, polygon.getPoints().get(i) * scale);
                polygon.getPoints().set(i, t);
            }
            else
            {
                polygon.getPoints().set(i, polygon.getPoints().get(i) * scale);
                polygon.getPoints().set(i + 1, polygon.getPoints().get(i + 1) * scale);
            }
        }
    }

    /**
     * Make a printer job.  By default, JavaFX tries to create a job with
     * the default printer, and returns null if that printer is not available.
     * If this happens, we try making a job with any other available printer.
     */
    public static PrinterJob createPrinterJob()
    {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null)
        {
            for (Printer printer : Printer.getAllPrinters())
            {
                job = PrinterJob.createPrinterJob(printer);
                if (job != null)
                    return job;
            }
        }
        return job;
    }

    /**
     * By default, scroll panes leave their content at its preferred size, and then add scroll bars
     * if that is bigger than the scroll pane size.  But often, what we want is twofold:
     *  - If the preferred size is bigger than the scroll pane, add scroll bars
     *  - BUT if the preferred size is smaller than the scroll pane, resize to fit the whole scroll pane.
     *  This method makes the scroll pane have that behaviour.
     */
    @OnThread(Tag.FX)
    public static void expandScrollPaneContent(ScrollPane scrollPane)
    {
        // Make content expand to fill when smaller than viewport, but scroll once larger than viewport:
        // Taken from https://reportmill.wordpress.com/2014/06/03/make-scrollpane-content-fill-viewport-bounds/
        addChangeListener(scrollPane.viewportBoundsProperty(), bounds -> {
            boolean oldFitToWidth = scrollPane.isFitToWidth();
            boolean oldFitToHeight = scrollPane.isFitToHeight();
            scrollPane.setFitToWidth(scrollPane.getContent().prefWidth(-1) < bounds.getWidth());
            scrollPane.setFitToHeight(scrollPane.getContent().prefHeight(-1) < bounds.getHeight());
            // It seems that the layout of the scroll pane content isn't always updated by just changing
            // the fit-to flags, so we also request a layout of the content if applicable:
            if (scrollPane.getContent() instanceof Region && (oldFitToWidth != scrollPane.isFitToWidth() || oldFitToHeight != scrollPane.isFitToHeight()))
            {
                ((Region) scrollPane.getContent()).requestLayout();
            }
        });
    }

    /**
     * Loads the given file as a JavaFX Image.
     *
     * @param image  The system file of the image to load.
     *               If null, null will definitely be returned.
     * @return       The image if successfully loaded, null otherwise.
     */
    public static Image loadImage(File image)
    {
        if (image != null)
        {
            try
            {
                return new Image(image.toURI().toURL().toExternalForm());
            }
            catch (MalformedURLException e)
            {
                Debug.reportError(e);
            }
        }
        
        return null;
    }

    /**
     * Loads a JavaFX Image from the path given
     *
     * @param path  The path of the image to load.
     *              If null, null will definitely be returned.
     * @return      The image if successfully loaded, null otherwise.
     */
    public static Image loadImage(String path)
    {
        return path == null ? null : loadImage(new File(path));
    }

    /**
     * Make a single menu item with a graphics node.
     * 
     * @param nameKey The key to lookup via Config.getString for the name
     * @param icon The node which has the icon of the menu item. It has to be
     *             an ImageView to get the icons working (at least on Mac).
     * @param accelerator The accelerator if any (null if none)
     * @param action The action to perform when the menu item is activated
     * @param binding  The binding for disabling the menu item (may be null).
     * 
     * @return The MenuItem combining all these items.
     */
    public static MenuItem makeMenuItem(String nameKey, ImageView icon, KeyCombination accelerator,
                                        FXPlatformRunnable action, ObservableValue<Boolean> binding)
    {
        MenuItem item = makeMenuItem(nameKey, accelerator, action, binding);
        item.setGraphic(icon);
        return item;
    }

    /**
     * Make a single menu item.
     * 
     * @param nameKey The key to lookup via Config.getString for the name
     * @param accelerator The accelerator if any (null if none)
     * @param action The action to perform when the menu item is activated
     * @param binding  The binding for disabling the menu item (may be null).
     * 
     * @return The MenuItem combining all these items.
     */
    public static MenuItem makeMenuItem(String nameKey, KeyCombination accelerator,
                                        FXPlatformRunnable action, ObservableValue<Boolean> binding)
    {
        MenuItem item = makeMenuItem(Config.getString(nameKey), action, accelerator);
        if (binding != null)
        {
            item.disableProperty().bind(binding);
        }
        return item;
    }

    public static void runAfterNextLayout(Scene scene, FXPlatformRunnable runnable)
    {
        if (scene != null)
        {
            scene.addPostLayoutPulseListener(new Runnable()
            {
                @Override
                public void run()
                {
                    runPlatformLater(runnable);
                    scene.removePostLayoutPulseListener(this);
                }
            });
        }
    }

    /**
     * A builder pattern for lists.
     */
    public static class ListBuilder<T>
    {
        private ArrayList<T> list;

        private ListBuilder(List<T> list)
        {
            this.list = new ArrayList<>(list);
        }

        /**
         * Add another item to the list, return this builder again.
         */
        public ListBuilder<T> add(T t)
        {
            list.add(t);
            return this;
        }

        /**
         * Turn the builder into an actual (unmodifiable) list
         */
        public List<T> build()
        {
            return Collections.unmodifiableList(list);
        }
    }
    
    /**
     * Helper method for extending a super-class's CSS meta data with some of your own
     */
    public static ListBuilder<CssMetaData<? extends Styleable, ?>> extendCss(
            List<CssMetaData<? extends Styleable, ?>> superClassCssMetaData)
    {
        return new ListBuilder<CssMetaData<? extends Styleable, ?>>(superClassCssMetaData);
    }

    /**
     * Adds handlers to the given node so that a popup menu is shown (via the given function)
     * when appropriate.
     * @param node The node to listen for the mouse events on
     * @param showContextMenu The function to call to show the menu.  The parameters are X and Y
     *                        screen (*NOT* scene) coordinates.  Returns true if the menu was shown.
     */
    public static void listenForContextMenu(Node node, FXPlatformBiFunction<Double, Double, Boolean> showContextMenu, KeyCode... otherKeys)
    {
        EventHandler<MouseEvent> popupHandler = e -> {
            if (e.isPopupTrigger())
            {
                if (showContextMenu.apply(e.getScreenX(), e.getScreenY()))
                    e.consume();
            }
        };
        
        // According to docs for isPopupTrigger, we need this handler on pressed and released:
        node.addEventHandler(MouseEvent.MOUSE_PRESSED, popupHandler);
        node.addEventHandler(MouseEvent.MOUSE_RELEASED, popupHandler);
        
        
        node.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.CONTEXT_MENU || Arrays.asList(otherKeys).contains(e.getCode()))
            {
                Point2D scenePt = node.localToScene(5, 5);
                Scene scene = node.getScene();
                Point2D screenPt = scenePt.add(scene.getWindow().getX() + scene.getX(), scene.getWindow().getY() + scene.getY()); 
                if (showContextMenu.apply(screenPt.getX(), screenPt.getY()))
                    e.consume();
            }
        });
    }

    /**
     * Makes a MenuItem with the given parameters.
     * 
     * @param text The content (label) of the menu item
     * @param run The action to run for the menu item.  May be null for none.
     * @param shortcut The shortcut to use.  May be null for none.
     */
    public static MenuItem makeMenuItem(String text, FXPlatformRunnable run, KeyCombination shortcut)
    {
        MenuItem item = new MenuItem(text);
        if (run != null)
            item.setOnAction(e -> run.run());
        if (shortcut != null)
            item.setAccelerator(shortcut);
        return item;
    }

    /**
     * Makes a MenuItem with the given parameters.
     *
     * @param text The content (label) of the menu item, which will be bound to
     * @param run The action to run for the menu item.  May be null for none.
     * @param shortcut The shortcut to use.  May be null for none.
     */
    public static MenuItem makeMenuItem(StringExpression text, FXPlatformRunnable run, KeyCombination shortcut)
    {
        MenuItem item = new MenuItem();
        item.textProperty().bind(text);
        if (run != null)
            item.setOnAction(e -> run.run());
        if (shortcut != null)
            item.setAccelerator(shortcut);
        return item;
    }

    /**
     * Makes a MenuItem which is disabled
     * @param text The content (label) of the menu item
     * @param shortcut The shortcut to display.  May be null for none.
     */
    public static MenuItem makeDisabledMenuItem(String text, KeyCombination shortcut)
    {
        MenuItem item = new MenuItem(text);
        item.setDisable(true);
        if (shortcut != null)
            item.setAccelerator(shortcut);
        return item;
    }

    /**
     * Makes a CheckMenuItem that binds bidirectionally to the given state.
     * 
     * @param text The content (label) for the CheckMenuItem
     * @param state The boolean state to bind the check state to (bidirectionally)
     * @param shortcut The shortcut to use.  May be null for none.
     */
    public static CheckMenuItem makeCheckMenuItem(String text, Property<Boolean> state, KeyCombination shortcut)
    {
        CheckMenuItem item = new CheckMenuItem(text);
        item.selectedProperty().bindBidirectional(state);
        if (shortcut != null)
            item.setAccelerator(shortcut);
        return item;
    }

    /**
     * Makes a CheckMenuItem that has bidirectional listeners to the given state.
     *
     * @param text The content (label) for the CheckMenuItem.
     * @param state The boolean state to bind the check state to (bidirectionally).
     * @param shortcut The shortcut to use.  May be null for none.
     * @param listener An FXConsumer to execute in case the checkMenuItem has been toggled.
     */
    @OnThread(Tag.FXPlatform)
    public static CheckMenuItem makeCheckMenuItem(String text, Property<Boolean> state, KeyCombination shortcut, FXPlatformConsumer<? super Boolean> listener)
    {
        CheckMenuItem item = new CheckMenuItem(text);
        item.setSelected(state.getValue());
        if (shortcut != null)
            item.setAccelerator(shortcut);

        addChangeListenerPlatform(item.selectedProperty(), selected -> {
            if (!selected.equals(state.getValue())) {
                state.setValue(selected);
                listener.accept(selected);
            }
        });

        addChangeListenerPlatform(state, newValue -> {
            item.setSelected(newValue);
        });

        return item;
    }

    /**
     * Helper method to construct a menu
     * @param text The content (label/header) for the menu
     * @param items The items to put in the menu (in order).
     */
    public static Menu makeMenu(String text, MenuItem... items)
    {
        Menu menu = new Menu(text);
        menu.getItems().setAll(items);
        return menu;
    }

    /**
     * Sets up code to show a tooltip on the frame catalogue when the given node is mouse-overed.
     * 
     * @param window The editor window
     * @param target The target node near which to display the tooltip
     * @param tooltipText The text of the tooltip to show
     * @param delay The delay after the mouse enters the node, before showing the toolrip.
     */
    public static void initializeCustomTooltipCatalogue(FXTabbedEditor window, Node target, String tooltipText, Duration delay)
    {
        final Label l = new Label(tooltipText);
        l.visibleProperty().bind(l.textProperty().isNotEmpty());
        l.setMouseTransparent(true);
        l.setWrapText(true);
        JavaFXUtil.addStyleClass(l, "frame-tooltip");
        JavaFXUtil.setPseudoclass("bj-tight-border", true, l);
        l.setMaxWidth(300);

        // We use the window overlay pane to show the tooltip.  The code overlay pane
        // doesn't include the catalogue, so we can't use that.
        FXRunnable show = () -> {
            final WindowOverlayPane overlayPane = window.getOverlayPane();
            double x = overlayPane.sceneXToWindowOverlayX(target.localToScene(target.getBoundsInLocal()).getMinX());
            double y = overlayPane.sceneYToWindowOverlayY(target.localToScene(target.getBoundsInLocal()).getMaxY() + 10);
            overlayPane.addOverlay(l, new ReadOnlyDoubleWrapper(x), new ReadOnlyDoubleWrapper(y), true);
            // Use short fade-in:
            FadeTransition ft = new FadeTransition(Duration.millis(100), l);
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            ft.play();
        };

        target.addEventFilter(MouseEvent.ANY, new EventHandler<MouseEvent>()
            {
                // The action to cancel showing the toolip:
                FXPlatformRunnable cancel;

                @Override
                @OnThread(Tag.FXPlatform)
                public void handle(MouseEvent event)
                {
                    if (event.getEventType() == MouseEvent.MOUSE_ENTERED)
                    {
                        cancel = JavaFXUtil.runAfter(delay, show);
                    }
                    else if (event.getEventType() == MouseEvent.MOUSE_EXITED)
                    {
                        // Cancel any pending show,
                        if (cancel != null)
                        {
                            cancel.run();
                            cancel = null;
                        }
                        // and remove tooltip from window (no-op if not present):
                        window.getOverlayPane().removeOverlay(l);
                    }
                }
            }
        );
    }

    /**
     * A helper class for showing a tooltip/help on mouse-over or pressing F1.
     */
    @OnThread(Tag.FXPlatform)
    private static class TooltipListener implements EventHandler<InputEvent>, FXPlatformConsumer<Boolean>
    {
        /** The label displaying the tooltip. Null when not showing. */
        private Label l;
        private final InteractionManager editor;
        /** The "parent" (not actual scene graph parent, but the node we are showing a tooltip for) */
        private final Node parent;
        /**
         * This is a tricky inversion of control.  Caller passes us a requestTooltip callback.
         * We call it back with another callback for the tooltip.  Caller then spends
         * time (potentially thread-hopping), and when it has the tooltip ready, calls
         * our callback.  We can't easily simplify this because of the gap and thread-hopping
         * inbetween.
         */
        private final FXConsumer<FXConsumer<String>> requestTooltip;
        /** An action to cancel a pending show-on-hover */
        private FXPlatformRunnable cancelHoverShow = null;
        /** Whether the tooltip is currently showing */
        private boolean showing = false;
        /** Whether the tooltip is showing because of mouse hover (only valid when showing == true) */
        private boolean mouseShown = false;

        /**
         * @param editor Reference to the editor
         * @param parent The node for which to show the tooltip
         * @param requestTooltip The callback (see comments above).
         */
        @OnThread(Tag.FX)
        private TooltipListener(InteractionManager editor, Node parent, FXConsumer<FXConsumer<String>> requestTooltip)
        {
            this.editor = editor;
            this.parent = parent;
            this.requestTooltip = requestTooltip;
        }

        @Override
        @OnThread(Tag.FXPlatform)
        public void handle(InputEvent original)
        {
            if (original instanceof KeyEvent)
            {
                KeyEvent event = (KeyEvent) original;
                if (event.getCode() == KeyCode.F1 && !showing)
                {
                    mouseShown = false;
                    if (cancelHoverShow != null)
                    {
                        cancelHoverShow.run();
                        cancelHoverShow = null;
                    }
                    show();
                    event.consume();
                }

                if (event.getCode() == KeyCode.ESCAPE && showing && !mouseShown)
                {
                    hide();
                    // We don't consume escape events in case they trigger more functionality
                }
            }
            else if (original instanceof MouseEvent)
            {
                MouseEvent event = (MouseEvent) original;

                if (event.getEventType() == MouseEvent.MOUSE_ENTERED)
                {
                    if (cancelHoverShow != null)
                    {
                        cancelHoverShow.run();
                        cancelHoverShow = null;
                    }
                    if (!showing)
                    {
                        cancelHoverShow = JavaFXUtil.runAfter(Duration.millis(1000), () -> {
                            mouseShown = true;
                            show();
                        });
                    }
                }
                else if (event.getEventType() == MouseEvent.MOUSE_EXITED)
                {
                    if (cancelHoverShow != null)
                    {
                        cancelHoverShow.run();
                        cancelHoverShow = null;
                    }
                    if (showing && mouseShown)
                    {
                        hide();
                    }
                }
            }
        }

        @OnThread(Tag.FXPlatform)
        private void show()
        {
            if (showing)
                hide();

            l = new Label();
            l.visibleProperty().bind(l.textProperty().isNotEmpty());
            l.setMouseTransparent(true);
            l.setWrapText(true);
            JavaFXUtil.addStyleClass(l, "frame-tooltip");

            requestTooltip.accept(l::setText);
            editor.getCodeOverlayPane().addOverlay(l, parent, null, l.heightProperty().negate().subtract(5.0), WidthLimit.LIMIT_WIDTH_AND_SLIDE_LEFT);
            //FadeTransition ft = new FadeTransition(Duration.millis(100), l);
            //ft.setFromValue(0.0);
            //ft.setToValue(1.0);
            //ft.play();

            showing = true;
        }

        /** Called when focused value of parent changes */
        @Override
        @OnThread(Tag.FXPlatform)
        public void accept(Boolean newVal)
        {

            if (newVal.booleanValue() == false && showing && !mouseShown)
            {
                hide();
            }            
        }

        @OnThread(Tag.FXPlatform)
        private void hide()
        {
            if (showing)
            {
                editor.getCodeOverlayPane().removeOverlay(l);
                l = null;
                showing = false;
            }
        }

    }


    /**
     * Initialises a custom help popup for the given node.  It will show when the user presses
     * F1, and optionally when they mouse hover.
     * 
     * @param editor Reference to the editor, so we can get access to code overlay
     * @param parent The node for which to show the tooltip
     * @param requestTooltip Calculation of tooltip, which we will pass a result-consuming tooltip
     * @param onHoverToo Whether to show on mouse hover (true) and F1, or just on F1 (false)
     */
    public static void initializeCustomHelp(InteractionManager editor, Node parent, FXConsumer<FXConsumer<String>> requestTooltip, boolean onHoverToo)
    {
        TooltipListener listener = new TooltipListener(editor, parent, requestTooltip);
        parent.addEventHandler(KeyEvent.KEY_PRESSED, listener);
        addFocusListener(parent, listener);
        if (onHoverToo)
        {
            parent.addEventHandler(MouseEvent.MOUSE_ENTERED, listener);
            parent.addEventHandler(MouseEvent.MOUSE_EXITED, listener);
        }
    }

    /**
     * Runs the given action (on the FX Platform thread) once the given node
     * gets added to a scene.
     */
    public static void onceInScene(Node node, FXPlatformRunnable action)
    {
        onceBecomesNotNull(node.sceneProperty(), s -> action.run());
    }

    /** 
     * Waits for the observable value to become non-null (if it is not already), then calls the
     * given function on the value once.
     *
     * @param observable The value to wait to become non-null.
     * @param callback The callback to call with the non-null value.  If
     *                 observable's value is already non-null, called immediately before returning
     */
    @OnThread(Tag.FXPlatform)
    public static <T6> void onceNotNull(ObservableValue<T6> observable, FXPlatformConsumer<T6> callback)
    {
        T6 t = observable.getValue();
        
        if (t != null)
        {
            callback.accept(t);
            return;
        }
        
        onceBecomesNotNull(observable, callback);
    }
    
    /** 
     * Waits for the observable value to become non-null, then calls the given function on the
     * value once.
     *
     * @param observable The value to wait to become non-null.
     * @param callback The callback to call with the non-null value.
     */
    @OnThread(Tag.FX)
    public static <T> void onceBecomesNotNull(ObservableValue<T> observable, FXPlatformConsumer<T> callback)
    {
        // Can't be a lambda because we need a reference to ourselves to self-remove:
        ChangeListener<? super T> listener = new ChangeListener<T>() {
            @Override
            @OnThread(value = Tag.FXPlatform, ignoreParent = true)
            public void changed(ObservableValue<? extends T> observable,
                    T oldValue, T newVal)
            {
                if (newVal != null)
                {
                    callback.accept(newVal);
                    observable.removeListener(this);
                }
            }
        };
        observable.addListener(listener);
    }

    /**
     * Waits for the observable value to become true, then calls the given function on the value once.
     *
     * @param observable The value to wait to become true.
     * @param callback The callback to call with the true value.
     *                 If observable's value is already true, call immediately before returning
     */
    @OnThread(Tag.FXPlatform)
    public static void onceTrue(ObservableValue<Boolean> observable, FXPlatformConsumer<Boolean> callback)
    {
        boolean value = observable.getValue();

        if (value)
        {
            callback.accept(value);
            return;
        }

        // Can't be a lambda because we need a reference to ourselves to self-remove:
        ChangeListener<Boolean> listener = new ChangeListener<Boolean>() {
            @Override
            @OnThread(value = Tag.FXPlatform, ignoreParent = true)
            public void changed(ObservableValue<? extends Boolean> observable,
                                Boolean oldValue, Boolean newVal)
            {
                if (newVal)
                {
                    callback.accept(newVal);
                    observable.removeListener(this);
                }
            }
        };
        observable.addListener(listener);
    }

    /**
     * "Binds" the destination list to the observable source list with a transformation function applied.
     * Whenever the source list changes, the destination list is altered to match by applying
     * the given function to each element in the source list.
     * 
     * @param dest The destination list to copy to
     * @param src The source list to copy from
     * @param func The function to apply to each item from src, when copying into dest.
     * @param changeWrapper A callback which will be passed the change function.  You can use this,
     *                      for example, to acquire and release a lock before/after list modification of dest
     */
    public static <SRC2, DEST2> void bindMap(List<DEST2> dest, ObservableList<SRC2> src, Function<SRC2, DEST2> func, FXConsumer<FXRunnable> changeWrapper)
    {
        changeWrapper.accept(() -> {
            dest.clear();
            dest.addAll(Utility.mapList(src, func));
        });
        
        src.addListener(new ListChangeListener<SRC2>()
        {
            @Override
            public void onChanged(ListChangeListener.Change<? extends SRC2> changes)
            {
                changeWrapper.accept(() -> {
                    while (changes.next())
                    {
                        if (changes.wasPermutated() || changes.wasUpdated())
                        {
                            // Same code for updated, replaced and permutation, just recalc the range:
                            for (int i = changes.getFrom(); i < changes.getTo(); i++)
                                dest.set(i, func.apply(src.get(i)));
                        } else
                        {
                            for (int i = 0; i < changes.getRemovedSize(); i++)
                                dest.remove(changes.getFrom());
                            for (int i = 0; i < changes.getAddedSubList().size(); i++)
                                dest.add(i + changes.getFrom(), func.apply(changes.getAddedSubList().get(i)));
                        }
                    }
                });
            }
        });
    }

    /**
     * Takes a BooleanProperty and a list of item.  Gives back an observable list that contains
     * the list of items when (and only when) the BooleanProperty is true, but is empty in the case
     * that the BooleanProperty is false.  Uses JavaFX bindings to update the list's contents.
     *
     * Note that if no reference is maintained to the BooleanExpression, it can get GC-ed,
     * in which case this methods will no longer update the list.  This may be what you want (once
     * the property is no longer in use, this listener will get GC-ed too), but if you don't then
     * make sure you store a reference to the putInList expression.
     * 
     * @param putInList Whether the list should contain the items (true expression) or be empty (false expression)
     * @param items The items to put in the list when putInList is true
     * @return The ObservableList which will (or will not) contain the items.
     */
    public static <T> ObservableList<T> listBool(BooleanExpression putInList, List<T> items)
    {
        ObservableList<T> r = FXCollections.observableArrayList();
        if (putInList.get()) {
            r.setAll(items);
        }
        putInList.addListener((a, b, newVal) -> {
            if (newVal.booleanValue()) {
                r.setAll(items);
            }
            else {
                r.clear();
            }
        });
        return r;
    }

    /**
     * Takes a BooleanProperty and an item.  Gives back an observable list that contains
     * a singleton list of the item when (and only when) the BooleanProperty is true, but is empty in the case
     * that the BooleanProperty is false.  Uses JavaFX bindings to update the list's contents.
     *
     * Note that if no reference is maintained to the BooleanExpression, it can get GC-ed,
     * in which case this methods will no longer update the list.  This may be what you want (once
     * the property is no longer in use, this listener will get GC-ed too), but if you don't then
     * make sure you store a reference to the putInList expression.
     *
     * @param putInList Whether the list should contain the item (true expression) or be empty (false expression)
     * @param items The item to put in the list when putInList is true
     * @return The ObservableList which will (or will not) contain the item.
     */
    public static <T> ObservableList<T> listBool(BooleanExpression putInList, T item)
    {
        return listBool(putInList, List.of(item));
    }

    /**
     * Creates a ChangeListener that will execute the given action (with the new value)
     * once, on the first change, and then remove itself as a listener.  Also returns
     * an action that can remove the listener earlier.
     * 
     * @param prop The observable value
     * @param callback A listener, to be called at most once, on the first change of "prop"
     * @return An action which, if run, removes this listener.  If the listener has already
     *         been run once, has no effect.
     */
    public static <T> FXRunnable addSelfRemovingListener(ObservableValue<T> prop, FXConsumer<T> callback)
    {
        ChangeListener<T> l = new ChangeListener<T>() {
            @Override
            public void changed(ObservableValue<? extends T> observable,
                    T oldValue, T newValue)
            {
                callback.accept(newValue);
                prop.removeListener(this);
            }
            
        };
        prop.addListener(l);
        return () -> prop.removeListener(l);
    }

    /**
     * Creates a ChangeListener that will execute the given action (with the new value)
     * once, on the first change, and then remove itself as a listener. The observable should
     * only be changed on the FX Platform thread.
     * 
     * Also returns an action that can remove the listener earlier.
     * 
     * @param prop The observable value
     * @param callback A listener, to be called at most once, on the first change of "prop"
     * @return An action which, if run, removes this listener.  If the listener has already
     *         been run once, has no effect.
     */
    @OnThread(Tag.FXPlatform)
    public static <T> FXPlatformRunnable listenOnce(ObservableValue<T> prop,
            FXPlatformConsumer<T> callback)
    {
        ChangeListener<T> l = new ChangeListener<T>() {
            @Override
            @OnThread(value = Tag.FXPlatform, ignoreParent = true)
            public void changed(ObservableValue<? extends T> observable,
                    T oldValue, T newValue)
            {
                callback.accept(newValue);
                prop.removeListener(this);
            }
        };
        prop.addListener(l);
        return () -> prop.removeListener(l);
    }
    
    /**
     * Makes one list (dest) always contain the contents of the other (src) until the returned
     * action is executed to cancel the binding.
     * 
     * This is effectively identical to binding on non-lists, but FX doesn't contain built-in
     * support for binding lists, so we do it ourselves.
     * 
     * @param dest The list to copy to
     * @param src The list to copy from
     * @return An action which, if run, cancels this binding effect.
     */
    public static <T> FXRunnable bindList(ObservableList<? super T> dest, ObservableList<T> src)
    {
        ListChangeListener<T> obs = c -> dest.setAll(src);
        src.addListener(obs);
        dest.setAll(src);
        return () -> src.removeListener(obs);
    }

    /**
     * Helper method for ObservableValue.addChangeListener.  It's very rare
     * that you care about all three parameters to addChangeListener (the change,
     * the old value and the new value).  Usually, you just want the new value.
     * This method lets you specify a lambda which only takes the new value,
     * and thus saves you having two unused parameters every time.
     * 
     * @param property The observable item to add the change listener to.
     *                 Must only ever be altered on the FX thread.
     * @param listener The listener to add; whenever there is a change in the property,
     *                 the listener will be called back with the new value.
     * @param <T>      The type in the observable.
     * @return An action which, if run, removes the change listener from the property.
     */
    public static <T> FXRunnable addChangeListener(ObservableValue<T> property, FXConsumer<? super T> listener)
    {
        ChangeListener<T> wrapped = (a, b, newVal) -> listener.accept(newVal);
        property.addListener(wrapped);
        return () -> property.removeListener(wrapped);
    }

    /**
     * Like addChangeListener, but for when the item will definitely only be changed on the platform thread
     */
    @OnThread(Tag.FX)
    @SuppressWarnings("threadchecker")
    public static <T> FXPlatformRunnable addChangeListenerPlatform(ObservableValue<T> property,
            FXPlatformConsumer<T> listener)
    {
        ChangeListener<T> wrapped = (a, b, newVal) -> listener.accept(newVal);
        property.addListener(wrapped);
        return () -> property.removeListener(wrapped);
    }

    /**
     * Creates a single FXRunnable which runs the given list of actions in order,
     * one after the other, on the FX thread.
     */
    public static FXRunnable sequence(FXRunnable... actions)
    {
        return () -> Arrays.stream(actions).forEach(FXRunnable::run);
    }

    /**
     * Runs the given task on the FX Platform thread after the given delay.
     * Returns a task which, if executed, cancels the task.  This can be done synchronously,
     * since the task runs on the FX thread and so does this call; you cannot make this call
     * during an execution of the task.
     * 
     * @param delay The delay before running the task (if zero or less, task is run before returning)
     * @param task The task to run (on the FX thread)
     * @return An action which, if run, cancels the execution of the task.  If the task
     *         has already run, running this returned item has no effect.
     */
    @OnThread(Tag.FXPlatform)
    public static FXPlatformRunnable runAfter(Duration delay, FXPlatformRunnable task)
    {
        if (delay.lessThanOrEqualTo(Duration.ZERO))
        {
            // Run immediately:
            task.run();
            return () -> {};
        }
        
        // The documentation for stop says it may not stop immediately, so we
        // use this boolean as a fail safe:
        BooleanProperty okToRun = new SimpleBooleanProperty(true);
        Timeline timeline = new Timeline(new KeyFrame(delay, e -> {
            if (okToRun.get())
                task.run();
        }));
        timeline.setCycleCount(1);
        timeline.play();
        return () -> {
            okToRun.set(false);
            timeline.stop();
        };
    }
    
    /**
     * Runs the given task on the FX Platform thread after the given interval,
     * and then forever-after, with the given interval between each execution.
     * 
     * Returns an action which, if executed, cancels the task.  This can be done synchronously,
     * since the task runs on the FX thread and so does this call; you cannot make this call
     * during an execution of the task.
     * 
     * @param interval The interval between tasks, and from now until the first run.
     *                 Must be greater than zero
     * @param task     The task to run (on the FX thread)
     * @return An action which, if executed, will cancel all future executions of the task.
     */
    public static FXRunnable runRegular(Duration interval, FXPlatformRunnable task)
    {
        if (interval.lessThanOrEqualTo(Duration.ZERO))
        {
            throw new IllegalArgumentException("Cannot run at a regular interval of zero or less");
        }
        
        // The documentation for stop says it may not stop immediately, so we
        // use this boolean as a fail safe:
        BooleanProperty okToRun = new SimpleBooleanProperty(true);
        Timeline timeline = new Timeline(new KeyFrame(interval, e -> {
            if (okToRun.get())
                task.run();
        }));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.setAutoReverse(false);
        timeline.play();
        return () -> {
            okToRun.set(false);
            timeline.stop();
        };
    }
    
    public static enum DragType
    {
        /** The copy key (Mac: option, Others: Ctrl) was held down */
        FORCE_COPYING,
        /** The move key (shift) was held down */
        FORCE_MOVING,
        /** No modifier was held down */
        DEFAULT;
    }

    /**
     * Checks if the drag-copy or drag-move modifiers was held down during a particular mouse event.
     * 
     * Drag-copy: On Mac this is the option key.  On Windows and Linux, this is the Ctrl key.
     * Drag-move: The shift key.
     */
    public static DragType getDragModifiers(MouseEvent event)
    {
        boolean forceCopy = Config.isMacOS() ? event.isAltDown() : event.isShortcutDown();
        boolean forceMove = event.isShiftDown();
        if (forceCopy)
            return DragType.FORCE_COPYING;
        else if (forceMove)
            return DragType.FORCE_MOVING;
        else
            return DragType.DEFAULT;
    }

    /**
     * Binds the pseudo-classes on the given node to the given set.
     * 
     * This translates into adding/removing classes as they are added/removed
     * from the given set, as well as adding all classes from the set initially.
     * 
     * If there is an orthogonal class, i.e. one never used at all in the from set,
     * it is valid to set that class on/off on the "to" node before or after this binding call.
     * 
     * @param to The node on which to set the pseudo-classes
     * @param from The set of pseudo-classes to copy from
     */
    public static void bindPseudoclasses(Node to, ObservableSet<PseudoClass> from)
    {
        from.addListener((SetChangeListener<PseudoClass>)c -> {
            if (c.wasAdded())
                to.pseudoClassStateChanged(c.getElementAdded(), true);
            if (c.wasRemoved())
                to.pseudoClassStateChanged(c.getElementRemoved(), false);
        });
        from.forEach(c -> to.pseudoClassStateChanged(c, true));
    }

    /**
     * Converts a Swing menu bar into a JavaFX menu bar.  For use when you convert
     * a Swing window into a SwingNode inside a JavaFX menu, but the Swing window
     * had a menu bar.  All the Swing actions are executed back on the Swing thread,
     * using the given eventSource in the ActionEvent
     *
     * @param swingMenu The swing menu to convert.
     * @param eventSource The source to be used in ActionEvent items which are passed to actionPerformed
     * @return An action to run on the FX thread which will create the FX menu
     */
    @OnThread(Tag.Swing)
    public static FXPlatformSupplier<MenuBar> swingMenuBarToFX(JMenuBar swingMenu, Object eventSource)
    {
        List<FXPlatformSupplier<Menu>> menus = new ArrayList<>();

        for (int i = 0; i < swingMenu.getMenuCount(); i++)
        {
            JMenu menu = swingMenu.getMenu(i);
            menus.add(swingMenuToFX(menu, eventSource, (a, b) -> {}));
        }
        return () -> {
            MenuBar menuBar = new MenuBar();
            for (FXPlatformSupplier<Menu> menuFXSupplier : menus)
            {
                menuBar.getMenus().add(menuFXSupplier.get());
            }
            return menuBar;
        };
    }
    
    public static interface SwingOrFXMenu
    {
        @OnThread(Tag.Swing)
        FXPlatformSupplier<Menu> getFXMenu(Object eventSource);
    }
    public static class SwingMenu implements SwingOrFXMenu
    {
        private final JMenu swingMenu;

        @OnThread(Tag.Any)
        public SwingMenu(JMenu swingMenu)
        {
            this.swingMenu = swingMenu;
        }

        @Override
        @OnThread(Tag.Swing)
        public FXPlatformSupplier<Menu> getFXMenu(Object eventSource)
        {
            return swingMenuToFX(swingMenu, eventSource, (a, b) -> {});
        }
    }
    public static class FXOnlyMenu implements SwingOrFXMenu
    {
        private final Menu fxMenu;

        @OnThread(Tag.Any)
        public FXOnlyMenu(Menu fxMenu)
        {
            this.fxMenu = fxMenu;
        }

        @Override
        @OnThread(Tag.Swing)
        public FXPlatformSupplier<Menu> getFXMenu(Object eventSource)
        {
            return () -> fxMenu;
        }
    }

    @OnThread(Tag.Swing)
    private static FXPlatformSupplier<Menu> swingMenuToFX(JMenu swingMenu, Object source, FXBiConsumer<JMenuItem, MenuItem> extraConvert)
    {
        List<JMenuItem> items = new ArrayList<>();
        for (int i = 0; i < swingMenu.getItemCount(); i++)
        {
            items.add(swingMenu.getItem(i));
        }
        String swingMenuTitle = swingMenu.getText();
        return swingMenuToFX(items, source, () -> new Menu(swingMenuTitle), extraConvert);
    }

    /**
     * Helper method for swingMenuBarToFX, see docs for that method
     */
    @OnThread(Tag.Swing)
    public static FXPlatformSupplier<Menu> swingMenuToFX(List<JMenuItem> swingMenuItems, Object source, FXPlatformSupplier<Menu> createMenu, FXBiConsumer<JMenuItem, MenuItem> extraConvert)
    {
        List<FXPlatformSupplier<? extends MenuItem>> items = new ArrayList<>();
        List<FXPlatformRunnable> onShow = new ArrayList<>();
        for (JMenuItem item : swingMenuItems)
        {
            if (item == null)
                // Separator:
                items.add(createMenuSeparator());
            else if (item instanceof JMenu)
                items.add(swingMenuToFX((JMenu)item, source, extraConvert));
            else
            {
                FXPlatformSupplier<MenuItemAndShow> menuItemAndShowFXSupplier = swingMenuItemToFX(item, source);
                FXPlatformSupplier<MenuItem> menuItemFXSupplier = () -> {
                    MenuItemAndShow itemAndShow = menuItemAndShowFXSupplier.get();
                    if (itemAndShow.onShow != null)
                        onShow.add(itemAndShow.onShow);
                    extraConvert.accept(item, itemAndShow.menuItem);
                    return itemAndShow.menuItem;
                };
                items.add(menuItemFXSupplier);
            }
        }
        return () -> {
            Menu menu = createMenu.get();
            for (FXPlatformSupplier<? extends MenuItem> menuItemFXSupplier : items)
            {
                menu.getItems().add(menuItemFXSupplier.get());
            }
            // Note: it is the supplier
            // which adds all the updates to the onShow list:
            menu.setOnShowing(e -> {
                onShow.forEach(FXPlatformRunnable::run);
            });
            return menu;
        };
    }
    
    @OnThread(Tag.Any)
    private static FXPlatformSupplier<? extends MenuItem> createMenuSeparator()
    {
        return () -> new SeparatorMenuItem();
    }

    private static class MenuItemAndShow
    {
        private final MenuItem menuItem;
        /**
         * Action to run when the menu containing this item is shown:
         */
        private final FXRunnable onShow;

        public MenuItemAndShow(MenuItem menuItem, FXRunnable onShow)
        {
            this.menuItem = menuItem;
            this.onShow = onShow;
        }

        public MenuItemAndShow(MenuItem menuItem)
        {
            this(menuItem, null);
        }
    }

    /**
     * Helper method for swingMenuBarToFX, see docs for that method
     */
    @OnThread(Tag.Swing)
    private static FXPlatformSupplier<MenuItemAndShow> swingMenuItemToFX(JMenuItem swingItem, Object source)
    {
        String menuText = swingItem.getText();
        AWTKeyStroke shortcut = swingItem.getAccelerator();
        if (swingItem instanceof JCheckBoxMenuItem)
        {
            JCheckBoxMenuItem checkBoxMenuItem = (JCheckBoxMenuItem) swingItem;
            boolean selected = checkBoxMenuItem.isSelected();
            return () -> {
                CheckMenuItem item = new CheckMenuItem(menuText);
                item.setSelected(selected);
                // Clicking the icon crosses back to the Swing thread to attempt state change:
                item.setOnAction(e -> {
                    int x = (int)item.getParentPopup().getX();
                    int y = (int)item.getParentPopup().getY();
                    SwingUtilities.invokeLater(() -> {
                        checkBoxMenuItem.setSelected(!checkBoxMenuItem.isSelected());
                        fireSwingMenuItemAction(swingItem, source, x, y);
                    });
                });
                item.setAccelerator(swingKeyStrokeToFX(shortcut));
                // We must also check the state (on Swing thread) before showing, because it may
                // have been altered by other actions, e.g. the Show Terminal menu item is affected
                // by the user closing the terminal window, not just by triggering the menu item
                FXRunnable getStatus = () -> {
                    SwingUtilities.invokeLater(() -> {
                        boolean curSelected = checkBoxMenuItem.isSelected();
                        Platform.runLater(() -> item.setSelected(curSelected));
                    });
                };
                SwingUtilities.invokeLater(() -> {
                    swingItem.addPropertyChangeListener("enabled", e2 -> {
                        boolean enabled = swingItem.isEnabled();
                        Platform.runLater(() -> item.setDisable(!enabled));
                    });
                });
                return new MenuItemAndShow(item, getStatus);
            };
        }
        else if (swingItem instanceof JRadioButtonMenuItem)
        {
            // We don't have any radio button menu items in our code currently
            throw new UnsupportedOperationException();
        }
        else
        {
            boolean startsEnabled = swingItem.isEnabled();
            // Circumvent thread-checker; it is ok to make an FX menu item on this
            // thread as long as we don't try to add it to scene graph:
            MenuItem item = ((BiFunction<String, Boolean, MenuItem>)JavaFXUtil::makeFXMenuItem).apply(menuText, startsEnabled);
            // Must add listeners now, because if change occurs between us returning an FX action,
            // and the action actually being run, we could miss it.
            swingItem.addPropertyChangeListener("enabled", e2 -> {
                boolean enabled = swingItem.isEnabled();
                Platform.runLater(() -> item.setDisable(!enabled));
            });
            swingItem.addPropertyChangeListener("action", e2 -> {
                boolean enabled = swingItem.isEnabled();
                String label = swingItem.getText();
                Platform.runLater(() -> {
                    item.setDisable(!enabled);
                    item.setText(label);
                });
            });
            
            return () -> {
                item.setOnAction(e -> {
                	ContextMenu menu = item.getParentPopup();
                	int x = menu != null ? (int)menu.getX() : 0;
                	int y = menu != null ? (int)menu.getY() : 0;
                    SwingUtilities.invokeLater(() -> fireSwingMenuItemAction(swingItem, source, x, y));
                });
                item.setAccelerator(swingKeyStrokeToFX(shortcut));
                return new MenuItemAndShow(item);
            };
        }
    }

    @OnThread(Tag.Swing)
    private static void fireSwingMenuItemAction(JMenuItem swingItem, Object source, int x, int y)
    {
        String menuText = swingItem.getText();

        // This is a bit hacky, but it is specifically to get the UML extension
        // and Klassekarte extensions working again.  For that we only need Component.getLocationOnScreen
        // to function correctly, and can have dummy values elsewhere:
        for (MouseListener mouseListener : swingItem.getMouseListeners())
        {
            mouseListener.mousePressed(new java.awt.event.MouseEvent(new Component() {
                @Override
                public Point getLocationOnScreen()
                {
                    return new Point(x, y);
                }
            }, 0, 0, 0, 0, 0, 1, false));
        }

        // Important we fetch action here not cache it earlier because
        // it may change after we make the menu item:
        Action action = swingItem.getAction();
        if (action != null) {
            action.actionPerformed(new java.awt.event.ActionEvent(source, 0, menuText));
        }
        else {
            // If no action, just call action listeners ourselves:
            Arrays.stream(swingItem.getActionListeners()).forEach(a -> a.actionPerformed(new java.awt.event.ActionEvent(source, 0, menuText)));
        }
    }

    private static MenuItem makeFXMenuItem(String menuText, boolean startsEnabled)
    {
        MenuItem item = new MenuItem(menuText);
        item.setDisable(!startsEnabled);
        return item;
    }

    /**
     * Converts an AWT key stroke combination to a JavaFX one.
     */
    private static KeyCombination swingKeyStrokeToFX(AWTKeyStroke shortcut)
    {
        if (shortcut == null)
            return null;

        int modifiers = shortcut.getModifiers();
        List<Modifier> fxModifiers = new ArrayList<>();
        if ((modifiers & java.awt.event.InputEvent.SHIFT_DOWN_MASK) != 0)
            fxModifiers.add(KeyCombination.SHIFT_DOWN);
        if ((modifiers & java.awt.event.InputEvent.CTRL_DOWN_MASK) != 0)
            fxModifiers.add(KeyCombination.CONTROL_DOWN);
        if ((modifiers & java.awt.event.InputEvent.META_DOWN_MASK) != 0)
            fxModifiers.add(KeyCombination.META_DOWN);
        // We don't support other modifiers on menu items: e.g. Alt as a
        // menu item shortcut is not supported because it doesn't work on some OSes
        KeyCode code = awtKeyCodeToFX(shortcut.getKeyCode());
        if (code != null)
            return new KeyCodeCombination(code, fxModifiers.toArray(new KeyCombination.Modifier[0]));
        else
            return null;
    }

    /**
     * Converts an integer AWT key code to a JavaFX enum.
     * Can return null if the keycode is unknown or unmappable
     */
    public static KeyCode awtKeyCodeToFX(int code)
    {
        // There may be a better way of doing this:
        switch (code)
        {
            // Letters, numbers, Function keys:
            case java.awt.event.KeyEvent.VK_A: return KeyCode.A;
            case java.awt.event.KeyEvent.VK_B: return KeyCode.B;
            case java.awt.event.KeyEvent.VK_C: return KeyCode.C;
            case java.awt.event.KeyEvent.VK_D: return KeyCode.D;
            case java.awt.event.KeyEvent.VK_E: return KeyCode.E;
            case java.awt.event.KeyEvent.VK_F: return KeyCode.F;
            case java.awt.event.KeyEvent.VK_G: return KeyCode.G;
            case java.awt.event.KeyEvent.VK_H: return KeyCode.H;
            case java.awt.event.KeyEvent.VK_I: return KeyCode.I;
            case java.awt.event.KeyEvent.VK_J: return KeyCode.J;
            case java.awt.event.KeyEvent.VK_K: return KeyCode.K;
            case java.awt.event.KeyEvent.VK_L: return KeyCode.L;
            case java.awt.event.KeyEvent.VK_M: return KeyCode.M;
            case java.awt.event.KeyEvent.VK_N: return KeyCode.N;
            case java.awt.event.KeyEvent.VK_O: return KeyCode.O;
            case java.awt.event.KeyEvent.VK_P: return KeyCode.P;
            case java.awt.event.KeyEvent.VK_Q: return KeyCode.Q;
            case java.awt.event.KeyEvent.VK_R: return KeyCode.R;
            case java.awt.event.KeyEvent.VK_S: return KeyCode.S;
            case java.awt.event.KeyEvent.VK_T: return KeyCode.T;
            case java.awt.event.KeyEvent.VK_U: return KeyCode.U;
            case java.awt.event.KeyEvent.VK_V: return KeyCode.V;
            case java.awt.event.KeyEvent.VK_W: return KeyCode.W;
            case java.awt.event.KeyEvent.VK_X: return KeyCode.X;
            case java.awt.event.KeyEvent.VK_Y: return KeyCode.Y;
            case java.awt.event.KeyEvent.VK_Z: return KeyCode.Z;
            case java.awt.event.KeyEvent.VK_0: return KeyCode.DIGIT0;
            case java.awt.event.KeyEvent.VK_1: return KeyCode.DIGIT1;
            case java.awt.event.KeyEvent.VK_2: return KeyCode.DIGIT2;
            case java.awt.event.KeyEvent.VK_3: return KeyCode.DIGIT3;
            case java.awt.event.KeyEvent.VK_4: return KeyCode.DIGIT4;
            case java.awt.event.KeyEvent.VK_5: return KeyCode.DIGIT5;
            case java.awt.event.KeyEvent.VK_6: return KeyCode.DIGIT6;
            case java.awt.event.KeyEvent.VK_7: return KeyCode.DIGIT7;
            case java.awt.event.KeyEvent.VK_8: return KeyCode.DIGIT8;
            case java.awt.event.KeyEvent.VK_9: return KeyCode.DIGIT9;
            case java.awt.event.KeyEvent.VK_NUMPAD0: return KeyCode.NUMPAD0;
            case java.awt.event.KeyEvent.VK_NUMPAD1: return KeyCode.NUMPAD1;
            case java.awt.event.KeyEvent.VK_NUMPAD2: return KeyCode.NUMPAD2;
            case java.awt.event.KeyEvent.VK_NUMPAD3: return KeyCode.NUMPAD3;
            case java.awt.event.KeyEvent.VK_NUMPAD4: return KeyCode.NUMPAD4;
            case java.awt.event.KeyEvent.VK_NUMPAD5: return KeyCode.NUMPAD5;
            case java.awt.event.KeyEvent.VK_NUMPAD6: return KeyCode.NUMPAD6;
            case java.awt.event.KeyEvent.VK_NUMPAD7: return KeyCode.NUMPAD7;
            case java.awt.event.KeyEvent.VK_NUMPAD8: return KeyCode.NUMPAD8;
            case java.awt.event.KeyEvent.VK_NUMPAD9: return KeyCode.NUMPAD9;
            case java.awt.event.KeyEvent.VK_F1: return KeyCode.F1;
            case java.awt.event.KeyEvent.VK_F2: return KeyCode.F2;
            case java.awt.event.KeyEvent.VK_F3: return KeyCode.F3;
            case java.awt.event.KeyEvent.VK_F4: return KeyCode.F4;
            case java.awt.event.KeyEvent.VK_F5: return KeyCode.F5;
            case java.awt.event.KeyEvent.VK_F6: return KeyCode.F6;
            case java.awt.event.KeyEvent.VK_F7: return KeyCode.F7;
            case java.awt.event.KeyEvent.VK_F8: return KeyCode.F8;
            case java.awt.event.KeyEvent.VK_F9: return KeyCode.F9;
            case java.awt.event.KeyEvent.VK_F10: return KeyCode.F10;
            case java.awt.event.KeyEvent.VK_F11: return KeyCode.F11;
            case java.awt.event.KeyEvent.VK_F12: return KeyCode.F12;
            case java.awt.event.KeyEvent.VK_F13: return KeyCode.F13;
            case java.awt.event.KeyEvent.VK_F14: return KeyCode.F14;
            case java.awt.event.KeyEvent.VK_F15: return KeyCode.F15;
            case java.awt.event.KeyEvent.VK_F16: return KeyCode.F16;
            case java.awt.event.KeyEvent.VK_F17: return KeyCode.F17;
            case java.awt.event.KeyEvent.VK_F18: return KeyCode.F18;
            case java.awt.event.KeyEvent.VK_F19: return KeyCode.F19;
            case java.awt.event.KeyEvent.VK_F20: return KeyCode.F20;
            case java.awt.event.KeyEvent.VK_F21: return KeyCode.F21;
            case java.awt.event.KeyEvent.VK_F22: return KeyCode.F22;
            case java.awt.event.KeyEvent.VK_F23: return KeyCode.F23;
            case java.awt.event.KeyEvent.VK_F24: return KeyCode.F24;

            // Rest, alphabetical:
            case java.awt.event.KeyEvent.VK_ACCEPT: return KeyCode.ACCEPT;
            case java.awt.event.KeyEvent.VK_ADD: return KeyCode.ADD;
            case java.awt.event.KeyEvent.VK_AGAIN: return KeyCode.AGAIN;
            case java.awt.event.KeyEvent.VK_ALL_CANDIDATES: return KeyCode.ALL_CANDIDATES;
            case java.awt.event.KeyEvent.VK_ALPHANUMERIC: return KeyCode.ALPHANUMERIC;
            case java.awt.event.KeyEvent.VK_AMPERSAND: return KeyCode.AMPERSAND;
            case java.awt.event.KeyEvent.VK_ASTERISK: return KeyCode.ASTERISK;
            case java.awt.event.KeyEvent.VK_AT: return KeyCode.AT;
            case java.awt.event.KeyEvent.VK_BACK_QUOTE: return KeyCode.BACK_QUOTE;
            case java.awt.event.KeyEvent.VK_BACK_SLASH: return KeyCode.BACK_SLASH;
            case java.awt.event.KeyEvent.VK_BACK_SPACE: return KeyCode.BACK_SPACE;
            case java.awt.event.KeyEvent.VK_BEGIN: return KeyCode.BEGIN;
            case java.awt.event.KeyEvent.VK_BRACELEFT: return KeyCode.BRACELEFT;
            case java.awt.event.KeyEvent.VK_BRACERIGHT: return KeyCode.BRACERIGHT;
            case java.awt.event.KeyEvent.VK_CANCEL: return KeyCode.CANCEL;
            case java.awt.event.KeyEvent.VK_CAPS_LOCK: return KeyCode.CAPS;
            case java.awt.event.KeyEvent.VK_CIRCUMFLEX: return KeyCode.CIRCUMFLEX;
            case java.awt.event.KeyEvent.VK_CLEAR: return KeyCode.CLEAR;
            case java.awt.event.KeyEvent.VK_CLOSE_BRACKET: return KeyCode.CLOSE_BRACKET;
            case java.awt.event.KeyEvent.VK_CODE_INPUT: return KeyCode.CODE_INPUT;
            case java.awt.event.KeyEvent.VK_COLON: return KeyCode.COLON;
            case java.awt.event.KeyEvent.VK_COMMA: return KeyCode.COMMA;
            case java.awt.event.KeyEvent.VK_COPY: return KeyCode.COPY;
            case java.awt.event.KeyEvent.VK_CUT: return KeyCode.CUT;
            case java.awt.event.KeyEvent.VK_DECIMAL: return KeyCode.DECIMAL;
            case java.awt.event.KeyEvent.VK_DELETE: return KeyCode.DELETE;
            case java.awt.event.KeyEvent.VK_DIVIDE: return KeyCode.DIVIDE;
            case java.awt.event.KeyEvent.VK_DOLLAR: return KeyCode.DOLLAR;
            case java.awt.event.KeyEvent.VK_DOWN: return KeyCode.DOWN;
            case java.awt.event.KeyEvent.VK_END: return KeyCode.END;
            case java.awt.event.KeyEvent.VK_ENTER: return KeyCode.ENTER;
            case java.awt.event.KeyEvent.VK_EQUALS: return KeyCode.EQUALS;
            case java.awt.event.KeyEvent.VK_ESCAPE: return KeyCode.ESCAPE;
            case java.awt.event.KeyEvent.VK_EURO_SIGN: return KeyCode.EURO_SIGN;
            case java.awt.event.KeyEvent.VK_EXCLAMATION_MARK: return KeyCode.EXCLAMATION_MARK;
            case java.awt.event.KeyEvent.VK_FINAL: return KeyCode.FINAL;
            case java.awt.event.KeyEvent.VK_FIND: return KeyCode.FIND;
            case java.awt.event.KeyEvent.VK_FULL_WIDTH: return KeyCode.FULL_WIDTH;
            case java.awt.event.KeyEvent.VK_GREATER: return KeyCode.GREATER;
            case java.awt.event.KeyEvent.VK_HALF_WIDTH: return KeyCode.HALF_WIDTH;
            case java.awt.event.KeyEvent.VK_HELP: return KeyCode.HELP;
            case java.awt.event.KeyEvent.VK_HIRAGANA: return KeyCode.HIRAGANA;
            case java.awt.event.KeyEvent.VK_HOME: return KeyCode.HOME;
            case java.awt.event.KeyEvent.VK_INSERT: return KeyCode.INSERT;
            case java.awt.event.KeyEvent.VK_INPUT_METHOD_ON_OFF: return KeyCode.INPUT_METHOD_ON_OFF;
            case java.awt.event.KeyEvent.VK_INVERTED_EXCLAMATION_MARK: return KeyCode.INVERTED_EXCLAMATION_MARK;
            case java.awt.event.KeyEvent.VK_JAPANESE_HIRAGANA: return KeyCode.JAPANESE_HIRAGANA;
            case java.awt.event.KeyEvent.VK_JAPANESE_KATAKANA: return KeyCode.JAPANESE_KATAKANA;
            case java.awt.event.KeyEvent.VK_JAPANESE_ROMAN: return KeyCode.JAPANESE_ROMAN;
            case java.awt.event.KeyEvent.VK_KANA: return KeyCode.KANA;
            case java.awt.event.KeyEvent.VK_KANA_LOCK: return KeyCode.KANA_LOCK;
            case java.awt.event.KeyEvent.VK_KANJI: return KeyCode.KANJI;
            case java.awt.event.KeyEvent.VK_KATAKANA: return KeyCode.KATAKANA;
            case java.awt.event.KeyEvent.VK_KP_DOWN: return KeyCode.KP_DOWN;
            case java.awt.event.KeyEvent.VK_KP_LEFT: return KeyCode.KP_LEFT;
            case java.awt.event.KeyEvent.VK_KP_RIGHT: return KeyCode.KP_RIGHT;
            case java.awt.event.KeyEvent.VK_KP_UP: return KeyCode.KP_UP;
            case java.awt.event.KeyEvent.VK_LEFT: return KeyCode.LEFT;
            case java.awt.event.KeyEvent.VK_LEFT_PARENTHESIS: return KeyCode.LEFT_PARENTHESIS;
            case java.awt.event.KeyEvent.VK_LESS: return KeyCode.LESS;
            case java.awt.event.KeyEvent.VK_META: return KeyCode.META;
            case java.awt.event.KeyEvent.VK_MINUS: return KeyCode.MINUS;
            case java.awt.event.KeyEvent.VK_MODECHANGE: return KeyCode.MODECHANGE;
            case java.awt.event.KeyEvent.VK_MULTIPLY: return KeyCode.MULTIPLY;
            case java.awt.event.KeyEvent.VK_NONCONVERT: return KeyCode.NONCONVERT;
            case java.awt.event.KeyEvent.VK_NUM_LOCK: return KeyCode.NUM_LOCK;
            case java.awt.event.KeyEvent.VK_NUMBER_SIGN: return KeyCode.NUMBER_SIGN;
            case java.awt.event.KeyEvent.VK_OPEN_BRACKET: return KeyCode.OPEN_BRACKET;
            case java.awt.event.KeyEvent.VK_PAGE_DOWN: return KeyCode.PAGE_DOWN;
            case java.awt.event.KeyEvent.VK_PAGE_UP: return KeyCode.PAGE_UP;
            case java.awt.event.KeyEvent.VK_PASTE: return KeyCode.PASTE;
            case java.awt.event.KeyEvent.VK_PAUSE: return KeyCode.PAUSE;
            case java.awt.event.KeyEvent.VK_PERIOD: return KeyCode.PERIOD;
            case java.awt.event.KeyEvent.VK_PLUS: return KeyCode.PLUS;
            case java.awt.event.KeyEvent.VK_PREVIOUS_CANDIDATE: return KeyCode.PREVIOUS_CANDIDATE;
            case java.awt.event.KeyEvent.VK_PRINTSCREEN: return KeyCode.PRINTSCREEN;
            case java.awt.event.KeyEvent.VK_PROPS: return KeyCode.PROPS;
            case java.awt.event.KeyEvent.VK_QUOTE: return KeyCode.QUOTE;
            case java.awt.event.KeyEvent.VK_QUOTEDBL: return KeyCode.QUOTEDBL;
            case java.awt.event.KeyEvent.VK_RIGHT: return KeyCode.RIGHT;
            case java.awt.event.KeyEvent.VK_RIGHT_PARENTHESIS: return KeyCode.RIGHT_PARENTHESIS;
            case java.awt.event.KeyEvent.VK_ROMAN_CHARACTERS: return KeyCode.ROMAN_CHARACTERS;
            case java.awt.event.KeyEvent.VK_SCROLL_LOCK: return KeyCode.SCROLL_LOCK;
            case java.awt.event.KeyEvent.VK_SEMICOLON: return KeyCode.SEMICOLON;
            case java.awt.event.KeyEvent.VK_SEPARATOR: return KeyCode.SEPARATOR;
            case java.awt.event.KeyEvent.VK_SLASH: return KeyCode.SLASH;
            case java.awt.event.KeyEvent.VK_SPACE: return KeyCode.SPACE;
            case java.awt.event.KeyEvent.VK_STOP: return KeyCode.STOP;
            case java.awt.event.KeyEvent.VK_SUBTRACT: return KeyCode.SUBTRACT;
            case java.awt.event.KeyEvent.VK_TAB: return KeyCode.TAB;
            case java.awt.event.KeyEvent.VK_UNDERSCORE: return KeyCode.UNDERSCORE;
            case java.awt.event.KeyEvent.VK_UNDO: return KeyCode.UNDO;
            case java.awt.event.KeyEvent.VK_UP: return KeyCode.UP;
        }
        Debug.message("Unknown key code: " + code);
        return null;
    }

    public static <T,R> ObjectBinding<R> of(ObservableValue<T> t, FXFunction<T, R> accessor)
    {
        return Bindings.createObjectBinding(() -> accessor.apply(t.getValue()), t);
    }

    public static <T> DoubleBinding ofD(ObservableValue<T> t, FXFunction<T, Double> accessor)
    {
        return Bindings.createDoubleBinding(() -> accessor.apply(t.getValue()), t);
    }

    /**
     * A method which runs the given action on the platform thread when it
     * later becomes available.  A specific way to call Platform.runLater
     * from the platform thread when you really mean to (thread checker will warn
     * you otherwise).
     */
    @OnThread(Tag.FXPlatform)
    public static void runAfterCurrent(FXPlatformRunnable r)
    {
        // Defeat thread checker:
        ((FXPlatformConsumer<Runnable>)(Platform::runLater)).accept(r::run);
    }

    /**
     * Runs the action on the FX platform thread (after the current action,
     * if called on the platform thread).
     *
     * Be very careful using this: race hazards ahoy if you use it from an FX
     * loading thread, expecting it to nicely take place after the current code.
     * Instead, it could run alongside the currently executing code.
     *
     * @param r
     */
    @OnThread(Tag.FX)
    public static void runPlatformLater(FXPlatformRunnable r)
    {
        Platform.runLater(r::run);
    }

    /**
     * Draw stripes over a rectangle - yet another thing missing from the AWT
     */
    public static void stripeRect(GraphicsContext g, int x, int y, int width, int height, int separation, int thickness, boolean backslash, Color color)
    {
        Paint prev = g.getStroke();
        g.setStroke(color);
        g.setLineWidth(thickness);
        for (int offset = separation/2; offset < width + height; offset += separation + thickness) {
            int x1, y1, x2, y2;

            if (offset < height) {
                x1 = x;
                y1 = y + offset;
            }
            else {
                x1 = x + offset - height;
                y1 = y + height;
            }

            if (offset < width) {
                x2 = x + offset;
                y2 = y;
            }
            else {
                x2 = x + width;
                y2 = y + offset - width;
            }
            
            if (backslash)
            {
                x1 = width - x1;
                x2 = width - x2;
            }

            g.strokeLine(x1, y1, x2, y2);
        }
        g.setStroke(prev);
    }

    public static Image createImage(int width, int height, FXConsumer<GraphicsContext> draw)
    {
        Canvas c = new Canvas(width, height);
        Scene s = new Scene(new StackPane(c));
        draw.accept(c.getGraphicsContext2D());
        WritableImage image = new WritableImage(width, height);
        SnapshotParameters p = new SnapshotParameters();
        p.setFill(Color.TRANSPARENT);
        c.snapshot(p, image);
        return image;
    }
}
