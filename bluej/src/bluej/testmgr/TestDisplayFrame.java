package bluej.testmgr;

import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.InvocationTargetException;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.debugger.DebuggerTestResult;
import bluej.debugger.SourceLocation;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.Project;
import bluej.utility.JavaNames;

/**
 * A Swing based user interface to run tests.
 *
 * @author  Andrew Patterson
 * @version $Id: TestDisplayFrame.java 2938 2004-08-24 02:11:19Z davmac $
 */
public class TestDisplayFrame
{
    // -- static singleton factory method --

    static TestDisplayFrame singleton = null;

    public synchronized static TestDisplayFrame getTestDisplay()
    {
        if(singleton == null)
            singleton = new TestDisplayFrame();
        return singleton;
    }

    public static boolean isFrameShown()
    {
        if(singleton == null)
            return false;
        else
            return singleton.isShown();
    }

    private JFrame frame;
	private DefaultListModel testEntries;

	private JList testnames;
    private ProgressBar pb;
    private GridBagConstraints pbConstraints;
    private JPanel statusLabel;
    private JPanel topPanel;
    
    // index of the progress bar in the topPanel's components
    private final static int PROGRESS_BAR_INDEX = 2;
    
    private CounterPanel cp;
    private int errorCount, failureCount;
    private int testTotal; 
    private boolean doingMultiple;
        
    // private FailureDetailView fdv;
    private JTextArea exceptionMessageField;
    private JButton showSourceButton;
    
    private Project lastProject;
    
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
    
    /**
     * Create the user-interface for the error display dialog.
     */
    protected void createUI()
    {
		frame = new JFrame(Config.getString("testdisplay.title"));

		frame.setIconImage(BlueJTheme.getIconImage());
		frame.setLocation(Config.getLocation("bluej.testdisplay"));

		// save position when window is moved
		frame.addComponentListener(new ComponentAdapter() {
				public void componentMoved(ComponentEvent event)
				{
					Config.putLocation("bluej.testdisplay", frame.getLocation());
				}
			});
		
		topPanel = new JPanel();
		{
			topPanel.setBorder(BlueJTheme.generalBorder);
			//topPanel.setLayout(new BoxLayout(topPanel,BoxLayout.Y_AXIS));
            topPanel.setLayout(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
					
			JScrollPane jsp = new JScrollPane();
			{
				testEntries = new DefaultListModel();
				
				testnames = new JList(testEntries);
				testnames.setCellRenderer(new MyCellRenderer());
				testnames.addListSelectionListener(new MyListSelectionListener());
                testnames.addMouseListener(new ShowSourceListener());
							
				jsp.setViewportView(testnames);
			}
		
			c.fill = GridBagConstraints.BOTH;
            c.weightx = 1.0;
            c.weighty = 1.0;
            c.gridx = 0;
            topPanel.add(jsp,c);
            
            c.weighty = 0;
            topPanel.add(Box.createVerticalStrut(BlueJTheme.generalSpacingWidth), c);
	        topPanel.add(pb=new ProgressBar(), c);
            topPanel.add(Box.createVerticalStrut(BlueJTheme.generalSpacingWidth), c);
	        topPanel.add(cp=new CounterPanel(), c);
            topPanel.add(Box.createVerticalStrut(BlueJTheme.generalSpacingWidth), c);
        
            // exception message field (text area)
            exceptionMessageField = new JTextArea("");
            exceptionMessageField.setEditable(false);
            Border x = new CompoundBorder(new LineBorder(Color.BLACK, 1),
                    new EmptyBorder(2,2,2,2));
            exceptionMessageField.setBorder(x);
            exceptionMessageField.setRows(2);
            exceptionMessageField.setLineWrap(true);
            exceptionMessageField.setFocusable(false);
            
            Dimension size = exceptionMessageField.getPreferredSize();
            size.width = exceptionMessageField.getMaximumSize().width;
            exceptionMessageField.setPreferredSize(size);
            size.width = exceptionMessageField.getMinimumSize().width;
            exceptionMessageField.setMinimumSize(size);

            // "show source" and "close" buttons
            showSourceButton = new JButton(Config.getString("testdisplay.showsource"));
            showSourceButton.addActionListener(new ShowSourceListener());
            
            JButton closeButton = new JButton(Config.getString("close"));
            closeButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    frame.setVisible(false);
                }
            });
            
	        // Panel for "show source" and "close" buttons
            JPanel buttonPanel = new JPanel();
            buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
            buttonPanel.add(showSourceButton);
            buttonPanel.add(Box.createHorizontalGlue());
            buttonPanel.add(closeButton);
            
            c.weighty = .1;
            topPanel.add(exceptionMessageField, c);
            c.weighty = 0;
            topPanel.add(Box.createVerticalStrut(BlueJTheme.generalSpacingWidth), c);
            topPanel.add(buttonPanel, c);

