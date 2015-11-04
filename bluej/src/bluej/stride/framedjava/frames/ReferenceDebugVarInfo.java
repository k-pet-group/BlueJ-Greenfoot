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
package bluej.stride.framedjava.frames;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.Config;
import bluej.debugger.DebuggerField;
import bluej.pkgmgr.Package;

public class ReferenceDebugVarInfo implements DebugVarInfo
{
    private Package pkg;
    private JFrame editorFrame;
    private DebuggerField field;
    
    @OnThread(Tag.Any)
    public ReferenceDebugVarInfo(Package pkg, JFrame editorFrame, DebuggerField field)
    {
        this.pkg = pkg;
        this.editorFrame = editorFrame;
        this.field = field;
    }

    @Override
    public Node getDisplay(DebugVarInfo prev)
    {
        Image img = Config.getImageAsFXImage("image.eval.object");
        ImageView view = new ImageView(img);
        view.setOnMouseClicked(new EventHandler<Event>() {
            @Override
            public void handle(Event e)
            {
                SwingUtilities.invokeLater(() ->
                    pkg.getProject().getInspectorInstance(field.getValueObject(null), null, pkg, null, editorFrame)
                );
                // Stop event being passed through to canvas:
                e.consume();
            }
        });
        return view;
    }

    @Override
    public String getInternalValueString()
    {
        return "" + field.getValueObject(null).getObjectReference().uniqueID();
    }

}
