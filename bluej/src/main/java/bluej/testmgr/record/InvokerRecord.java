/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011,2012,2014,2015  Michael Kolling and John Rosenberg 
 
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
package bluej.testmgr.record;

import java.util.ArrayList;

import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.debugger.DebuggerObject;
import bluej.pkgmgr.PkgMgrFrame;

/**
 * Records a single user interaction with the object
 * construction/method call mechanisms of BlueJ.
 * 
 * Also contains static methods to help deal with the
 * construction and maintenance of assertion data.
 *
 * @author  Andrew Patterson
 */
public abstract class InvokerRecord
{
    final static String statementEnd = ";\n";
    
    final static String fieldDeclarationStart = "private ";

    // -------------- instance fields ----------------
    
    /**
     * A collection of assertion skeletons made about the invoker
     * record.
     */
    private ArrayList<String> assertions = new ArrayList<String>();
    private DebuggerObject resultObject;
    
    private static int nextUniqueIdentifier = 1;
    private final int uniqueIdentifier;
    
    // -------------- instance methods -----------------
    
    public InvokerRecord()
    {
        uniqueIdentifier = nextUniqueIdentifier++;
    }
    
    /**
     * If this invoker record represents a method or constructor call,
     * this method returns the argument values used in the call.
     * Otherwise it returns null.
     */
    public String [] getArgumentValues()
    {
        return null;
    }
    
    /**
     * If the result of this invocation is to be consigned to the object bench, get
     * its name (otherwise returns null).
     */
    public String getResultName()
    {
        return null;
    }
    
    /**
     * Get the (static, compile time) result type of the invocation, if known. 
     */
    public String getResultTypeString()
    {
        return null;
    }
    
    /**
     * Check whether this record represents an invocation that has no result.
     */
    public abstract boolean hasVoidResult();
    
    /**
     * Construct a declaration for any objects constructed
     * by this invoker record.
     * 
     * @return a String representing the object declaration
     *         src or null if there is none.
     */    
    public abstract String toFixtureDeclaration(String firstIndent);

    /**
     * Construct a portion of an initialisation method for
     * this invoker record.
     *  
     * @return a String reprenting the object initialisation
     *         src or null if there is none. 
     */
    @OnThread(Tag.FXPlatform)
    public abstract String toFixtureSetup(String secondIndent);

    /**
     * Construct a portion of a test method for this
     * invoker record.
     * 
     * @return a String representing the test method src
     */
    @OnThread(Tag.FXPlatform)
    public abstract String toTestMethod(PkgMgrFrame pmf, String secondIndent);

    /**
     * Construct an expression. This is an open expression which is not ended by
     * by semicolon or line breaks. It is typically used by other InvokerRecords
     * to created "chained" invocation records. The returned string should not
     * include any indentation.
     * 
     * @return a String representing the expression (or null if this record cannot
     *         be represented as an expression)
     */
    public abstract String toExpression();
    
    /**
     * Construct a statement. This returns a Java-language string which should be
     * appropriately terminated by a semicolon.
     * 
     * @return  A String representing the statement (or null if this record cannot
     *          be used as a statement)
     */
    public String toStatement()
    {
        return toExpression() + ";";
    }
    
    /**
     * Set the name of this result as saved on the object bench.
     * @param benchName  The name of the object (on the Object Bench).
     * @param benchType  The type of the object as known to the bench.
     */
    public void setBenchName(String benchName, String benchType)
    {
        // By default do nothing.
    }
    
    /**
     * Add the skeleton of an assertion statement to our list of
     * assertions made about this invoker record.
     * 
     * @param assertion
     */
    public void addAssertion(String assertion)
    {
        assertions.add(assertion);  
    }
    
    public int getAssertionCount()
    {
        return assertions.size();
    }
    
    public String getAssertion(int i)
    {
        return (String) assertions.get(i);
    }
    
