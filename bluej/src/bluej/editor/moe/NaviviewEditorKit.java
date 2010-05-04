/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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
package bluej.editor.moe;

import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

/**
 * An EditorKit implementation for the NaviView. The main purpose is to provide a NaviviewView as
 * the default view.
 * 
 * @author Davin McCall
 */
public class NaviviewEditorKit extends DefaultEditorKit implements ViewFactory
{
    private NaviView naviView;
    
    public NaviviewEditorKit(NaviView naviView)
    {
        this.naviView = naviView;
    }
    
    @Override
    public ViewFactory getViewFactory()
    {
        return this;
    }

    public View create(Element elem)
    {
        return new NaviviewView(elem, naviView);
    }
    
    @Override
    public Document createDefaultDocument()
    {
        return new MoeSyntaxDocument();
    }
}
