package bluej.tester;

import bluej.pkgmgr.Project;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import junit.swingui.TestRunner;
import junit.framework.*;

public class UnitTestTestSuiteLoader implements junit.runner.TestSuiteLoader
{
    private Project project;

    public UnitTestTestSuiteLoader(Project p)
    {
        if (p == null)
            throw new NullPointerException();

        project = p;
    }

	/**
	 * Uses the project class loader to load the test class
	 */
	public Class load(String suiteClassName) throws ClassNotFoundException
	{
		return project.loadClass(suiteClassName);
	}
	/**
	 * Project.loadClass() will handle reinitialising the class loader when the
	 * project gets recompiled
	 */
	public Class reload(Class aClass) throws ClassNotFoundException
	{
		return project.loadClass(aClass.getName());
	}
}
