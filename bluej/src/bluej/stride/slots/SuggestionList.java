/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015 Michael Kölling and John Rosenberg 
 
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
package bluej.stride.slots;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.CssMetaData;
import javafx.css.SimpleStyleableDoubleProperty;
import javafx.css.Styleable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.effect.BlendMode;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.web.WebView;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Duration;
import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.Config;
import bluej.stride.generic.InteractionManager;
import bluej.utility.Utility;
import bluej.utility.javafx.FXRunnable;
import bluej.utility.javafx.FXSupplier;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.ScalableHeightLabel;


/**
 * A code completion suggestion list that pops up beneath a slot to offer suggestions
 * for the user to pick from, using either the arrow keys, the mouse, or by typing more and pressing enter.
 *
 * Suggestions can either be "direct", meaning that what the user has typed so far is
 * (ignoring case) an exact prefix of the suggestion, or they can be "similar" meaning
 * that what the user has typed is (ignoring case) within MAX_EDIT_DISTANCE of one
 * chunk of the suggestion.  Here, chunk means a part of the suggestion that begins
 * after an underscore (e.g. actor_details in "get_actor_details") or a case change
 * (e.g. ActorDetails in "getActorDetails").
 * 
 *  Direct suggestions are always shown first, and similar suggestions are shown beneath
 *  a suitable label.
 */
public class SuggestionList
{
    private final SuggestionListListener listener;

    private static class SuggestionVBox extends VBox
    {
        private final SimpleStyleableDoubleProperty cssTypeWidthProperty = new SimpleStyleableDoubleProperty(TYPE_WIDTH_META_DATA);
        public final SimpleStyleableDoubleProperty cssTypeWidthProperty() { return cssTypeWidthProperty; }
        private final SimpleStyleableDoubleProperty cssMaxWidthProperty = new SimpleStyleableDoubleProperty(MAX_WIDTH_META_DATA);
        public final SimpleStyleableDoubleProperty cssMaxWidthProperty() { return cssMaxWidthProperty; }

        private static final CssMetaData<SuggestionVBox, Number> TYPE_WIDTH_META_DATA =
                JavaFXUtil.cssSize("-bj-type-width", SuggestionVBox::cssTypeWidthProperty);
        private static final CssMetaData<SuggestionVBox, Number> MAX_WIDTH_META_DATA =
                JavaFXUtil.cssSize("-bj-max-width", SuggestionVBox::cssMaxWidthProperty);

        private static final List <CssMetaData <? extends Styleable, ? > > cssMetaDataList =
                JavaFXUtil.extendCss(VBox.getClassCssMetaData())
                  .add(TYPE_WIDTH_META_DATA)
                  .add(MAX_WIDTH_META_DATA)
                  .build();

        public static List <CssMetaData <? extends Styleable, ? > > getClassCssMetaData() { return cssMetaDataList; }
        @Override public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() { return getClassCssMetaData(); }
    }

    /**
     * Maximum edit distance: deliberately tight to avoid too many far-out suggestions
     * showing up in the "Similar" list.
     */
    private static final int MAX_EDIT_DISTANCE = 1;
    
    /**
     * Element containing all the suggestion items:
     */
    private final SuggestionVBox listBox;
    
    /**
     * Scroll pane containing listBox.
     */
    private final ScrollPane pane;
    
    /**
     * The window containing the pane
     */
    private final Stage window;
    
    /**
     * List of strings to be matched with.  Note that this is not necessarily what is
     * displayed to the user, as the user's display may have added information
     * like a type before the completion, or parameter types afterwards.
     */
    private final List<SuggestionDetails> choices;
    /**
     * This array contains two entries per choice.  There is one complete set from
     * 0 to choices.size() - 1 which are the direct suggestions, and a second
     * set from choices.size() to (2 * choices.size() - 1) which are the similar
     * suggestions.  We make sure that a suggestion is never shown twice: either
     * its direct suggestion or its similar suggestion, or neither, but never both.
     */
    private final List<Suggestion> doubleSuggestions = new ArrayList<>();
    /**
     * Current eligible completions, map from index in doubleSuggestions to details
     * about the completion.  As noted above, no choice will appear twice; it will either
     * appear once as direct, once as similar, or not at all.
     */
    private final HashMap<Integer, EligibleDetail> eligible = new HashMap<>();
    /**
     * The current highlighted item; -1 for no highlight, or otherwise an index
     * between 0 and doubleSuggestions.size() - 1 (if it is < choices.size(), it must
     * be a direct suggestion, it if is >= choices.size() it must be a similar suggestion).
     */
    private int highlighted = -1;
    /**
     * A listener that needs to be updated when the highlighted suggestion changes.
     * Integer is index into choices, not doubleSuggestions.
     */
    private final Consumer<Integer> highlightListener;
    /**
     * The label that divides the direct and similar suggestions.  Only displays when
     * there are actual similar suggestions to show
     */
    private final ScalableHeightLabel similarLabel;
    /**
     * The label for when there are no eligible suggestions.  Only displays when
     * there no eligible suggestions;
     */
    private final ScalableHeightLabel noneLabel;
    /**
     * The width of the type labels that appear to the left of the suggestions.
     * Can be zero, when you don't want to show any types.
     */
    private final DoubleExpression typeWidth;

