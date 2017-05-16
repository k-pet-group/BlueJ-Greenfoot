/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2016  Michael Kolling and John Rosenberg 
 
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
package bluej.debugmgr.objectbench;


import javax.swing.SwingUtilities;
import javafx.beans.binding.When;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.AnchorPane;

import bluej.debugger.*;
import bluej.debugmgr.inspector.ObjectBackground;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.prefmgr.PrefMgr;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A wrapper around array objects.
 * 
 * The array wrapper is represented by a few red ovals that are visible on the
 * object bench.
 * 
 * @author Andrew Patterson
 * @author Bruce Quig
 * @version $Id: ArrayWrapper.java 16633 2016-09-30 20:47:52Z nccb $
 */
@OnThread(Tag.FXPlatform)
public class ArrayWrapper extends ObjectWrapper
{
    public static int WORD_GAP = 8;
    public static int SHADOW_SIZE = 3;
    public static int ARRAY_GAP = 3;

    public ArrayWrapper(PkgMgrFrame pmf, ObjectBench ob, DebuggerObject obj, String instanceName)
    {
        super(pmf, ob, obj, obj.getGenType(), instanceName);
    }

    @Override
    protected void createComponent(Label label)
    {
        AnchorPane multipleBackgrounds = new AnchorPane();
        for (int i = 2; i >= 0; i--)
        {
            ObjectBackground bk = new ObjectBackground(CORNER_SIZE, new When(focusedProperty()).then(FOCUSED_BORDER).otherwise(UNFOCUSED_BORDER));
            multipleBackgrounds.getChildren().add(bk);
            AnchorPane.setTopAnchor(bk, (double)(i * ARRAY_GAP));
            AnchorPane.setLeftAnchor(bk, (double)(i * ARRAY_GAP));
            AnchorPane.setRightAnchor(bk, (double)((2-i) * ARRAY_GAP));
            AnchorPane.setBottomAnchor(bk, (double)((2-i) * ARRAY_GAP));
        }
        getChildren().addAll(multipleBackgrounds, label);
        setBackground(null);
        setEffect(new DropShadow(SHADOW_RADIUS, SHADOW_RADIUS/2.0, SHADOW_RADIUS/2.0, javafx.scene.paint.Color.GRAY));
    }
}
