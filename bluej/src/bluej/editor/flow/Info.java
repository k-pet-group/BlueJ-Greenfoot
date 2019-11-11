/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011,2013,2014,2016,2019  Michael Kolling and John Rosenberg
 
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
package bluej.editor.flow;

import bluej.prefmgr.PrefMgr;
import bluej.utility.javafx.JavaFXUtil;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * An information panel, displayed at the bottom of a MoeEditor window. The panel can
 * display error messages / notices to the user.
 *
 * @author Michael Kolling
 */
@OnThread(Tag.FXPlatform)
public final class Info extends TextFlow
{
    private final Text text;
    private boolean isClear;

    // ------------- METHODS --------------

    /**
     * Construct a new Info instance.
     */
    public Info()
    {
        JavaFXUtil.addStyleClass(this, "moe-info");
        text = new Text();
        JavaFXUtil.addStyleClass(text, "moe-info-text");
        getChildren().add(text);

        text.styleProperty().bind(PrefMgr.getEditorFontCSS(false));

        isClear = true;
    }

    /**
     * display a one- or two-line message (using '\n' to separate multiple lines).
     */
    public void message(String msg)
    {
        text.setText(msg);
        isClear = false;
    }
    
    /**
     * Like message(String), but the message may be displayed in a pop-up dialog if the user
     * has enabled this preference (e.g. for blind users with screen readers)
     */
    public void messageImportant(String msg)
    {
        message(msg);
    }

    /**
     * display a two line message
     */
    public void message(String msg1, String msg2)
    {
        message(msg1 + '\n' + msg2);
    }

    /**
     * clear the display
     */
    public void clear()
    {
        if (!isClear) {
            message ("");
            isClear = true;
        }
    }
}
