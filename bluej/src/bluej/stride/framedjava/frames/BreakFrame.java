/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016,2018 Michael Kölling and John Rosenberg
 
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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package bluej.stride.framedjava.frames;


import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import bluej.stride.framedjava.elements.BreakElement;
import bluej.stride.generic.DefaultFrameFactory;
import bluej.stride.generic.FrameCanvas;
import bluej.stride.generic.FrameFactory;
import bluej.stride.generic.InteractionManager;
import bluej.stride.generic.SingleLineFrame;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.SharedTransition;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A break statement (no parameters). Further, the break statement takes on the colour of whatever it is breaking from, to indicate that it has some scope-level significance.
 * @author Fraser McKay
 */
public class BreakFrame extends SingleLineFrame
  implements CodeFrame<BreakElement>, DebuggableFrame
{

    private SimpleDoubleProperty xOffset;
    private SimpleDoubleProperty yOffset;

    public static enum BreakEncloser
    {
        WHILE, FOREACH, SWITCH;
        
        public String getPseudoClass()
        {
            switch (this)
            {
            case WHILE: return "bj-break-while";
            case FOREACH: return "bj-break-foreach";
            case SWITCH: return "bj-break-switch";
            }
            return null; // Impossible
        }
    }
    
    private BreakElement element;
    private Rectangle rectangle;
    private FrameCanvas outer;
    private VBox overlay;
    private boolean normalView = true;

    /**
     * Default constructor.
     */
    private BreakFrame(InteractionManager editor)
    {
        super(editor, "break", "break-");
        setHeaderRow(previewSemi);
    }
    
    public BreakFrame(InteractionManager editor, boolean enabled)
    {
        this(editor);
        frameEnabledProperty.set(enabled);
    }
        
    @Override
    public void updateAppearance(FrameCanvas c)
    {
        super.updateAppearance(c);
        if (!normalView || !isFrameEnabled())
        {
            JavaFXUtil.runNowOrLater(() -> setOverlay(false, null, null));
            return;
        }
        
        // Remove all relevant pseudo-classes:
        for (BreakEncloser e : BreakEncloser.values())
            JavaFXUtil.setPseudoclass(e.getPseudoClass(), false, getNode());
        
        while (c != null && c.getParent() != null && c.getParent().getFrame() != null && c.getParent().getFrame() instanceof CodeFrame<?>)
        {
            CodeFrame<?> cf = (CodeFrame<?>)c.getParent().getFrame();
            if (cf.asBreakEncloser() != null)
            {
                JavaFXUtil.setPseudoclass(cf.asBreakEncloser().getPseudoClass(), true, getNode());
                final FrameCanvas cFinal = c;
                JavaFXUtil.runNowOrLater(() -> setOverlay(true, cFinal, cf.asBreakEncloser().getPseudoClass()));
                return;
            }
            c = c.getParent().getFrame().getParentCanvas();
        }
        JavaFXUtil.runNowOrLater(() -> setOverlay(false, null, null));
    }

    public static FrameFactory<BreakFrame> getFactory()
    {
        return new DefaultFrameFactory<>(BreakFrame.class, BreakFrame::new);
    }

    @Override
    public BreakElement getCode()
    {
        return element;
    }
    
    @Override
    public void regenerateCode()
    {
        element = new BreakElement(this, frameEnabledProperty.get());
    }

    private void adjustOverlayBounds()
    {
        if (outer == null || rectangle == null || overlay == null)
            return;

        final double ourX = getNode().localToScene(0, 0).getX();
        final double theirX = outer.getContentSceneBounds().getMinX();

        double width = ourX - theirX;
        rectangle.setWidth(width + 1.0); // to break border of break
        // Round it to integer height:
        rectangle.setHeight(Math.ceil(getRegion().getHeight() / 4.0));

        xOffset.set(-width);
        yOffset.set(getRegion().getHeight() * 3.0 / 8.0);
    }

    @OnThread(Tag.FXPlatform)
    private void setOverlay(boolean on, FrameCanvas outer, String pseudo)
    {
        if (on && overlay == null)
        {
            this.outer = outer;
            rectangle = new Rectangle();
            overlay = new VBox(rectangle);
            JavaFXUtil.addStyleClass(overlay, "break-frame-overlay");
            JavaFXUtil.addStyleClass(rectangle, "break-frame-overlay-rect");
            JavaFXUtil.setPseudoclass(pseudo, true, overlay, rectangle);

            xOffset = new SimpleDoubleProperty(0.0);
            yOffset = new SimpleDoubleProperty(0.0);

            JavaFXUtil.onceInScene(overlay, this::adjustOverlayBounds);
            getEditor().getCodeOverlayPane().addOverlay(overlay, getNode(), xOffset, yOffset);
            
            JavaFXUtil.addChangeListener(getNode().localToSceneTransformProperty(), t -> adjustOverlayBounds());
            JavaFXUtil.addChangeListener(getNode().boundsInLocalProperty(), b -> adjustOverlayBounds());
        }
        else if (!on && overlay != null)
        {
            getEditor().getCodeOverlayPane().removeOverlay(overlay);
            overlay = null;
            rectangle = null;
            this.outer = null;
        }
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public void setView(View oldView, View newView, SharedTransition animation)
    {
        super.setView(oldView, newView, animation);
        normalView = newView == View.NORMAL;
        updateAppearance(getParentCanvas());
    }

    @Override
    protected void saveAsRecent()
    {
        // Do nothing; can never change value
    }

    @Override
    protected void cleanupFrame()
    {
        JavaFXUtil.runNowOrLater(() -> setOverlay(false, null, null));
    }
}
