package bluej.debugger;

import java.util.List;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.basic.*;
import java.util.*;

import bluej.testmgr.*;

/**
 * The object responsible for the panel that displays objects
 * at the bottom of the package manager.
 *
 * @author  Michael Cahill
 * @author  Andrew Patterson
 * @version $Id: ObjectBench.java 1954 2003-05-15 06:06:01Z ajp $
 */
public class ObjectBench
{
    static final int SCROLL_AMOUNT = (ObjectWrapper.WIDTH / 3);

    private JPanel containerPanel;
    private JButton leftArrowButton, rightArrowButton;
    private JViewport viewPort;
    private ObjectBenchPanel obp;

    public ObjectBench()
    {
        containerPanel = new JPanel();
        containerPanel.setLayout(new BoxLayout(containerPanel, BoxLayout.X_AXIS));
            
        leftArrowButton = new ObjectBenchArrowButton(SwingConstants.WEST);
        leftArrowButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            { 
                moveBench(-1);
            }
        });
                
        containerPanel.add(leftArrowButton);

        obp = new ObjectBenchPanel();

        viewPort = new JViewport();
        viewPort.setView(obp);
//        viewPort.setScrollMode(JViewport.SIMPLE_SCROLL_MODE);

/*        obp.addComponentListener(new ComponentListener() {
            public void componentHidden(ComponentEvent e) {}
            public void componentMoved(ComponentEvent e) {}
            public void componentShown(ComponentEvent e)  {}
            public void componentResized(ComponentEvent e)
            {
                enableButtons(ObjectBench.this.viewPort.getViewPosition());
            }
        }); */
        
        viewPort.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e)
            {
                enableButtons(ObjectBench.this.viewPort.getViewPosition());
            }
        });

        viewPort.setMinimumSize(new Dimension(ObjectWrapper.WIDTH * 3, ObjectWrapper.HEIGHT));
        viewPort.setPreferredSize(new Dimension(ObjectWrapper.WIDTH * 3, ObjectWrapper.HEIGHT));
        viewPort.setMaximumSize(new Dimension(ObjectWrapper.WIDTH * 1000, ObjectWrapper.HEIGHT));
        
        containerPanel.add(viewPort);
            
        rightArrowButton = new ObjectBenchArrowButton(SwingConstants.EAST);
        rightArrowButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            { 
                moveBench(1);
            }
        });
        
        containerPanel.add(rightArrowButton);

 //       moveBench(0);

