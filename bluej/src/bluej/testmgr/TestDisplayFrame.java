package bluej.testmgr;

import java.awt.*;

import javax.swing.*;
import javax.swing.event.*;

import bluej.Config;
import bluej.debugger.DebuggerTestResult;

/**
 * A Swing based user interface to run tests.
 */
public class TestDisplayFrame
{
    static final String windowTitle = Config.getString("terminal.title");

    private static final Color fgColour = Color.black;
    private static final Color errorColour = Color.red;
    private static final Image iconImage = Config.getImageAsIcon("image.icon.terminal").getImage();

    // -- static singleton factory method --

    static TestDisplayFrame singleton = null;

    public synchronized static TestDisplayFrame getTestDisplay()
    {
        if(singleton == null)
            singleton = new TestDisplayFrame();
        return singleton;
    }

    private JFrame frame;
	private DefaultListModel testEntries;

	private JList testnames;
    private ProgressBar pb;
    
    private CounterPanel cp;
    private int errorCount, failureCount;
    private int testTotal; 
    private boolean doingMultiple;
        
    private FailureDetailView fdv;
    
    public TestDisplayFrame()
    {
        testTotal = 0;
        errorCount = 0;
        failureCount = 0;
        doingMultiple = false;

        createUI();
    }

    /**
     * Show or hide the test display window.
     */
    public void showTestDisplay(boolean doShow)
    {
		frame.setVisible(doShow);
    }

    /**
     * Return true if the window is currently displayed.
     */
    public boolean isShown()
    {
        return frame.isShowing();
    }


    
    protected void createUI()
    {
		frame = new JFrame(Config.getString("testdisplay.title"));

		frame.getContentPane().setLayout(new BoxLayout(frame.getContentPane(),BoxLayout.Y_AXIS));

		testEntries = new DefaultListModel();
		
		JScrollPane jsp = new JScrollPane();
		{
			testnames = new JList(testEntries);
			testnames.setCellRenderer(new MyCellRenderer());

			testnames.addListSelectionListener(new MyListSelectionListener());
						
			jsp.setViewportView(testnames);
		}
		
		frame.getContentPane().add(jsp);
        frame.getContentPane().add(pb=new ProgressBar());
        frame.getContentPane().add(cp=new CounterPanel());
        
        fdv = new FailureDetailView();
        
        frame.getContentPane().add(new JScrollPane(fdv.getComponent()));

        frame.pack();
    }

    protected void reset()
    {
        testEntries.clear();
        
        errorCount = 0;
        failureCount = 0;
        testTotal = 0;   

        fdv.clear();
        pb.reset();
        cp.setTotal(0);       
    }
    
    /**
     * Indicate that we are starting a bunch of tests and that we
     * do not know how many total tests there will be. Each call to
     * startTest from now on will add on to the previous results,
     * until endMultipleTests() is called.
     */
    public void startMultipleTests()
    {
        doingMultiple = true;    

        reset();
    }
    
    public void endMultipleTests()
    {
        doingMultiple = false;
    }  

    /**
     * Tell the dialog we are about to start a test run.
     * 
     * @param num   the number of tests we will run
     */
	public void startTest(int num)
	{
        if (doingMultiple) {
            testTotal += num;
        }
        else {
            reset();
            
            testTotal = num;
        }

        cp.setTotal(testTotal);
        pb.setmaximum(testTotal);	
	}

    /**
     * Add a test result to the test displayer.
     * 
     * @param dtr
     */	
	public void addResult(DebuggerTestResult dtr)
	{
        addResultQuietly(dtr);
        
		showTestDisplay(true);
	}

    /**
     * Add a test result to the test displayer but do not
     * bring the test display window to the front.
     * 
     * @param dtr
     */ 
    public void addResultQuietly(DebuggerTestResult dtr)
    {
        testEntries.addElement(dtr);
        
        cp.setRunValue(testEntries.getSize());
        pb.step(testEntries.getSize(), dtr.isSuccess());
        
        if (!dtr.isSuccess())
            cp.setErrorValue(++errorCount);
    }



	class MyListSelectionListener implements ListSelectionListener
	{
		public void valueChanged(ListSelectionEvent e)
		{
			if (testnames.getSelectedValue() != null) {
				DebuggerTestResult dtr = (DebuggerTestResult) testnames.getSelectedValue();

				if (dtr.isError()) {
					fdv.showFailure(dtr.getExceptionMessage() + "\n---\n" + dtr.getTrace());
				} else
					fdv.clear();		
			}
		}
	}
}

class MyCellRenderer extends JLabel implements ListCellRenderer
{
	final static Icon errorIcon = Config.getImageAsIcon("image.testmgr.error");
	final static Icon okIcon = Config.getImageAsIcon("image.testmgr.ok");

	// This is the only method defined by ListCellRenderer.
	// We just reconfigure the JLabel each time we're called.
	public Component getListCellRendererComponent(
	  JList list,
	  Object value,            // value to display
	  int index,               // cell index
	  boolean isSelected,      // is the cell selected
	  boolean cellHasFocus)    // the list and the cell have the focus
	{
		if (value instanceof DebuggerTestResult) {
			DebuggerTestResult dtr = (DebuggerTestResult) value;
			
			setText(dtr.getName());
			setIcon((dtr.isSuccess()) ? okIcon : errorIcon);
			
		} else
			setText(value.toString());

		if (isSelected) {
			setBackground(list.getSelectionBackground());
			setForeground(list.getSelectionForeground());
		}
		else {
			setBackground(list.getBackground());
			setForeground(list.getForeground());
		}
		setEnabled(list.isEnabled());
		setFont(list.getFont());
		setOpaque(true);

		return this;
	}
}