    /** Used when "replaying" last calculateEligible call */
    private String lastPrefix;
    /** Used when "replaying" last calculateEligible call */
    private boolean lastAllowSimilar;

    private boolean expectingToLoseFocus = false;

    private ObjectProperty<SuggestionShown> shownState = new SimpleObjectProperty<>(SuggestionShown.COMMON);
    
    private FXRunnable cancelShowDocsTask;
    private Pane docPane;

    private boolean hiding = false;

    private final BooleanProperty moreLabelAtBottom = new SimpleBooleanProperty(true);

    private static class EligibleDetail implements Comparable<EligibleDetail>
    {
        // The offset into the suggestion string of the matching part
        public final int suggestionOffset;
        // The edit distance between the matching part and what the user has typed.
        public final int distance;
        // The length of what the user has typed.
        private final int length;

        public EligibleDetail(int suggestionOffset, int distance, int length)
        {
            this.suggestionOffset = suggestionOffset;
            this.distance = distance;
            this.length = length;
        }

        @OnThread(value = Tag.FX, ignoreParent = true)
        public int compareTo(EligibleDetail e)
        {
            // If one matches at the start, that is definitely better than one
            // that matches later on, even if it has higher edit distance.
            // Compare whether one of us and "e" matches at start and other doesn't:
            if ((suggestionOffset == 0) == (e.suggestionOffset == 0))
            {
                // Either both at start or neither; compare edit distance
                return Integer.compare(distance, e.distance);
            }
            else
            {
                if (suggestionOffset == 0)
                    return -1; // We start at the beginning and are the better one
                else
                    return 1; // Must be them
            }
        }

        public boolean close()
        {
            if (distance == 0 && suggestionOffset == 0)
                return true; // Always show direct suggestions
            if (distance == 0 && suggestionOffset != 0)
                return length >= 2; // Only show substring matches after two characters
            if (distance == 1)
                return length >= 3; // Only show typo matches after three characters
            if (distance == 2)
                return length >= 10; // Show further matches, but only if you are typing a long identifier
            return false; // distance 3 or higher; Too far away
        }
    }

    // Whether the suggestion is common (shown from first trigger) or rare (shown only on second trigger)
    public static enum SuggestionShown
    {
        COMMON, RARE;
    }

    public static class SuggestionDetails
    {
        // The string of the choices to match the user's input against.  Cannot be null.
        public final String choice;
        // Non-matchable bit displayed at the end of a suggestion, e.g. param types.  Can be null.
        public final String suffix;
        // A type to be displayed before the suggestion.  Can be null.
        public final String type;
        // Whether the suggestion is common (shown from first trigger) or rare (shown only on second trigger)
        public final SuggestionShown shown;

        public SuggestionDetails(String choice)
        {
            this(choice, null, null, SuggestionShown.COMMON);
        }

        public SuggestionDetails(String choice, String suffix, String type, SuggestionShown shown)
        {
            if (choice == null)
                throw new IllegalArgumentException();
            if (shown == null)
                throw new IllegalArgumentException();
            this.choice = choice;
            this.suffix = suffix;
            this.type = type;
            this.shown = shown;
        }

        public boolean hasDocs()
        {
            return false;
        }

        public Pane makeDocPane()
        {
            return null;
        }
    }

    public static class SuggestionDetailsWithHTMLDoc extends SuggestionDetails
    {
        private final String docHTML;

        public SuggestionDetailsWithHTMLDoc(String choice, SuggestionShown shown, String docHTML)
        {
            super(choice, null, null, shown);
            this.docHTML = docHTML;
        }

