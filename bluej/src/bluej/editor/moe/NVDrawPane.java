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

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import javax.swing.JEditorPane;
import javax.swing.plaf.ComponentUI;
import javax.swing.text.Document;
import javax.swing.text.View;

/**
 * A JEditorPane implementation to provide the root view for the document. This is used
 * by the NaviView component. Basically this provides:
 * 
 * <ul>
 * <li>A means to get the root view for the document
 * <li>A proxy for repaints due to document updates. The repaints are passed on
 *     to the NaviView, allowing it to translate co-ordinates appropriately.
 * </ul>
 * 
 * @author Davin McCall
 */
public class NVDrawPane extends JEditorPane
{
    private NaviView nview;
    
    private int repaintTop;
    private int repaintEnd;
    
    public NVDrawPane(NaviView nview)
    {
        this.nview = nview;
        Font smallFont = new Font("Monospaced", Font.BOLD, 1);
        setFont(smallFont);
        setEditorKit(new NaviviewEditorKit(nview));
    }

    @Override
    public void setDocument(Document doc)
    {
        super.setDocument(doc);
        setBorder(null);
        fakeRepaint();
    }
    
    @Override
    protected void setUI(ComponentUI newUI)
    {
        super.setUI(newUI);
        fakeRepaint();
    }
    
    private void fakeRepaint()
    {
        // Hack: the BasicTextUI.UpdateHandler passes a null allocation to the view
        //      if it hasn't painted yet. The result is that same-line updates aren't
        //      handled. If we do a pretend paint here, the problem is solved.
        BufferedImage bi = new BufferedImage(1,1, BufferedImage.TYPE_INT_ARGB);
        Graphics g = bi.getGraphics();
        g.setClip(0, 0, 1, 1);
        getUI().paint(g, this);
    }
    
    @Override
    public void repaint()
    {
        if (nview != null) {
            //nview.repaint();
            Rectangle r = getBounds();
            repaint(0, r.x, r.y, r.width, r.height);
            //nview.repaint();
        }
    }
    
    @Override
    public void repaint(long tm, int x, int y, int width, int height)
    {
        if (nview != null) {
            // Note this condition appears impossible, however JEditorPane constructor
            // does call repaint().
            repaintTop = y;
            repaintEnd = y + height;
            nview.repaintModel(repaintTop, repaintEnd);
        }
    }
    
    @Override
    public Rectangle getBounds()
    {
        View view = getUI().getRootView(this);
        Rectangle r = new Rectangle(1,
                (int) view.getPreferredSpan(View.Y_AXIS));
        return r;
    }
}
