package bluej.testmgr;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;
import junit.framework.*;

import bluej.Config;
import bluej.debugger.*;

/**
 * A Swing based user interface to run tests.
 * Enter the name of a class which either provides a static
 * suite method or is a subclass of TestCase.
 * <pre>
 * Synopsis: java junit.swingui.TestRunner [-noloading] [TestCase]
 * </pre>
 * TestRunner takes as an optional argument the name of the testcase class to be run.
 */
public class TestDisplayFrame
{
    static final String windowTitle = Config.getString("terminal.title");
    static final int windowHeight = Config.getPropInteger("bluej.terminal.height", 22);
    static final int windowWidth = Config.getPropInteger("bluej.terminal.width", 80);

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
    private ProgressBar pb;
    private CounterPanel cp;
    private FailureDetailView fdv;
    
    public TestDisplayFrame()
    {
        createUI();
    }

    /**
     * Show or hide the test display window.
     */
    public void showTestDisplay(boolean doShow)
    {
	frame.setVisible(doShow);
	if(doShow) {
//	    text.requestFocus();
	}
    }


    /**
     * Return true if the window is currently displayed.
     */
    public boolean isShown()
    {
        return frame.isShowing();
    }


    
    public void createUI()
    {
	frame = new JFrame(Config.getString("testdisplay.title"));

	frame.getContentPane().setLayout(new BoxLayout(frame.getContentPane(),BoxLayout.Y_AXIS));

        frame.getContentPane().add(pb=new ProgressBar());
        frame.getContentPane().add(cp=new CounterPanel());
        
        fdv = new FailureDetailView();
        
        frame.getContentPane().add(new JScrollPane(fdv.getComponent()));

        frame.pack();
    }

    public void setResult(DebuggerTestResult dtr)
    {
        pb.start(dtr.runCount());
       
        cp.setTotal(dtr.runCount());

        for(int i=0; i<=dtr.runCount(); i++) {
            cp.setRunValue(i);
            pb.step(i, (dtr.errorCount() == 0 && dtr.failureCount() == 0));
        }

        cp.setErrorValue(dtr.errorCount());
        cp.setFailureValue(dtr.failureCount());

        if (dtr.failureCount() > 0) {
            fdv.showFailure(dtr.getFailure(0));
        }
    }
}
