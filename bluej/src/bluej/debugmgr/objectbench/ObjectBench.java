package bluej.debugmgr.objectbench;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.event.*;
import javax.swing.plaf.basic.BasicArrowButton;


import bluej.Config;
import bluej.testmgr.record.InvokerRecord;

/**
 * The object responsible for the panel that displays objects
 * at the bottom of the package manager.
 * @author  Michael Cahill
 * @author  Andrew Patterson
 * @version $Id: ObjectBench.java 2714 2004-07-01 15:55:03Z mik $
 */
public class ObjectBench extends JPanel 
    implements FocusListener, KeyListener, MouseListener
{
    static final int SCROLL_AMOUNT = (ObjectWrapper.WIDTH / 3);
    private static final Color BACKGROUND_COLOR = Config.getItemColour("colour.objectbench.background");
    
    private JButton leftArrowButton, rightArrowButton;
    public JViewport viewPort;
    private ObjectBenchPanel obp;
    public boolean hasFocus;
    private List objectWrappers;
    private ObjectWrapper selectedObjectWrapper;
    private int currentObjectWrapperIndex = -1;
	
    // All invocations done since our last reset.
    private List invokerRecords;

   
    /**
     * Construct an object bench which is used to hold
     * a bunch of object reference Components.
     */
    public ObjectBench()
    {
        super();
        objectWrappers = new ArrayList();
        createComponent();
    }

    
    /**
     * Paint the object bench.
     */
    public void paint(Graphics g){
        super.paint(g);
        if(hasFocus){
            g.setColor(Color.BLUE);
            g.drawRect(0, 0, getWidth() - 2, getHeight() - 3);
        }
    }
    /**
     * Add an object (in the form of an ObjectWrapper) to this bench.
     */
    public void addObject(ObjectWrapper wrapper)
    {
        // check whether name is already taken

        String newname = wrapper.getName();
        int count = 1;

        while(hasObject(newname)) {
            count++;
            newname = wrapper.getName() + "_" + count;
        }
        wrapper.setName(newname);

        wrapper.addFocusListener(this);
        obp.add(wrapper);
        objectWrappers.add(wrapper);
        obp.setPreferredSize(new Dimension(obp.getLayoutWidthMin(), ObjectWrapper.HEIGHT));
        enableButtons(viewPort.getViewPosition());
        obp.revalidate();
        obp.repaint();
    }

    /**
     * Return all the wrappers stored in this object bench in an array
     */
    public ObjectWrapper[] getObjects()
    {
        Component[] components = obp.getComponents();
        int count = getObjectCount();
                        
        ObjectWrapper[] wrappers = new ObjectWrapper[count];

        for(int i=0, j=0; i<components.length; i++) {
            if (components[i] instanceof ObjectWrapper)
                wrappers[j++] = (ObjectWrapper) components[i];
        }
        
        return wrappers;
    }
    
    /**
     * Get the object with name 'name', or null, if it does not
     * exist.
     *
     * @param name  The name to check for.
     * @return  The named object wrapper, or null if not found.
     */
    public ObjectWrapper getObject(String name)
    {
        ObjectWrapper[] wrappers = getObjects();

        for(int i=0; i<wrappers.length; i++)
            if(wrappers[i].getName().equals(name))
                return wrappers[i];

        return null;
    }

    /**
     * Check whether the bench contains an object with name 'name'.
     *
     * @param name  The name to check for.
     */
    public boolean hasObject(String name)
    {
        return getObject(name) != null;
    }

    /**
     * Count of object bench copmponents that are object wrappers
     * @return number of ObjectWrappers on the bench
     */
    public int getObjectCount()
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
     * Remove all objects from the object bench.
     */
    public void removeAllObjects(String scopeId)
    {
        ObjectWrapper[] wrappers = getObjects();

        for(int i=0; i<wrappers.length; i++) {
            ObjectWrapper aWrapper = wrappers[i];

            if ( aWrapper == selectedObjectWrapper )
                setSelectedObject ( null );
            
            aWrapper.prepareRemove();
            aWrapper.getPackage().getDebugger().removeObject(aWrapper.getName());

            obp.remove(aWrapper);
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
    public void removeObject(ObjectWrapper wrapper, String scopeId)
    {
        if ( wrapper == selectedObjectWrapper )
            setSelectedObject ( null );
            
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
        
        obp.revalidate();
        obp.repaint();
    }

    
    /**
     * Sets what is the currently selected ObjectWrapper, null can be given to 
     * signal that no wrapper is selected.
     */
    public void setSelectedObject (ObjectWrapper aWrapper)
    {
        if (selectedObjectWrapper != null){
            selectedObjectWrapper.setSelected(false);
        }
        selectedObjectWrapper = aWrapper;
        
        if (selectedObjectWrapper != null) {
            selectedObjectWrapper.setSelected(true);
            currentObjectWrapperIndex = objectWrappers.indexOf(aWrapper);
            selectedObjectWrapper.requestFocusInWindow();
        }
    }

    /**
     * Returns the currently selected object wrapper. 
     * If no wrapper is selected null is returned.
     */
    public ObjectWrapper getSelectedObject()
    {
        return selectedObjectWrapper;
    }

    /**
     * Add a listener for object events to this bench.
     * @param l  The listener object.
     */
    public void addObjectBenchListener(ObjectBenchListener l)
    {
        obp.addObjectBenchListener(l);
    }

    /**
     * Remove a listener for object events to this bench.
     * @param l  The listener object.
     */
    public void removeObjectBenchListener(ObjectBenchListener l)
    {
        obp.removeObjectBenchListener(l);
    }
    
    /**
     * Fire an object event for the named object.
     */
    public void fireObjectEvent(ObjectWrapper wrapper)
    {
        obp.fireObjectEvent(wrapper);
    }


    /**
	 * Post the object menu for the selected object.
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
                revalidate();
                repaint();
            }
        };
        SwingUtilities.invokeLater(refreshUI);
    }

    protected int getMaxXExtent()
    {
        return Math.max(obp.getLayoutWidthMin() - viewPort.getWidth(), 0);
    }
    
    // --- FocusListener interface ---
    
    /**
     * Note that the object bench got keyboard focus.
     * @see java.awt.event.FocusListener#focusGained(java.awt.event.FocusEvent)
     */
    public void focusGained(FocusEvent e) {
        hasFocus = true;
        repaint();
    }

    /**
     * Note that the object bench lost keyboard focus.
     * @see java.awt.event.FocusListener#focusLost(java.awt.event.FocusEvent)
     */
    public void focusLost(FocusEvent e) {
        hasFocus = false;
        repaint();
    }

    // --- end of FocusListener interface ---

    // --- KeyListener interface ---

    /**
     * A key was pressed in the object bench.
     * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
     */
    public void keyPressed(KeyEvent e) 
    {
        int key = e.getKeyCode();
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
                setSelectedObject(null);
                repaint();
                break;
            }
        }
        boolean isInRange = (0 <= currentObjectWrapperIndex && currentObjectWrapperIndex < objectWrappers.size()); 
        if (isInRange){
            ObjectWrapper currentObjectWrapper = (ObjectWrapper) objectWrappers.get(currentObjectWrapperIndex);
            setSelectedObject(currentObjectWrapper);
            adjustBench(currentObjectWrapper);
            repaint();
        }
    }

    /**
     * A key was released in the object bench.
     * @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent)
     */
    public void keyReleased(KeyEvent e) {
    }

    /**
     * A key was typed in the object bench.
     * @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent)
     */
    public void keyTyped(KeyEvent e) {
    }
    
    // --- end of KeyListener interface ---

    // --- MouseListener interface ---

    /**
     * The mouse was clicked in the object bench.
     * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
     */
    public void mouseClicked(MouseEvent e) {
    }

    /**
     * The mouse entered the object bench.
     * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
     */
    public void mouseEntered(MouseEvent e) {
    }

    /**
     * The mouse left the object bench.
     * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
     */
    public void mouseExited(MouseEvent e) {
    }

    /**
     * The mouse was pressed in the object bench.
     * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
     */
    public void mousePressed(MouseEvent e) {
        requestFocusInWindow();
    }

    /**
     * The mouse was released in the object bench.
     * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
     */
    public void mouseReleased(MouseEvent e) {
    }

    // --- end of MouseListener interface ---

    
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

    /**
     * Create the object bench as a good looking Swing component.
     */
    private void createComponent()
    {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createBevelBorder(BevelBorder.LOWERED),
                BorderFactory.createEmptyBorder(5,0,5,0)));

        // scroll left button
        leftArrowButton = new ObjectBenchArrowButton(SwingConstants.WEST);
        leftArrowButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            { 
                moveBench(-1);
            }
        });
        add(leftArrowButton);

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
        
        add(viewPort);
            
        // scroll right button
        rightArrowButton = new ObjectBenchArrowButton(SwingConstants.EAST);
        rightArrowButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            { 
                moveBench(1);
            }
        });
        
        add(rightArrowButton);

        // start with a clean slate recording invocations
        resetRecordingInteractions();
        //when empty, the objectbench is focusable
        setFocusable(true);        

        obp.setBackground(BACKGROUND_COLOR);
        setBackground(BACKGROUND_COLOR);        

        addFocusListener(this);
        addKeyListener(this);
        addMouseListener(this);        
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
            setSelectedObject ( wrapper );
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
}