        public SuggestionDetailsWithHTMLDoc(String choice, String suffix, String type, SuggestionShown shown, String docHTML)
        {
            super(choice, suffix, type, shown);
            this.docHTML = docHTML;
        }

        public boolean hasDocs()
        {
            return true;
        }

        public Pane makeDocPane()
        {
            WebView webView = new WebView();
            Pane docDisplay = new BorderPane(webView);
            JavaFXUtil.addStyleClass(docDisplay, "suggestion-javadoc");
            webView.getEngine().setJavaScriptEnabled(false);
            webView.getEngine().loadContent(docHTML);

            docDisplay.setMaxWidth(400);
            docDisplay.setMaxHeight(300);
            // Workaround to get transparent background, from:
            // http://stackoverflow.com/questions/12421250/transparent-background-in-the-webview-in-javafx
            webView.setBlendMode(BlendMode.DARKEN);
            return docDisplay;
        }
    }

    public static class SuggestionDetailsWithCustomDoc extends SuggestionDetails
    {
        private final FXSupplier<Pane> docMaker;

        public SuggestionDetailsWithCustomDoc(String choice, String suffix, String type, SuggestionShown shown, FXSupplier<Pane> docMaker)
        {
            super(choice, suffix, type, shown);
            this.docMaker = docMaker;
        }

        public boolean hasDocs()
        {
            return true;
        }

        public Pane makeDocPane()
        {
            return docMaker.get();
        }
    }

