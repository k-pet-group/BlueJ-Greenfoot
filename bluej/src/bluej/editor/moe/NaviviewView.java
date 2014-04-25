/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009, 2014  Michael Kolling and John Rosenberg 
 
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
 
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Transparency;
import java.awt.image.BufferedImage;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentEvent.ElementChange;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.Position;
import javax.swing.text.Segment;
import javax.swing.text.TabExpander;
import javax.swing.text.ViewFactory;

import bluej.Config;

/**
 * A view for the NaviView component.
 * 
 * @author Davin McCall
 */
public class NaviviewView extends BlueJSyntaxView
{
    private static final boolean SCOPE_HIGHLIGHTING = true;
    private static final boolean HIGHLIGHT_METHODS_ONLY = true;
    private static final boolean SYNTAX_COLOURING = false;
    
    // MacOS font rendering at small sizes seems to be vastly different
    // Use 2^3 for MacOS Tiger/Leopard
    // Use 2^2 for MacOS Snow Leopard
    // Use 2^1 for everything else
    private static final int DARKEN_AMOUNT = Config.isMacOS() ? (Config.isMacOSSnowLeopard() ? 2 : 3) : 1;
    
    private NaviView naviView;
    
    public NaviviewView(Element elem, NaviView naviView)
    {
        super(elem, 0);
        this.naviView = naviView;
    }
    
    @Override
    protected void paintTaggedLine(Segment line, int lineIndex, Graphics g,
            int x, int y, MoeSyntaxDocument document, Color def,
            Element lineElement, TabExpander tx)
    {
        // Painting at such a small font size means the font appears very light.
        // To get around this problem, we paint into a temporary image, then darken
        // the text, and finally copy the temporary image to the output Graphics.
        
        try {
            int lineHeight = metrics.getHeight();
            int endPos = lineElement.getEndOffset() - 1;
            Rectangle dummyShape = new Rectangle(0,0,0,0);
            int endX = modelToView(endPos, dummyShape, Position.Bias.Forward).getBounds().x;
            int beginX = modelToView(lineElement.getStartOffset(), dummyShape, Position.Bias.Forward).getBounds().x;
            int width = endX - beginX;
            if (width <= 0) {
                return;
            }

            Rectangle clipBounds = g.getClipBounds();
            width = Math.min(width, clipBounds.width);
            BufferedImage img;
            if (g instanceof Graphics2D) {
                img = ((Graphics2D)g).getDeviceConfiguration()
                .createCompatibleImage(width, lineHeight, Transparency.TRANSLUCENT);
            }
            else {
                img = new BufferedImage(width, lineHeight, BufferedImage.TYPE_INT_ARGB);
            }

            Graphics2D imgG = img.createGraphics();
            imgG.setFont(g.getFont());
            imgG.setColor(g.getColor());

            if (SYNTAX_COLOURING) {
                if (document.getParsedNode() != null) {
                    super.paintTaggedLine(line, lineIndex, imgG, x - clipBounds.x,
                            metrics.getAscent(), document, def, lineElement, tx);
                } else {
                    paintPlainLine(lineIndex, imgG, x - clipBounds.x, metrics.getAscent());
                }
            } else {
                paintPlainLine(lineIndex, imgG, x - clipBounds.x, metrics.getAscent());
            }

            // Filter the image - adjust alpha channel to darken the image.
            int argb;
            int alpha;
            for (int iy = 0; iy < img.getHeight(); iy++) {
                for (int ix = 0; ix < img.getWidth(); ix++) {
                    argb = img.getRGB(ix, iy);
                    alpha = (argb >>> 24) << DARKEN_AMOUNT;//get the alpha channel and apply the darken effect
                    if (alpha > 255) {
                        alpha = 255; //prevent saturation
                    }
                    img.setRGB(ix, iy, (alpha << 24) | (argb & 0xffffff));//apply the new aplha channel to the image
                }
            }

            g.drawImage(img, clipBounds.x, y - metrics.getAscent(), null);
        }
        catch (BadLocationException ble) {}
    }
    
    @Override
    public void paint(Graphics g, Shape a)
    {
        Rectangle bounds = a.getBounds();
        Rectangle clip = g.getClipBounds();
        if (clip == null) {
            clip = a.getBounds();
        }
        
        if (SCOPE_HIGHLIGHTING) {
            // Scope highlighting
            MoeSyntaxDocument document = (MoeSyntaxDocument)getDocument();
            if (document.getParsedNode() != null) {
                int spos = viewToModel(bounds.x, clip.y, a, new Position.Bias[1]);
                int epos = viewToModel(bounds.x, clip.y + clip.height - 1, a, new Position.Bias[1]);

                Element map = getElement();
                int firstLine = map.getElementIndex(spos);
                int lastLine = map.getElementIndex(epos);
                paintScopeMarkers(naviView.getScalingImgBufferGraphics(g), document, a, firstLine, lastLine,
                        HIGHLIGHT_METHODS_ONLY, true);
            }
        }

        super.paint(g, a);
    }
    

    @Override
    protected void updateDamage(DocumentEvent changes, Shape a, ViewFactory f)
    {
        ElementChange ec = changes.getChange(getDocument().getDefaultRootElement());
        if (ec != null) {
            Element [] addedChildren = ec.getChildrenAdded();
            Element [] removedChildren = ec.getChildrenRemoved();
            if (addedChildren.length != removedChildren.length) {
                naviView.documentChangedLength();
            }
        }
        super.updateDamage(changes, a, f);
    }
    
}
