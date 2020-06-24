/*
 This file is part of the BlueJ program.
 Copyright (C) 2020 Michael Kölling and John Rosenberg

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
package bluej.editor.fixes;

import bluej.editor.EditorWatcher;
import bluej.prefmgr.PrefMgr;
import bluej.utility.Utility;
import bluej.utility.javafx.FXPlatformRunnable;
import bluej.utility.javafx.JavaFXUtil;
import javafx.scene.control.Label;

import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 *  This class is shared and to be utilised by Java and Stride editors
 *  to manage fix display (basic UI elements) and fix execution.
 *  Each editors can wrap around this class and add more UI elemnts etc if required.
 */
@OnThread(Tag.FX)
public abstract class FixDisplayManager
{
    protected int highlighted = -1; // Offset into the list of FixSuggestions of an error
    protected final List<FixDisplay> fixes = new ArrayList<>();
    private int errorIdentifier;

    @OnThread(value = Tag.FX, ignoreParent = true)
    private static class FixDisplay extends HBox
    {
        private final Label enterHint = new Label("\u21B5");
        private final String displayText;
        private final FXPlatformRunnable executeFix;

        public FixDisplay(String display, FXPlatformRunnable executeFix)
        {
            this.displayText = display;
            this.executeFix = executeFix;
            enterHint.setVisible(false);
            Label l = new Label(display);
            getChildren().addAll(l, enterHint);
            HBox.setHgrow(l, Priority.ALWAYS);
            l.setMaxWidth(9999);
            setStyle(PrefMgr.getEditorFontCSS(false).get());
        }
        private void setHighlight(boolean highlight)
        {
            if (highlight)
                JavaFXUtil.addStyleClass(this, "fix-highlight");
            else
                JavaFXUtil.removeStyleClass(this, "fix-highlight");

            enterHint.setVisible(highlight);
        }

        @OnThread(Tag.Any)
        public String getDisplayText()
        {
            return displayText;
        }

        @OnThread(Tag.FXPlatform)
        protected void executeSuggestion(){
            executeFix.run();
        }
    }

    protected void prepareFixDisplay(VBox vboxContainer, List<FixSuggestion> fixSuggestions, Supplier<EditorWatcher> editorWatcherSupplier, int errorIdentifier){
        this.errorIdentifier = errorIdentifier;
        if (fixSuggestions != null)
        {
            for (FixSuggestion fix : fixSuggestions)
            {
                FixDisplay l = new FixDisplay("  \u2022 Fix: " + fix.getDescription(), () -> fix.execute());
                l.onMouseClickedProperty().set(e ->
                {
                    recordExecute(editorWatcherSupplier, fixes.indexOf(l));
                    fix.execute();
                    hide();
                    postFixError();
                    e.consume();
                });
                l.onMouseEnteredProperty().set(e -> setHighlighted(fixes.indexOf(l)));
                vboxContainer.getChildren().add(l);
                fixes.add(l);
            }
        }
        JavaFXUtil.addStyleClass(vboxContainer, "error-fix-display");
        vboxContainer.setMinWidth(250.0);
    }

    @OnThread(Tag.FXPlatform)
    protected void executeSelectedFix()
    {
        fixes.get(highlighted).executeSuggestion();
    }

    @OnThread(Tag.FXPlatform)
    protected void recordShow(Supplier<EditorWatcher> editorWatcherProvider)
    {
        final EditorWatcher watcher = editorWatcherProvider.get();
        List<String> fixDisplayText = Utility.mapList(fixes, FixDisplay::getDisplayText);
        watcher.recordShowErrorMessage(errorIdentifier, fixDisplayText);
    }

    @OnThread(Tag.FXPlatform)
    protected void recordExecute(Supplier<EditorWatcher> editorWatcherSupplier, int fixIndex)
    {
        final EditorWatcher watcher = editorWatcherSupplier.get();
        watcher.recordFix(errorIdentifier, fixIndex);
    }

    protected abstract void hide();

    protected abstract void postFixError();

    protected  void setHighlighted(int newHighlight)
    {
        if (highlighted != newHighlight)
        {
            // Remove highlight from old item:
            if (highlighted != -1)
                fixes.get(highlighted).setHighlight(false);
            highlighted = newHighlight;
            // Highlight new item:
            if (highlighted != -1)
                fixes.get(highlighted).setHighlight(true);
        }
    }

    public boolean hasFixes()
    {
        return !fixes.isEmpty();
    }

    /**
     * Called when down is pressed on the slot we are joined to.
     * Only call this if hasFixes() returns true, otherwise handle keypress differently.
     */
    public void down()
    {
        setHighlighted(Math.min(fixes.size() - 1, highlighted + 1));
    }

    /**
     * Called when up is pressed on the slot we are joined to.
     * Only call this if hasFixes() returns true, otherwise handle keypress differently.
     */
    public void up()
    {
        setHighlighted(Math.max(0, highlighted - 1));
    }

}
