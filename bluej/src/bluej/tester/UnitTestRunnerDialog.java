package bluej.tester;

import bluej.Config;
import bluej.utility.Utility;
import bluej.utility.JavaNames;
import bluej.utility.DialogManager;
import bluej.pkgmgr.Project;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import junit.swingui.TestRunner;
import junit.runner.TestSuiteLoader;
import junit.framework.*;

/**
 * Unit Test dialog
 *
 * @author  Andrew Patterson
 * @version $Id: UnitTestRunnerDialog.java 1012 2001-11-30 01:26:31Z ajp $
 */
public class UnitTestRunnerDialog extends TestRunner
{
    private TestSuiteLoader loader;

    public UnitTestRunnerDialog(TestSuiteLoader loader)
    {
        super();

        this.loader = loader;

        fFrame = createUI("");
        fFrame.pack();
    }

    public void start(String[] args)
    {
        fFrame.setVisible(true);
    }

    public void terminate()
    {
        fFrame.dispose();
    }

    protected void createMenus(JMenuBar mb)
    {
    }

    public TestSuiteLoader getLoader()
    {
        return loader;
    }
}