    /**
     * Returns a statement representing this assertion
     * with an @@ at the point where code needs to be
     * inserted.
     * 
     * This case is for when there is only a single argument
     * for the assertion.

     * @return a String of the assertion statement.
     */
    public static String makeAssertionStatement(String assertName)
    {
        StringBuffer sb = new StringBuffer();
        sb.append(assertName);
        sb.append("(@@)");
        return sb.toString();
    }

    /**
     * Returns a statement representing this assertion
     * with an @@ at the point where code needs to be
     * inserted.
     * 
     * This case is for when there are two arguements
     * for the assertion.
     * 
     * For example, if the user has selected equals and
     * has put a value of
     * 
     * new X(a,b)
     * 
     * in the edit box, we return the string
     * 
     * "assertEquals(new X(a,b), @@)"  
     *  
     * @return a String of the assertion statement.
     * 
     * There is a danger here if the characters @@ appear
     * in the user input. This is considered unlikely, but
     * when searching for the @@ location, the _last_ @@
     * in this string should be found.
     */
    public static String makeAssertionStatement(String assertName, String userData)
    {
        StringBuffer sb = new StringBuffer();
        sb.append(assertName);
        sb.append("(");
        sb.append(userData);
        sb.append(", @@)");
        return sb.toString();    
    }

    /**
     * Returns a statement representing this assertion
     * with an @@ at the point where a statement needs to be
     * inserted.
     * 
     * This case is for when we have floating point or
     * double assertions with assertEquals()
     *
     * @return a String of the assertion statement.
     */
    public static String makeAssertionStatement(String assertName, String userData, String deltaData)
    {
        StringBuffer sb = new StringBuffer();
        sb.append(assertName);
        sb.append("(");
        sb.append(userData);
        sb.append(", @@, ");
        sb.append(deltaData);
        sb.append(")");
        return sb.toString();    
    }

    /**
     * Insert a command into an assertion.
     * 
     * Our assertions have an @@ wherever the command needs to go. We
     * find this and insert the string there.
     * 
     * @param  assertion   a String representing the assertion in the
     *                     form "assertXXXX(YYYY, @@)"
     * @param  command     a String representing the statement to make the
     *                     assertion against ie foo.bar(5,6) 
     * @return the combined string representing original assertion with the 
     *         insertion of the command or reference to bench object.
     *         ie assertXXXX(YYYY, foo.bar(5,6)) 
     */
    public static String insertCommandIntoAssertionStatement(String assertion, String command)
    {
        StringBuffer assertCommand = new StringBuffer(assertion);
        // search for the _last_ @@ in case the user has input some of these
        // @@ characters in the assertion
        int insertionSpot = assertion.lastIndexOf("@@");

        if(insertionSpot == -1)
            throw new IllegalArgumentException("the assertion must have an @@");

        assertCommand.replace(insertionSpot, insertionSpot + 2, command);
            
        return assertCommand.toString();
    }

    /**
     * If a result is related to this invoker record, it can be set here. This
     * is mostly here for the MethodInvokerRecord, so that we can refer to the
     * object by a unique name, by adding it to the object bench.
     * 
     * @param resultObject Result object or null
     */
    public void setResultObject(DebuggerObject resultObject)
    {
        this.resultObject = resultObject;
    }
    
    /**
     * Get the result object.
     * 
     * @return The result object or null.
     */
    protected DebuggerObject getResultObject() 
    {
        return resultObject;
    }
    
    /**
     * Call when using this invoker record as a parent for another invoker
     * record. This implementation does nothing - overide for subclasses 
     * that need this.
     */
    public void incUsageCount()
    {
    }
    
    /**
     * Gets the original command.  Used by the Data Collection mechanism.
     */
    public String getOriginalCommand() { return null; }

    /**
     * Gets a unique identifier for this invoker.  For data collection purposes.
     */
    public int getUniqueIdentifier()
    {
        return uniqueIdentifier;
    }
}
