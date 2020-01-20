/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016,2019,2020 Michael Kölling and John Rosenberg
 
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

import javafx.application.Platform;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

import bluej.utility.javafx.FXPlatformConsumer;
import bluej.utility.javafx.JavaFXUtil;
import threadchecker.OnThread;
import threadchecker.Tag;

class SuggestionCell extends ListCell<SuggestionList.SuggestionListItem> implements ChangeListener<Object>
{
    private final Label special;
    private final Label type;
    private final Label prefix;
    private final Label matching;
    private final Label next; // next char to input
    private final Label suffix;
    private final Label fixedPostSuffix;
    private final BorderPane pane;
    private final HBox hbox;

    public SuggestionCell(DoubleExpression typeWidth, FXPlatformConsumer<SuggestionList.SuggestionListItem> clickListener)
    {
        this.special = new Label();
        this.type = new Label();
        this.type.minWidthProperty().bind(typeWidth);
        this.type.maxWidthProperty().bind(typeWidth);
        this.type.setEllipsisString("\u2026");
        this.prefix = new Label();
        this.matching = new Label();
        this.next = new Label();
        this.suffix = new Label();
        this.fixedPostSuffix = new Label();

        JavaFXUtil.addStyleClass(this, "suggestion-cell");
        JavaFXUtil.addStyleClass(this.type, "suggestion-type");
        JavaFXUtil.addStyleClass(prefix, "suggestion-prefix");
        JavaFXUtil.addStyleClass(matching, "suggestion-matching");
        JavaFXUtil.addStyleClass(next, "suggestion-next");
        JavaFXUtil.addStyleClass(suffix, "suggestion-suffix");
        prefix.setMinWidth(Region.USE_PREF_SIZE);
        matching.setMinWidth(Region.USE_PREF_SIZE);
        next.setMinWidth(Region.USE_PREF_SIZE);
        // Suffix will be abbreviated if we run out of space:
        suffix.setMinWidth(0.0);
        fixedPostSuffix.setMinWidth(0.0);
        
        special.setMaxWidth(9999.0);
        special.setText("Related:");
        JavaFXUtil.addStyleClass(special, "suggestion-similar-heading");

        hbox = new HBox();
        hbox.getChildren().addAll(this.type, prefix, matching, next, suffix, fixedPostSuffix);
        hbox.setSpacing(0);
        
        // By using a BorderPane, buttonHint will always appear,
        // and hbox will shrink if needed (shrinking suffix)
        pane = new BorderPane();
        pane.setCenter(hbox);
        JavaFXUtil.addStyleClass(pane, "suggestion");

        pane.setOnMouseClicked(e -> clickListener.accept(itemProperty().get()));

        itemProperty().addListener((_obs, oldItem, item) -> {
            if (oldItem != null)
            {
                oldItem.eligibleAt.removeListener(this);
                oldItem.eligibleLength.removeListener(this);
                oldItem.eligibleCanTab.removeListener(this);
                oldItem.highlighted.removeListener(this);
            }

            JavaFXUtil.runNowOrLater(() -> update(item));

            if (item != null)
            {
                item.eligibleAt.addListener(this);
                item.eligibleLength.addListener(this);
                item.eligibleCanTab.addListener(this);
                item.highlighted.addListener(this);
            }
        });

        setGraphic(pane);
    }

    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    private void update(SuggestionList.SuggestionListItem item)
    {
        if (item != null && item.index == -1)
        {
            pane.setCenter(special);
            pane.setRight(null);
        }
        else
        {
            pane.setCenter(hbox);
        }

        if (item != null && item.index != -1)
        {
            update(item.getDetails().choice, item.getDetails().suffix, item.getDetails().type, item.typeMatch, item.direct, item.eligibleAt.get(), item.eligibleLength.get(), item.eligibleCanTab.get(), item.highlighted.get());
        }
        else
        {
            update("", "", "", false, true, 0, 0, false, false);
        }
    }

    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    private void update(String text, String unmatchableSuffix, String type, boolean typeMatch, boolean direct, int at, int len, boolean canTab, boolean highlighted)
    {
        this.type.setText(type);
        JavaFXUtil.setPseudoclass("bj-match", typeMatch, this.type);
        if (text.length() >= 1)
        {
            this.next.setText(text.substring(0, 1));
            this.suffix.setText(text.substring(1));
        }
        else
        {
            this.next.setText("");
            this.suffix.setText("");
        }
        this.fixedPostSuffix.setText(unmatchableSuffix);
        JavaFXUtil.setPseudoclass("bj-suggestion-similar", !direct, pane);

        JavaFXUtil.setPseudoclass("bj-suggestion-highlight", highlighted, pane);
        
        prefix.setText(text.substring(0, at));
        int end = Math.min(at + len, text.length());
        matching.setText(text.substring(at, end));
        String rest = text.substring(end);
        if (rest.length() >= 1)
        {
            next.setText(rest.substring(0, 1));
            suffix.setText(rest.substring(1));
        }
        else
        {
            next.setText("");
            suffix.setText("");
        }
    }

    // We are a change listener on several properties, to update the item each time.
    // Rather than pick out what changed, we just do a full update:
    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    public void changed(ObservableValue<?> observable, Object oldValue, Object newValue)
    {
        update(itemProperty().get());
    }
}