package bluej.testmgr;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;
import junit.framework.*;

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
    private JFrame frame;
    private ProgressBar pb;
    private CounterPanel cp;
    private FailureDetailView fdv;
    
    public TestDisplayFrame()
    {
        createUI();
    }
    
    public void createUI()
    {
		frame= new JFrame("BlueJ - JUnit");

		frame.getContentPane().setLayout(new BoxLayout(frame.getContentPane(),BoxLayout.Y_AXIS));

        frame.getContentPane().add(pb=new ProgressBar());
        frame.getContentPane().add(cp=new CounterPanel());
        
        fdv = new FailureDetailView();
        
        frame.getContentPane().add(new JScrollPane(fdv.getComponent()));

        frame.pack();
        
        frame.show();
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