    /**
     * Create a SuggestionList.
     * 
     * @param editor Editor (used to get overlay panes)
     * @param choices The strings of the choices to match the user's input against
     * @param suffixes Non-matchable bits on the end of a suggestion, e.g. param types.
     *                 Should either be null or same length as choices, one suffix per choice. 
     * @param types Should either be null, or the same length as choices, one type per choice
     * @param highlightListener A listener for the highlighted item changing.  Can be null.
     * @param clickListener A listener for a choice being clicked (and thus selected).  Cannot be null.
     */
    public SuggestionList(InteractionManager editor, List<? extends SuggestionDetails> choices, String targetType,
                          SuggestionShown startShown, Consumer<Integer> highlightListener, final SuggestionListListener listener)
    {
        if (listener == null)
            throw new IllegalArgumentException("SuggestionListListener cannot be null");
        
        this.choices = new ArrayList<>(choices);
        this.shownState.set(startShown);
        this.listener = listener;
        this.highlightListener = highlightListener;
        this.similarLabel = new ScalableHeightLabel("Related:", false);
        similarLabel.setMaxWidth(9999);
        this.noneLabel = new ScalableHeightLabel("No completions", false);
        noneLabel.setMaxWidth(9999);
        JavaFXUtil.addStyleClass(similarLabel, "suggestion-similar-heading");
        JavaFXUtil.addStyleClass(noneLabel, "suggestion-none");
        this.listBox = new SuggestionVBox();
        JavaFXUtil.addStyleClass(listBox, "suggestion-list");
        this.typeWidth = choices.stream().allMatch(s -> s.type == null) ? new ReadOnlyDoubleWrapper(0.0) : listBox.cssTypeWidthProperty();

        listBox.setBackground(null);
        listBox.setFillWidth(true);
        
        this.pane = new ScrollPane(listBox);
        pane.setFitToWidth(true);
        pane.setBackground(null);
        JavaFXUtil.addStyleClass(pane, "suggestion-list-scroll-pane");
        pane.setStyle("-fx-font-size: " + editor.getFontSizeCSS().get() + ";");
        pane.maxWidthProperty().bind(listBox.cssMaxWidthProperty());
        pane.setBackground(new Background(new BackgroundFill(Color.BLACK, null, null)));
        
        pane.setHbarPolicy(ScrollBarPolicy.NEVER);
        
        this.docPane = new Pane();
        docPane.setMinWidth(400.0);
        docPane.setMaxHeight(300.0);
        docPane.setBackground(null);
        docPane.setMouseTransparent(true);
        
        BorderPane listAndDocBorderPane = new BorderPane();
        JavaFXUtil.addStyleClass(listAndDocBorderPane, "suggestion-top-level");
        
        // We wrap left-hand part in an AnchorPane to allow any unused space beneath the list
        // to be transparent:
        AnchorPane listAndMoreAndTransPane = new AnchorPane();
        listAndMoreAndTransPane.setBackground(null);
        listAndMoreAndTransPane.setPickOnBounds(false);
        pane.setMaxHeight(300.0);
        listAndDocBorderPane.setCenter(listAndMoreAndTransPane);
        listAndDocBorderPane.setRight(docPane);
        listAndDocBorderPane.setMaxHeight(300.0);
        Label moreLabel = new Label("Showing common options. Press Ctrl+Space again to see all options");
        JavaFXUtil.addStyleClass(moreLabel, "suggestion-more-label");

        listAndDocBorderPane.setBackground(null);
        listAndDocBorderPane.setPickOnBounds(false);

        AnchorPane moreLabelPane = new AnchorPane(moreLabel);
        moreLabel.setMaxWidth(300.0);
        AnchorPane.setLeftAnchor(moreLabel, 0.0);
        AnchorPane.setTopAnchor(moreLabel, 0.0);
        AnchorPane.setBottomAnchor(moreLabel, 0.0);
        JavaFXUtil.addStyleClass(moreLabelPane, "suggestion-more-label-pane");
        
        window = new Stage(StageStyle.TRANSPARENT);
        window.setResizable(false);
        BorderPane listAndMorePane = new BorderPane();
        JavaFXUtil.addStyleClass(listAndMorePane, "suggestion-dialog-lhs");
        listAndMorePane.setCenter(pane);
        if (shownState.get() == SuggestionShown.COMMON)
            listAndMorePane.setBottom(moreLabelPane);
        listAndMoreAndTransPane.getChildren().add(listAndMorePane);
        AnchorPane.setLeftAnchor(listAndMorePane, 0.0);
        AnchorPane.setRightAnchor(listAndMorePane, 0.0);
        AnchorPane.setTopAnchor(listAndMorePane, 0.0);
        JavaFXUtil.addChangeListener(moreLabelAtBottom, atBottom -> {
            if (atBottom)
            {
                listAndMorePane.setTop(null);
                listAndMorePane.setBottom(shownState.get() == SuggestionShown.COMMON ? moreLabelPane : null);
                AnchorPane.setTopAnchor(listAndMorePane, 0.0);
                AnchorPane.setBottomAnchor(listAndMorePane, null);
                BorderPane.setAlignment(docPane, Pos.TOP_LEFT);
            }
            else
            {
                listAndMorePane.setBottom(null);
                listAndMorePane.setTop(shownState.get() == SuggestionShown.COMMON ? moreLabelPane : null);
                AnchorPane.setTopAnchor(listAndMorePane, null);
                AnchorPane.setBottomAnchor(listAndMorePane, 0.0);
                BorderPane.setAlignment(docPane, Pos.BOTTOM_LEFT);
            }
            JavaFXUtil.setPseudoclass("bj-at-top", !atBottom, moreLabelPane);
        });
        JavaFXUtil.addChangeListener(shownState, s -> {
            if (s == SuggestionShown.RARE)
            {
                listAndMorePane.setTop(null);
                listAndMorePane.setBottom(null);
            }
        });

        Scene scene = new Scene(listAndDocBorderPane);
        window.setHeight(350.0);
        scene.setFill(null);
        Config.addEditorStylesheets(scene);
        window.setScene(scene);

        editor.setupSuggestionWindow(window);
        
        for (int j = 0; j <= 1; j++)
        {
            for (int i = 0; i < choices.size(); i++)
            {
                final int index = i + j*choices.size();
                SuggestionDetails choice = choices.get(i);
                String display = choice.choice + (choice.suffix == null ? "" : choice.suffix);
                Suggestion sugg = new Suggestion(choice.choice, choice.suffix == null ? "" : choice.suffix, choice.type == null ? "" : choice.type, targetType != null && choice.type != null ? targetType.equals(choice.type) : false, typeWidth, j == 0);
                listBox.getChildren().add(sugg.getNode());
                sugg.getNode().setOnMouseClicked(e -> {
                    highlighted = index;
                    listener.suggestionListChoiceClicked(getHighlighted());
                    expectingToLoseFocus = true;
                    hiding = true;
                    window.hide();
                    listener.hidden();
                });

                doubleSuggestions.add(sugg);
            }
            if (j == 0)
            {
                listBox.getChildren().add(similarLabel);
            }
            else
            {
                listBox.getChildren().add(noneLabel);
            }
        }

        JavaFXUtil.addChangeListener(window.focusedProperty(), focused -> {
            if (!focused)
            {
                hideDocDisplay();
                hiding = true;
                // We must hide the window during a runLater.  If we hide it straight away
                // during the focus switching, odd effects happen where the focus returns
                // to the triggering slot, and the click event gets lost:
                Platform.runLater(() -> {
                    window.hide();
                    if (!expectingToLoseFocus)
                        listener.suggestionListFocusStolen(getHighlighted());
                    listener.hidden();
                });
            }
        });
        
        // On Mac, we have to check for Ctrl-Space in KEY_PRESSED, not KEY_TYPED:
        pane.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.SPACE && e.isControlDown())
            {
                // Consume either way, because otherwise we get an invalid character:
                e.consume();

                if (shownState.get() == SuggestionShown.COMMON)
                {
                    shownState.set(SuggestionShown.RARE);
                    calculateEligible(lastPrefix, lastAllowSimilar, false);
                    updateVisual(lastPrefix, false);
                }
            }
        });

        pane.addEventFilter(KeyEvent.KEY_TYPED, e -> {
            if (e.getCharacter().equals(" ") && e.isControlDown())
            {
                if (shownState.get() == SuggestionShown.COMMON)
                {
                    shownState.set(SuggestionShown.RARE);
                    calculateEligible(lastPrefix, lastAllowSimilar, false);
                    updateVisual(lastPrefix, false);
                }
            }
            // On Mac, some keypresses can produce null character, so guard against that:
            else if (!e.getCharacter().contains("\u0000") && listener.suggestionListKeyTyped(e, getHighlighted()) == SuggestionListListener.Response.DISMISS)
            {
                expectingToLoseFocus = true;
                hiding = true;
                window.hide();
                listener.hidden();
            }
            e.consume();
        });
        pane.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            switch (e.getCode())
            {
                case UP:
                    up();
                    break;
                case DOWN:
                    down();
                    break;
                case PAGE_UP:
                    pageUp();
                    break;
                case PAGE_DOWN:
                    pageDown();
                    break;
                case HOME:
                    home();
                    break;
                case END:
                    end();
                    break;
                default:
                    int selected = getHighlighted();
                    if (selected == -1 && eligibleCount() == 1)
                        selected = getFirstEligible();
                    if (listener.suggestionListKeyPressed(e, selected) == SuggestionListListener.Response.DISMISS)
                    {
                        expectingToLoseFocus = true;
                        hiding = true;
                        window.hide();
                        listener.hidden();
                    }
                    break;
            }
            e.consume();
        });
    }
    
    public void show(final Node reference, final DoubleExpression xOffset, final DoubleExpression yOffset)
    {
        // If there's only one option, don't bother showing, just choose it right off the bat:
        if (eligibleCount() == 1)
        {
            boolean singleOptionAvailable = true;
            // We need to make sure there are no rare options available:
            if (shownState.get() == SuggestionShown.COMMON)
            {
                calculateEligible(lastPrefix, lastAllowSimilar, SuggestionShown.RARE, false);
                if (eligibleCount() != 1)
                {
                    //There were more options: set it back
                    calculateEligible(lastPrefix, lastAllowSimilar, SuggestionShown.COMMON, false);
                    singleOptionAvailable = false;
                }
                // Otherwise, still only one option available, no need to set it back as we won't show anyway
            }

            if (singleOptionAvailable)
            {
                int choice = getFirstEligible();
                // runLater because our caller won't expect us to call back before show has finished:
                Platform.runLater(() -> {
                    listener.hidden();
                    listener.suggestionListChoiceClicked(choice);
                });
                return;
            }
        }

        // Make sure CSS has taken effect, so typeWidth is valid:
        window.getScene().getRoot().applyCss();

        double xPos = reference.localToScene(reference.getBoundsInLocal()).getMinX();

        Window refWindow = reference.getScene().getWindow();
        // Work out most restrictive Y coordinate on all the screens our editor window is overlapping:
        double screenMaxY = Screen.getScreensForRectangle(refWindow.getX(), refWindow.getY(), refWindow.getWidth(), refWindow.getHeight())
            .stream().mapToDouble(s -> s.getVisualBounds().getMaxY()).min().orElse(999999.0);

        //editor.getCodeOverlayPane().addOverlay(pane, reference, actualXOffset, yOffset);
        double windowX = refWindow.getX() + reference.getScene().getX() + xPos + xOffset.get() - typeWidth.get() - 1.0f; //1 pixel for border
        double windowY = refWindow.getY() + reference.getScene().getY() + reference.localToScene(reference.getBoundsInLocal()).getMinY() + yOffset.get();
        if (screenMaxY < window.getHeight() + windowY)
        {
            // Bottom of suggestions window will be off screen; we must place window above reference, not below
            windowY = refWindow.getY() + reference.getScene().getY() + reference.localToScene(reference.getBoundsInLocal()).getMinY() - 350.0;
            moreLabelAtBottom.set(false);
        }
        else
        {
            moreLabelAtBottom.set(true);
        }
        window.setX(windowX);
        window.setY(windowY);
        if (window.getOwner() == null)
            window.initOwner(reference.getScene().getWindow());
        window.show();
        //ScenicView.show(window.getScene());
        pane.requestFocus();
    }
    
    private void up()
    {
        // Go backwards through eligible completions to find previous eligible.
        // Pressing UP on top item deselects all completions
        for (int candidate = highlighted - 1; candidate >= -1; candidate--)
        {
            if (candidate == -1 || eligible.containsKey(candidate))
            {
                setHighlighted(candidate, true);
                break;
            }
        }
    }
    
    private void down()
    {
        // This works if highlighted is -1, too; advance to 0:
        for (int candidate = highlighted + 1; candidate < doubleSuggestions.size(); candidate++)
        {
            if (eligible.containsKey(candidate))
            {
                setHighlighted(candidate, true);
                break;
            }
        }
    }

    private void home()
    {
        setHighlighted(getFirstEligible(), true);
    }

    private void end()
    {
        setHighlighted(getLastEligible(), true);
    }

    private void pageUp()
    {
        double height = doubleSuggestions.get(getFirstEligible()).getNode().getHeight();
        int itemsPerWindow = (int)Math.floor(pane.getHeight() / height);

        for (int i = 0; i < itemsPerWindow; i++)
            up();

        // But don't leave nothing selected:
        if (highlighted == -1)
            home();
    }

    private void pageDown()
    {
        double height = doubleSuggestions.get(getFirstEligible()).getNode().getHeight();
        int itemsPerWindow = (int)Math.floor(pane.getHeight() / height);

        for (int i = 0; i < itemsPerWindow; i++)
            down();
    }


    
    protected void setHighlighted(int newHighlight, boolean scrollTo)
    {
        if (highlighted == newHighlight)
            return;
        
        if (highlighted != -1)
            doubleSuggestions.get(highlighted).setHighlight(false);
        
        highlighted = newHighlight;

        if (highlighted != -1)
        {
            doubleSuggestions.get(highlighted).setHighlight(true);
            
            // Scroll to show the item:
            if (scrollTo)
            {
                double before = 0, after = 0;
                for (int n : eligible.keySet())
                {
                    if (n < highlighted)
                        before += 1;
                    else if (n > highlighted)
                        after += 1;
                }
                pane.setVvalue(Math.max(0.0, (before / (before + after))  ));
            }
        }
        
        if (highlightListener != null)
            highlightListener.accept(getHighlighted());
        
        showDocsFor(getHighlighted());
    }

    public void calculateEligible(String prefix, boolean allowSimilar, boolean canChangeToRare)
    {
        calculateEligible(prefix, allowSimilar, shownState.get(), canChangeToRare);
    }
    
    /**
     * Calculates the eligible choices (those that begin with the given prefix).
     * Does not actually do any graphical update; for that, call updateVisual
     * @param prefix The current prefix that the user has typed
     * @param allowSimilar Whether to allow similar items (correct for typos)
     * @param canChangeToRare If we are showing common and there are no suggestions, can we switch to rare?
     */
    public void calculateEligible(String prefix, boolean allowSimilar, SuggestionShown shown, boolean canChangeToRare)
    {
        lastPrefix = prefix;
        lastAllowSimilar = allowSimilar;
        eligible.clear();
        for (int i = 0; i < choices.size(); i++)
        {
            String sugg = choices.get(i).choice;
            if (choices.get(i).shown.compareTo(shown) > 0)
            {
                // Cannot put in eligible because it is rare and we are only showing common
            }
            else if (sugg.toLowerCase().startsWith(prefix.toLowerCase()))
            {
                eligible.put(i, new EligibleDetail(0, 0, prefix.length()));
            }
            else if (allowSimilar)
            {
                // Look if this text starts a word in the identifier:
                List<Integer> wordStarts = splitIdentLower(sugg);
                Optional<EligibleDetail> me = wordStarts.stream().map(j -> new EligibleDetail(j, distanceTo(prefix, sugg, j), prefix.length()))
                    .filter(EligibleDetail::close)
                    .sorted() // Will put smallest distance first
                    .findFirst();
                if (me.isPresent())
                {
                    eligible.put(i + doubleSuggestions.size() / 2, me.get());
                }
            }
        }

        if (eligible.isEmpty() && shown == SuggestionShown.COMMON && canChangeToRare)
        {
            shownState.set(SuggestionShown.RARE);
            // Go round again:
            calculateEligible(prefix, allowSimilar, SuggestionShown.RARE, false);
        }
    }
    
    private static int distanceTo(String prefix, String candidate, int offset)
    {
        // We check, given a prefix (e.g. "abc"), whether the substring of the same length (e.g. 3)
        // at the given point in the candidate is a closen enough match by edit distance
        // An exact match is edit distance 0
        prefix = prefix.toLowerCase();
        String partialLower = candidate.substring(offset, Math.min(candidate.length(), offset + prefix.length())).toLowerCase();
        
        // We also check for the strings one longer and one shorter, as they might have better edit distance:
        String partialLowerShort = candidate.substring(offset, Math.min(candidate.length(), offset + Math.max(1, prefix.length() - 1))).toLowerCase();
        String partialLowerLong = candidate.substring(offset, Math.min(candidate.length(), offset + 1 + prefix.length())).toLowerCase();
        
        return Math.min(
                Utility.editDistance(partialLower, prefix),
                Math.min(Utility.editDistance(partialLowerShort, prefix), Utility.editDistance(partialLowerLong, prefix))
               );
    }
    
    private static boolean hasCase(char c)
    {
        // It has case if one of these methods returns differently to the other:
        return Character.isUpperCase(c) != Character.isLowerCase(c);
    }

    private static List<Integer> splitIdentLower(String text)
    {
        int startCurWord = 0;
        List<Integer> r = new ArrayList<>();
        // We split on a change of case, or an underscore, or a dot (e.g. in Greenfoot.isKeyDown)
        for (int i = 1 /* start at 2nd char */; i < text.length(); i++)
        {
            if ((hasCase(text.charAt(i)) && hasCase(text.charAt(i - 1))) && 
               (Character.isUpperCase(text.charAt(i)) == Character.isLowerCase(text.charAt(i - 1))
             || Character.isLowerCase(text.charAt(i)) == Character.isUpperCase(text.charAt(i - 1)))
               && (startCurWord == 0 || i - startCurWord > 1))
            {
                // Case change:
                r.add(startCurWord);
                startCurWord = i;
            }
            else if ((text.charAt(i) == '_' || text.charAt(i) == '.') && startCurWord < i - 1)
            {
                r.add(startCurWord);
                startCurWord = i + 1; 
            }
        }
        r.add(startCurWord);
        return r;
    }

    /**
     * Updates the available options in the dropdown, restricting it to those
     * that are currently marked as eligible.  Thus this function only has a useful effect
     * if you call calculateEligible first.
     * 
     * @param immediate Whether to make the change immediately (true) or animate it (false)
     */
    public void updateVisual(String prefix, boolean immediate)
    {        
        boolean showingAny = false;
        boolean showingAnySimilar = false;
        
        // Fade in and out the suggestions (calling animate out on already animated out is fine, same for in)
        for (int i = 0; i < doubleSuggestions.size(); i++)
        {
            if (eligible.containsKey(i))
            {
                doubleSuggestions.get(i).animateIn(immediate);
                if (i > doubleSuggestions.size() / 2)
                    showingAnySimilar = true;
                showingAny = true;
            }
            else
                doubleSuggestions.get(i).animateOut(immediate);
        }
        
        if (showingAnySimilar)
        {
            if (immediate)
                similarLabel.setToFullHeight();
            else
                similarLabel.getGrowToFullHeightTimeline(Suggestion.FADE_IN_SPEED).play();
        }
        else
        {
            if (immediate)
                similarLabel.setToNothing();
            else
                similarLabel.getShrinkToNothingTimeline(Suggestion.FADE_OUT_SPEED).play();
        }
        
        if (!showingAny)
        {
            if (immediate)
                noneLabel.setToFullHeight();
            else
                noneLabel.getGrowToFullHeightTimeline(Suggestion.FADE_IN_SPEED).play();
        }
        else
        {
            if (immediate)
                noneLabel.setToNothing();
            else
                noneLabel.getShrinkToNothingTimeline(Suggestion.FADE_OUT_SPEED).play();
        }

        // Now we must update those that are showing, to update their completion hints and so on:
        for (Entry<Integer, EligibleDetail> e : eligible.entrySet())
        {
            // It's available to complete if either:
            // - it's the only available completion, or
            // - it's the highlighted item in the dropdown, or
            // - it's exactly equal to the current string
            // Note that 3 does not necessarily imply 1, if one completion is a prefix of another, e.g. "foo" and "fooBar".
            //  In the case that "foo" and "fooBar" are available completions:
            //  - If you type "fo", then both "foo" and "fooBar" are available, neither can complete (if neither are highlighted)
            //  - If you type "foo", then "foo" is available to complete by pressing enter
            //  - If you type "fooB" then "fooB" is available to complete if and only if no other options begin with "fooB". 
            boolean canComplete = eligible.size() == 1
                                  || highlighted == e.getKey()
                                  || doubleSuggestions.get(e.getKey()).getText().equals(prefix);
            doubleSuggestions.get(e.getKey()).notifyEligible(e.getValue().suggestionOffset, prefix.length(), canComplete, immediate);
        }
        
        // If we had a highlight before, but it's no longer eligible, remove the highlight:
        if (highlighted != -1 && !eligible.containsKey(highlighted))
        {
            setHighlighted(getFirstEligible(), true);
        }
    }
    
    public int eligibleCount()
    {
        return eligible.size();
    }
    
    public int getFirstEligible()
    {
        return eligible.keySet().stream().mapToInt(i -> i).min().orElse(-1);
    }

    public int getLastEligible()
    {
        return eligible.keySet().stream().mapToInt(i -> i).max().orElse(-1);
    }
    
    public void highlightFirstEligible()
    {
        setHighlighted(getFirstEligible(), false);
    }
    
    // Returns an index into suggestions
    
    private int getHighlighted()
    {
        return choices.size() == 0 ? -1 : highlighted % choices.size();
    }
    
    public static interface SuggestionListListener
    {
        void suggestionListChoiceClicked(int highlighted);

        Response suggestionListKeyTyped(KeyEvent event, int highlighted);

        // Note: UP, DOWN are automatically handled, but not ESCAPE, ENTER, etc
        Response suggestionListKeyPressed(KeyEvent event, int highlighted);

        // Called when focus was lost and we are hiding, but not because choiceClicked() or keyTyped returned DISMISS
        default void suggestionListFocusStolen(int highlighted) { };

        default void hidden() { };

        public static enum Response
        {
            DISMISS, CONTINUE;
        }
    }

    public Optional<String> getLongestCommonPrefix()
    {
        // We look for the longest prefix of all available *direct* suggestions:
        return longestCommonPrefix(eligible.entrySet().stream()
                  .filter(e -> e.getKey() < choices.size()) // direct only
                  .map(e -> choices.get(e.getKey()).choice)
                  .collect(Collectors.toList()));
    }

    private static Optional<String> longestCommonPrefix(List<String> srcs)
    {
        return srcs.stream().reduce((a, b) -> {
            int maxLen = Math.min(a.length(), b.length());

            if (maxLen == 0) return "";

            for (int i = 0; i < maxLen; i++)
            {
                if (a.charAt(i) != b.charAt(i))
                {
                    return a.substring(0, i);
                }
            }

            // Must match up to maxLen:
            return a.substring(0, maxLen);

        }); // Otherwise empty if no suggestions available
        
    }

    public DoubleExpression widthProperty()
    {
        return pane.widthProperty();
    }

    public DoubleExpression typeWidthProperty()
    {
        return typeWidth;
    }

    public boolean isShowing()
    {
        return window.isShowing();
    }
    
    public boolean isInMiddleOfHiding()
    {
        return hiding ;
    }
    
    private void showDocsFor(int selected)
    {
        if (cancelShowDocsTask != null)
        {
            cancelShowDocsTask.run();
            cancelShowDocsTask = null;
        }
        hideDocDisplay();
        if (selected != -1 && choices.get(selected).hasDocs())
        {
            // Schedule the docs to show after a delay, assuming they don't move before then:
            cancelShowDocsTask = JavaFXUtil.runAfter(Duration.millis(500), () -> {
                docPane.getChildren().setAll(choices.get(selected).makeDocPane());
            });
        }
    }

    private void hideDocDisplay()
    {
        docPane.getChildren().clear();
    }
}
