package bluej.debugmgr.objectbench;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.basic.BasicArrowButton;


import bluej.testmgr.record.InvokerRecord;

/**
 * The object responsible for the panel that displays objects
 * at the bottom of the package manager.
 * @author  Michael Cahill
 * @author  Andrew Patterson
 * @version $Id: ObjectBench.java 2611 2004-06-14 12:46:18Z mik $
 */
public class ObjectBench
{
    static final int SCROLL_AMOUNT = (ObjectWrapper.WIDTH / 3);

    private JPanel containerPanel;
    private JButton leftArrowButton, rightArrowButton;
    public JViewport viewPort;
    private ObjectBenchPanel obp;
    public boolean hasFocus;
    private List objectWrappers;
    private ObjectWrapper selectedObjectWrapper;
    private int currentObjectWrapperIndex = -1;
	
   
    /**
     * Construct an object bench which is used to hold
     * a bunch of object reference Components.
     */
    public ObjectBench()
    {
    	objectWrappers = new LinkedList();
        containerPanel = new ContainerPanel(this);
        containerPanel.setLayout(new BoxLayout(containerPanel, BoxLayout.X_AXIS));

        // scroll left button
        leftArrowButton = new ObjectBenchArrowButton(SwingConstants.WEST);
        leftArrowButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            { 
                moveBench(-1);
            }
        });
        containerPanel.add(leftArrowButton);

        // a panel holding the actual object components
        obp = new ObjectBenchPanel();

        // a sliding viewport, showing us the above panel
        viewPort = new JViewport();
        viewPort.setView(obp);
        
        // when the view changes, we may need to enable/disable
        // the arrow buttons
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
            
        // scroll right button
        rightArrowButton = new ObjectBenchArrowButton(SwingConstants.EAST);
        rightArrowButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            { 
                moveBench(1);
            }
        });
        
        containerPanel.add(rightArrowButton);

        // start with a clean slate recording invocations
        resetRecordingInteractions();
        //when empty, the objectbench is not focusable
        containerPanel.setFocusable(false);
    }

    /**
     * Sets what is the currently selected ObjectWrapper, null can be given to 
     * signal that no wrapper is selected.
     */
    public void setSelectedObjectWrapper (ObjectWrapper aWrapper)
    {
    	if (selectedObjectWrapper != null){
    		selectedObjectWrapper.setSelected(false);
    		
    	}
        selectedObjectWrapper = aWrapper;
        
        if (selectedObjectWrapper != null){
    		selectedObjectWrapper.setSelected(true);
    		currentObjectWrapperIndex = objectWrappers.indexOf(aWrapper);
    		selectedObjectWrapper.requestFocusInWindow();
    	}
    }

    /**
     * Returns the currently selected object wrapper. 
    * If no wrapper is selected null is returned.
     */
    public ObjectWrapper getSelectedObjectWrapper()
    {
        return selectedObjectWrapper;
    }
    
    /**
	 * @param key
	 */
	public void handleKeyPressed(int key) {
		switch (key){
			case KeyEvent.VK_LEFT: {
				if (currentObjectWrapperIndex > 0){
				currentObjectWrapperIndex--;
				}
				else if (currentObjectWrapperIndex < 0 ){
					//currentObjectWrapperIndex = objectWrappers.size() - 1;
					currentObjectWrapperIndex = 0;
				}
				break;
			}
			case KeyEvent.VK_RIGHT: {
				if (currentObjectWrapperIndex < objectWrappers.size() - 1){
					currentObjectWrapperIndex++;
				}
				break;
			}
			case KeyEvent.VK_ENTER:{
				showPopupMenu();
				break;
			}
			case KeyEvent.VK_SPACE:{
				showPopupMenu();
				break;
			}
			case KeyEvent.VK_ESCAPE:{
				currentObjectWrapperIndex = -1;
				setSelectedObjectWrapper(null);
				containerPanel.repaint();
				break;
			}
		}
		boolean isInRange = (0 <= currentObjectWrapperIndex && currentObjectWrapperIndex < objectWrappers.size()); 
		if (isInRange){
			ObjectWrapper currentObjectWrapper = (ObjectWrapper) objectWrappers.get(currentObjectWrapperIndex);
			setSelectedObjectWrapper(currentObjectWrapper);
			adjustBench(currentObjectWrapper);
			containerPanel.repaint();
		}
	}
    
    /**
	 * 
	 */
	private void showPopupMenu() {
		if (selectedObjectWrapper != null){
			selectedObjectWrapper.showMenu();
		}
	}

	public void adjustBench(ObjectWrapper objectWrapper){
    	Rectangle wrapper = new Rectangle(objectWrapper.getX(), objectWrapper.getY(),
    									  objectWrapper.getWidth(), objectWrapper.getHeight());
    	//viewPort.scrollRectToVisible(wrapper); //this doesn't word! Bug in java?
    	Rectangle view = viewPort.getViewRect();
    	if (!view.contains(wrapper)){
    		if (wrapper.x < view.x) {
    			//then the wrapper is on the left side of view
    			int x = wrapper.x - ObjectWrapper.GAP;
    			int y = viewPort.getViewPosition().y;
    			viewPort.setViewPosition(new Point(x, y));
    		} else {
    			//then the wrapper is intersection the right side
    			int x = wrapper.x + wrapper.width - view.width;
    			int y = viewPort.getViewPosition().y;
    			viewPort.setViewPosition(new Point(x, y));
    		}
    	}
    }

    /**
     * Move the displayed objects on the object bench left and right.
     * 
     * @param xamount
     */
    public void moveBench(int xamount)
    {        
        Point pt = viewPort.getViewPosition();

        pt.x += (SCROLL_AMOUNT * xamount);
        pt.x = Math.max(0, pt.x);
        pt.x = Math.min(getMaxXExtent(), pt.x);

        viewPort.setViewPosition(pt);
    }

    /**
     * Based on the state of the viewport, enable or disable
     * the left and right scrolling arrows.
     * 
     * This also affects the visibility of the arrows.
     * 
     * @param pt
     */
    private void enableButtons(Point pt)
    {
        boolean buttonsNeeded = true;
        
        int maxExtent = getMaxXExtent();
        int currentLWidth = leftArrowButton.isVisible() ? leftArrowButton.getWidth() : 0;
        int currentRWidth = rightArrowButton.isVisible() ? rightArrowButton.getWidth() : 0;
        
        // check if we need the buttons at all
        int allowedWidth = viewPort.getWidth() + currentLWidth + currentRWidth;
        if (allowedWidth >= obp.getLayoutWidthMin() ) {
            if (pt.x != 0) {
                viewPort.setViewPosition(new Point(0,0));
            }
            buttonsNeeded = false;
        }
        
        if (buttonsNeeded) {
            leftArrowButton.setEnabled(pt.x > 0);
            rightArrowButton.setEnabled(pt.x < maxExtent);
           
        }
                
        rightArrowButton.setVisible(buttonsNeeded);
        leftArrowButton.setVisible(buttonsNeeded);
        
        // validating now could cause re-entrancy, which seems to cause
        // some minor problems.
        Runnable refreshUI = new Runnable()
        {
            public void run()
            {
                containerPanel.revalidate();
                containerPanel.repaint();
            }
        };
        SwingUtilities.invokeLater(refreshUI);
    }

    protected int getMaxXExtent()
    {
        return Math.max(obp.getLayoutWidthMin() - viewPort.getWidth(), 0);
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
            setSelectedObjectWrapper ( wrapper );
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
    
    /**
     * The panel that contains the arrows and the objectBenchPanel.
     * @author fisker
     *
     */
    
    
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

/*
		Experimental code to add == signs between equal objects on the object bench
		
		ObjectWrapper[] wrappers = getWrappers();
		int addPosition = -1;
		
		for(int i=wrappers.length-1; i>=0; i--)
			if(wrappers[i].getObject().equals(wrapper.getObject()))
				addPosition = i;

		if (addPosition != -1) {
			obp.add(wrapper, addPosition+1);
			obp.add(new JLabel(" =="), addPosition+1);
		}
		else
*/
        wrapper.addFocusListener((FocusListener) containerPanel);
		obp.add(wrapper);
		objectWrappers.add(wrapper);
        obp.setPreferredSize(new Dimension(obp.getLayoutWidthMin(), ObjectWrapper.HEIGHT));
        enableButtons(viewPort.getViewPosition());
        updateFocusability();
        obp.revalidate();
        obp.repaint();
    }

    
    //
    private void updateFocusability(){
    	if (getObjectWrapperCount() > 0){
    		containerPanel.setFocusable(true);
    	}
    	else{
    		containerPanel.transferFocusBackward();
    		containerPanel.setFocusable(false);
    	}
    }
    
    /**
     * Return all the wrappers stored in this object bench in an array
     */
    public ObjectWrapper[] getWrappers()
    {
        Component[] components = obp.getComponents();
        int count = getObjectWrapperCount();
                        
        ObjectWrapper[] wrappers = new ObjectWrapper[count];

        for(int i=0, j=0; i<components.length; i++) {
            if (components[i] instanceof ObjectWrapper)
                wrappers[j++] = (ObjectWrapper) components[i];
        }
        
        return wrappers;
    }
    
    /**
     * Count of object bench copmponents that are object wrappers
     * @return number of ObjectWrappers on the bench
     */
    public int getObjectWrapperCount()
    {
        Component[] components = obp.getComponents();
        int count = 0;
        
        for(int i=0; i<components.length; i++) {
            if (components[i] instanceof ObjectWrapper)
                count++;
        }
        return count;
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
            ObjectWrapper aWrapper = wrappers[i];

            if ( aWrapper == selectedObjectWrapper )
                setSelectedObjectWrapper ( null );
            
            aWrapper.prepareRemove();
            aWrapper.getPackage().getDebugger().removeObject(aWrapper.getName());

            obp.remove(aWrapper);
        }

        resetRecordingInteractions();
                      
        enableButtons(viewPort.getViewPosition());
        updateFocusability();
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
        if ( wrapper == selectedObjectWrapper )
            setSelectedObjectWrapper ( null );
            
        wrapper.prepareRemove();
		wrapper.getPackage().getDebugger().removeObject(wrapper.getName());
        obp.remove(wrapper);
        objectWrappers.remove(wrapper);
        // check whether we still need navigation arrows with the reduced
        // number of objects on the bench.
        enableButtons(viewPort.getViewPosition());

        // pull objects to the right if there is empty space on the right-
        // hand side 
        moveBench(0);
        
        updateFocusability();
    	obp.revalidate();
    	obp.repaint();
    }

    /**
     * All invocations done since our last reset.
     */
    private List invokerRecords;
      
    /**
     * Reset the recording of invocations.
     */
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
