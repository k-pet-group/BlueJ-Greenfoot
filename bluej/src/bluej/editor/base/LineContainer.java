/*
 This file is part of the BlueJ program. 
 Copyright (C) 2021  Michael Kolling and John Rosenberg

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
package bluej.editor.base;

import bluej.utility.javafx.JavaFXUtil;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import threadchecker.OnThread;
import threadchecker.Tag;

@OnThread(value = Tag.FXPlatform, ignoreParent = true)
public class LineContainer extends Region
{
    private final LineDisplay lineDisplay;
    private final boolean lineWrapping;

    public LineContainer(LineDisplay lineDisplay, boolean lineWrapping)
    {
        this.lineDisplay = lineDisplay;
        this.lineWrapping = lineWrapping;
        JavaFXUtil.addStyleClass(this, "line-container");
    }

    @Override
    protected void layoutChildren()
    {
        double y = snapPositionY(lineDisplay.getFirstVisibleLineOffset());
        if (!lineWrapping)
        {
            double height = snapSizeY(lineDisplay.calculateLineHeight());
            for (Node child : getChildren())
            {
                if (child instanceof MarginAndTextLine)
                {
                    double nextY = snapPositionY(y + height);
                    child.resizeRelocate(0, y, Math.max(getWidth(), child.prefWidth(-1.0)), nextY - y);
                    y = nextY;
                }
            }
        } else
        {
            for (Node child : getChildren())
            {
                if (child instanceof MarginAndTextLine)
                {
                    double height = snapSizeY(child.prefHeight(getWidth()));
                    double nextY = snapPositionY(y + height);
                    child.resizeRelocate(0, y, getWidth(), nextY - y);
                    y = nextY;
                }
            }
        }
    }

    @Override
    @OnThread(Tag.FX)
    public ObservableList<Node> getChildren()
    {
        return super.getChildren();
    }

    public double getTextDisplayWidth()
    {
        return getWidth() - lineDisplay.textLeftEdge();
    }
}