/*        obpholder.setLayout(new BoxLayout(obpholder, BoxLayout.Y_AXIS));

        obpholder.add(obp);        

        obpholder.setMinimumSize(new Dimension(WIDTH,HEIGHT));
        obpholder.setPreferredSize(new Dimension(WIDTH,HEIGHT));
        obpholder.setMaximumSize(new Dimension(WIDTH*1000,HEIGHT)); */

        resetRecordingInteractions();
    }

    private void moveBench(int xamount)
    {        
        Point pt = viewPort.getViewPosition();

        pt.x += (SCROLL_AMOUNT * xamount);
        pt.x = Math.max(0, pt.x);
        pt.x = Math.min(getMaxXExtent(), pt.x);

        viewPort.setViewPosition(pt);
    }

    private void enableButtons(Point pt)
    {
        boolean buttonsNeeded = false;
        
        if (pt.x == 0)
            leftArrowButton.setEnabled(false);
        else {
            leftArrowButton.setEnabled(true);
            buttonsNeeded = true;
        }

        if (pt.x >= getMaxXExtent())
            rightArrowButton.setEnabled(false);
        else {
            rightArrowButton.setEnabled(true);
            buttonsNeeded = true;
        }
        
        if(buttonsNeeded) {
            rightArrowButton.setVisible(true);
            leftArrowButton.setVisible(true);
        }
       else {
            rightArrowButton.setVisible(false);
            leftArrowButton.setVisible(false);
        }
    }

    protected int getMaxXExtent()
    {
        return obp.getLayoutWidthMin() - viewPort.getWidth();
    }
    
    // ------------- nested class ObjectBenchPanel --------------

    /**
     * This is an inner class so that people can't add or remove
     * components to it that are of the wrong type (ie not ObjectWrapper).
     */
    private class ObjectBenchPanel extends JPanel
    {
        LayoutManager lm;
        
        public ObjectBenchPanel()
        {
            setLayout(lm = new BoxLayout(this, BoxLayout.X_AXIS));
            setAlignmentY(0);

            setMinimumSize(new Dimension(ObjectWrapper.WIDTH, ObjectWrapper.HEIGHT));
            setPreferredSize(new Dimension(ObjectWrapper.WIDTH, ObjectWrapper.HEIGHT));
            setMaximumSize(new Dimension(ObjectWrapper.WIDTH * 1000, ObjectWrapper.HEIGHT));

            setSize(ObjectWrapper.WIDTH, ObjectWrapper.HEIGHT);
        }

        public int getLayoutWidthMin()
        {
            Dimension d = lm.minimumLayoutSize(this);

            return d.width;
        }

        /**
         * This component will raise ObjectBenchEvents when nodes are
         * selected in the bench. The following functions manage this.
         */
        public void addObjectBenchListener(ObjectBenchListener l)
        {
            listenerList.add(ObjectBenchListener.class, l);
        }

        public void removeObjectBenchListener(ObjectBenchListener l)
        {
            listenerList.remove(ObjectBenchListener.class, l);
        }

        // notify all listeners that have registered interest for
        // notification on this event type.
        void fireObjectEvent(ObjectWrapper wrapper)
        {
            // guaranteed to return a non-null array
            Object[] listeners = listenerList.getListenerList();
            // process the listeners last to first, notifying
            // those that are interested in this event
            for (int i = listeners.length-2; i>=0; i-=2) {
                if (listeners[i] == ObjectBenchListener.class) {
                    ((ObjectBenchListener)listeners[i+1]).objectEvent(
                            new ObjectBenchEvent(this,
                                    ObjectBenchEvent.OBJECT_SELECTED, wrapper));
                }
            }
        }
    }
    
    // ----------- nested class ObjectBenchArrowButton ------------
    
    /**
     * Nested class to define our custom Object Bench scroll buttons.
     */
    class ObjectBenchArrowButton extends BasicArrowButton
    {
        public ObjectBenchArrowButton(int direction)
        {
            super(direction);
        }
        
        public Dimension getMaximumSize()
        {
            return new Dimension(14, ObjectWrapper.HEIGHT);
        }

        public Dimension getMinimumSize()
        {
            return new Dimension(14, ObjectWrapper.HEIGHT);
        }

        public Dimension getPreferredSize()
        {
            return new Dimension(14, ObjectWrapper.HEIGHT);
        }

        public void paint(Graphics g)
        {
            Color origColor = g.getColor();
            int w = getSize().width;
            int h = getSize().height;
            final int size = 10;  // arrow size
            boolean isPressed = getModel().isPressed();
            
            g.setColor(getBackground());
            g.fillRect(0, 0, w, h);

            if (isPressed) {
                g.translate(1, 1);
            }

            paintTriangle(g, (w - size) / 2, (h - size) / 2, size, 
                          getDirection(), isEnabled());
        
            // Reset the Graphics back to it's original settings
            if (isPressed) {
                g.translate(-1, -1);
            }
            g.setColor(origColor);
        }    
    }
    
    // ----------------- end of nested class ------------------

    public JComponent getComponent()
    {
        return containerPanel;
    }

    public void addObjectBenchListener(ObjectBenchListener l)
    {
        obp.addObjectBenchListener(l);
    }

    public void removeObjectBenchListener(ObjectBenchListener l)
    {
        obp.removeObjectBenchListener(l);
    }
    
    public void fireObjectEvent(ObjectWrapper wrapper)
    {
        obp.fireObjectEvent(wrapper);
    }

    /**
     * Add an object (in the form of an ObjectWrapper) to this bench.
     */
    public void add(ObjectWrapper wrapper)
    {
        // check whether name is already taken

        String newname = wrapper.getName();
        int count = 1;

        while(hasObject(newname)) {
            count++;
            newname = wrapper.getName() + "_" + count;
        }
        wrapper.setName(newname);

        // add to bench

        //wrapper.setAlignmentY(0);
        obp.add(wrapper);

        obp.setPreferredSize(new Dimension(obp.getLayoutWidthMin(), ObjectWrapper.HEIGHT));
        enableButtons(viewPort.getViewPosition());
//        obp.setSize(new Dimension(obp.getLayoutWidthMin(), HEIGHT));
//        obp.invalidate();
//        obp.validate();
        obp.revalidate();
        obp.repaint();
    }

    /**
     * Return all the wrappers stored in this object bench in an array
     */
    public ObjectWrapper[] getWrappers()
    {
        Component[] components = obp.getComponents();
        int count = 0;
        
        for(int i=0; i<components.length; i++) {
            if (components[i] instanceof ObjectWrapper)
                count++;
        }
                        
        ObjectWrapper[] wrappers = new ObjectWrapper[count];

        for(int i=0, j=0; i<components.length; i++) {
            if (components[i] instanceof ObjectWrapper)
                wrappers[j++] = (ObjectWrapper) components[i];
        }
        
        return wrappers;
    }

    /**
     * Check whether the bench contains an object with name 'name'.
     *
     * @param name  The name to check for.
     */
    public boolean hasObject(String name)
    {
        ObjectWrapper[] wrappers = getWrappers();

        for(int i=0; i<wrappers.length; i++)
            if(wrappers[i].getName().equals(name))
                return true;

        return false;
    }

    /**
     * Remove all objects from the object bench.
     */
    public void removeAll(String scopeId)
    {
        ObjectWrapper[] wrappers = getWrappers();

        for(int i=0; i<wrappers.length; i++) {
            wrappers[i].prepareRemove();
			wrappers[i].getPackage().getDebugger().removeObjectFromScope(scopeId, wrappers[i].getName());

            obp.remove(wrappers[i]);
        }

        resetRecordingInteractions();
                      
        enableButtons(viewPort.getViewPosition());
    	obp.revalidate();
        obp.repaint();
    }

    /**
     * Remove an object from the object bench. When this is done, the object
     * is also removed from the scope of the package (so it is not accessible
     * as a parameter anymore) and the bench is redrawn.
     */
    public void remove(ObjectWrapper wrapper, String scopeId)
    {
        wrapper.prepareRemove();
		wrapper.getPackage().getDebugger().removeObjectFromScope(scopeId, wrapper.getName());
        obp.remove(wrapper);

        enableButtons(viewPort.getViewPosition());
    	obp.revalidate();
    	obp.repaint();
    }

    private List invokerRecords;
          
    public void resetRecordingInteractions()
    {
        invokerRecords = new LinkedList();
    }

    public void addInteraction(InvokerRecord ir)
    {
        if (invokerRecords == null)
            resetRecordingInteractions();
            
        invokerRecords.add(ir);    
    }
    
    public String getFixtureDeclaration()
    {
        StringBuffer sb = new StringBuffer();
        Iterator it = invokerRecords.iterator();
        
        while(it.hasNext()) {
            InvokerRecord ir = (InvokerRecord) it.next();
            
            if (ir.toFixtureDeclaration() != null)
            	sb.append(ir.toFixtureDeclaration());
        }                    

        return sb.toString();
    }
    
    public String getFixtureSetup()
    {
        StringBuffer sb = new StringBuffer();
        Iterator it = invokerRecords.iterator();
        
        while(it.hasNext()) {
            InvokerRecord ir = (InvokerRecord) it.next();
            
			if (ir.toFixtureSetup() != null)
	            sb.append(ir.toFixtureSetup());
        }                    

        return sb.toString();
    }
    
    public String getTestMethod()
    {
        StringBuffer sb = new StringBuffer();
        Iterator it = invokerRecords.iterator();
        
        while(it.hasNext()) {
            InvokerRecord ir = (InvokerRecord) it.next();
            
			if (ir.toTestMethod() != null)
	            sb.append(ir.toTestMethod());
        }                    

        return sb.toString();
    }
}