            c.gridy = PROGRESS_BAR_INDEX;
            pbConstraints = c;
        }
        
		frame.getContentPane().add(topPanel);
        frame.pack();
    }

    protected void reset()
    {
        testEntries.clear();
        
        errorCount = 0;
        failureCount = 0;
        testTotal = 0;   

        exceptionMessageField.setText("");
        showSourceButton.setEnabled(false);
        pb.reset();
        cp.setTotal(0);
        cp.setErrorValue(0);
        cp.setFailureValue(0);
        
        topPanel.remove(PROGRESS_BAR_INDEX);
        topPanel.add(pb, pbConstraints, PROGRESS_BAR_INDEX);
        topPanel.validate();
        pb.repaint();
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
        
        try {
            EventQueue.invokeAndWait(new Runnable() {
                public void run() {
                    setResultLabel();
                }
            });
        }
        catch(InvocationTargetException ite) { }
        catch(InterruptedException ie) { }
    }  

    /**
     * Tell the dialog we are about to start a test run.
     * 
     * @param num   the number of tests we will run
     */
	public void startTest(Project project, int num)
	{
        lastProject = project;
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
        
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                showTestDisplay(true);
            }
        });
	}

    /**
     * Add a test result to the test displayer but do not
     * bring the test display window to the front.
     * 
     * @param dtr
     */ 
    public void addResultQuietly(final DebuggerTestResult dtr)
    {
        if (!dtr.isSuccess()) {
            if (dtr.isFailure())
                ++failureCount;
            else
                ++errorCount;
        }

        try {
        EventQueue.invokeAndWait(new Runnable() {
            public void run() {
                testEntries.addElement(dtr);
                pb.step(testEntries.getSize(), dtr.isSuccess());

                cp.setFailureValue(failureCount);
                cp.setErrorValue(errorCount);
                cp.setRunValue(testEntries.getSize());
                
                if (!doingMultiple && pb.getValue() == pb.getMaximum())
                    setResultLabel();
            }
        });
        }
        catch(InvocationTargetException ite) { }
        catch(InterruptedException ie) { }
    }
    
    /**
     * Change the progress bar into a red or green label, depending on
     * success/failure status. Should be called on the swing event thread.
     */
    private void setResultLabel()
    {
        statusLabel = new JPanel();

        if (errorCount + failureCount == 0)
            statusLabel.setBackground(Color.GREEN);
        else
            statusLabel.setBackground(Color.RED);

        statusLabel.setMinimumSize(pb.getMinimumSize());
        statusLabel.setMaximumSize(pb.getMaximumSize());
        statusLabel.setPreferredSize(pb.getSize());
        statusLabel.setOpaque(true);
        topPanel.remove(PROGRESS_BAR_INDEX);
        topPanel.add(statusLabel, pbConstraints, PROGRESS_BAR_INDEX);
        topPanel.validate();
        statusLabel.repaint();
    }

	class MyListSelectionListener implements ListSelectionListener
	{
		public void valueChanged(ListSelectionEvent e)
		{
			if (testnames.getSelectedValue() != null) {
				DebuggerTestResult dtr = (DebuggerTestResult) testnames.getSelectedValue();

				if (dtr.isError() || dtr.isFailure()) {
					// fdv.showFailure(dtr.getExceptionMessage() + "\n---\n" + dtr.getTrace());
                    if (dtr.isError()) {
                        String text = dtr.getTrace();
                        int index = text.indexOf('\n');
                        if (index == -1)
                            index = text.length();
                        exceptionMessageField.setText(text.substring(0, index));
                    }
                    else
                        exceptionMessageField.setText(dtr.getExceptionMessage());
                    // This puts in the stack trace as well
                    //exceptionMessageField.setText(exceptionMessageField.getText()
                    //        + "\n---\n" + dtr.getTrace());
                    exceptionMessageField.setCaretPosition(0);
                    if (dtr.getExceptionLocation() != null)
                        showSourceButton.setEnabled(true);
				} else {
                    exceptionMessageField.setText("");
                    showSourceButton.setEnabled(false);
                }
			}
		}
	}
    
    class ShowSourceListener extends MouseAdapter implements ActionListener
    {
        public void mouseClicked(MouseEvent e)
        {
            int cc = e.getClickCount();
            if (cc == 2) {
                showSource();
            }
        }
        
        public void actionPerformed(ActionEvent e)
        {
            showSource();
        }
        
        private void showSource()
        {
            DebuggerTestResult dtr = (DebuggerTestResult) testnames.getSelectedValue();
            if (dtr != null && (dtr.isError() || dtr.isFailure())) {
                SourceLocation exceptionLocation = dtr.getExceptionLocation();
                String packageName = JavaNames.getPrefix(exceptionLocation.getClassName());

                Package spackage = lastProject.getExistingPackage(packageName);
                if (spackage == null)
                    return;

                // We have the package name. Now get the source name and
                // line number.
                String sourceName = exceptionLocation.getFileName();
                int lineno = exceptionLocation.getLineNumber();

                spackage.showSource(sourceName, lineno, "", false);
            }
        }
    };

}

class MyCellRenderer extends JLabel implements ListCellRenderer
{
	final static Icon errorIcon = Config.getImageAsIcon("image.testmgr.error");
	final static Icon failureIcon = Config.getImageAsIcon("image.testmgr.failure");
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
			setIcon((dtr.isSuccess()) ? okIcon : (dtr.isFailure() ? failureIcon : errorIcon));
			
		} else
			setText(value.toString());

		if (isSelected) {
			setBackground(list.getSelectionBackground());
			setForeground(list.getSelectionForeground());
            setOpaque(true);
		}
		else {
			setBackground(list.getBackground());
			setForeground(list.getForeground());
            setOpaque(false);
		}
		setEnabled(list.isEnabled());
		setFont(list.getFont());

		return this;
	}
}

