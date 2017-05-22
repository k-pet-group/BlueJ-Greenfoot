/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2014  Michael Kolling and John Rosenberg 
 
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

import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.debugger.DebuggerObject;
import bluej.debugger.gentype.JavaType;
import bluej.debugmgr.objectbench.ObjectBench;
import bluej.debugmgr.objectbench.ObjectWrapper;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.utility.JavaNames;

/**
 * Records a single user interaction with the 
 * method call mechanisms of BlueJ.
 * 
 * This record is for method calls that return a result.
 *
 * @author  Andrew Patterson
 */
public class MethodInvokerRecord extends VoidMethodInvokerRecord
{
    private JavaType returnType;
    private String benchType;
    protected String benchName;
    
    /** How many times has this record been used. */
    private int usageCount;
    
    /** Has the method call been initialised? */
    private boolean methodCallInited = false;
    
    /**
     * Records a method call that returns a result to the user.
     * 
     * @param returnType  the Class of the return type of the method
     * @param command     the method statement to execute
     */
    @OnThread(Tag.FXPlatform)
    public MethodInvokerRecord(JavaType returnType, String command, String [] argumentValues)
    {
        super(command, argumentValues);
    
        this.returnType = returnType;
        this.benchType = returnType.toString(false);
        this.benchName = null;
    }
    
    @Override
    public boolean hasVoidResult()
    {
        return false;
    }

    /**
     * Give this method invoker record a name on the object
     * bench (the user has done a "Get" on the result). The type
     * is the type that the object is on the actual bench.
     * 
     * @param name
     * @param type
     */
    @Override
    public void setBenchName(String name, String type)
    {
        benchName = name;
        benchType = type;
    }

    /**
     * Construct a declaration for any objects constructed
     * by this invoker record.
     * 
     * @return a String representing the object declaration
     *         src or null if there is none.
     */
    @Override
    public String toFixtureDeclaration(String firstIndent)
    {
        // if it hasn't been assigned a name there is nothing to do for
        // fixture declaration
        if (benchName == null) {
            return null;
        }

        // declare the variable
        StringBuffer sb = new StringBuffer();
        sb.append(firstIndent);
        sb.append(fieldDeclarationStart);
        sb.append(benchDeclaration());
        sb.append(benchName);
        sb.append(statementEnd);

        return sb.toString();
    }
    
    /**
     * Construct a portion of an initialisation method for
     * this invoker record.
     *  
     * @return a String reprenting the object initialisation
     *         src or null if there is none. 
     */
    @Override
    @OnThread(Tag.FXPlatform)
    public String toFixtureSetup(String secondIndent)
    {
        if (benchName == null) {
            return secondIndent + command + statementEnd;
        }

        StringBuffer sb = new StringBuffer();
        sb.append(secondIndent);
        sb.append(benchAssignmentTypecast());
        sb.append(statementEnd);

        return sb.toString();
    }

    /*
     * @see bluej.testmgr.record.VoidMethodInvokerRecord#toTestMethod(bluej.pkgmgr.PkgMgrFrame)
     */
    @Override
    public String toTestMethod(PkgMgrFrame pmf, String secondIndent)
    {
        StringBuffer sb = new StringBuffer();
        sb.append(toTestMethodInit(pmf, secondIndent));

        String resultRef = toExpression();

        // with no uses of the result, just invoke the method.
        if (getUsageCount() == 0) {
            sb.append(secondIndent + resultRef + statementEnd);
        }
        else {
            // here are all the assertions
            for (int i = 0; i < getAssertionCount(); i++) {
                sb.append(secondIndent);
                sb.append(insertCommandIntoAssertionStatement(getAssertion(i), resultRef));
                sb.append(statementEnd);
            }
        }

        return sb.toString();
    }

    /**
     * Do any initialisation needed for creating the test method. This will set
     * up local variables if the result of the method is used more than once or
     * placed on the bench by using "Get".
     */
    @OnThread(Tag.FXPlatform)
    private String toTestMethodInit(PkgMgrFrame pkgMgrFrame, String secondIndent)
    {
        // If we have already prepared the method call, we return the name that
        // references it.
        if (methodCallInited) {
            return "";
        }

        // Method result has not been put on the bench by using "Get".
        if (benchName == null) {
            if (getUsageCount() > 1) {
                // If the method result is not "Get" onto the bench, and we use the
                // method result more than once, we need to put it on the bench to
                // give it a unique name.
                DebuggerObject result = getResultObject();
                ObjectBench bench = pkgMgrFrame.getObjectBench();
                ObjectWrapper wrapper = ObjectWrapper.getWrapper(pkgMgrFrame, bench, result, result.getGenType(),
                "result");
                bench.addObject(wrapper); // might change name
                benchName = wrapper.getName();            
            }
            else {
                // Nothing to prepare
                return "";
            }
        }
        else {
            // We used "Get" on the result, so increase usage count.
            incUsageCount();
        }

        methodCallInited = true;
        // assign result to a local variable with the given benchName.
        return secondIndent + benchDeclaration() + benchAssignmentTypecast() + statementEnd;
    }

    /**
     * This will return a string containing a reference to the method result.
     * Either as the command itself, or the name of a local variable containing
     * the result.
     * 
     * @return Reference to the method result
     */
    @Override
    public String toExpression()
    {
        assert (methodCallInited);

        // Method result has not been put on the bench by using "Get".
        if (benchName == null) {
            return command;
        }
        return benchName;
    }

    /**
     * @return A string representing the type name of an object
     */
    private String benchDeclaration()
    {
        return JavaNames.typeName(benchType) + " ";
    }

    /**
     * @return A string representing the assignment statement
     *         with an optional typecast to get the type correct
     */
    @OnThread(Tag.FXPlatform)
    protected String benchAssignmentTypecast()
    {
        StringBuffer sb = new StringBuffer();

        sb.append(benchName);
        sb.append(" = ");

        // check if a typecast is required
        if (!benchType.equals(returnType.toString(false))) {
            sb.append("(");
            sb.append(benchType);
            sb.append(")");
        }

        sb.append(command);

        return sb.toString();
    }

    @Override
    public void addAssertion(String assertion)
    {
        super.addAssertion(assertion);
        usageCount++;        
    }

    /**
     * Call when using this invoker record as a parent for another invoker
     * record. Increases usage count.
     */
    public void incUsageCount()
    {
        usageCount++;
    }

    /**
     * Get the number of times the result of this record is used (by another record,
     * or by an assertion).
     */
    private int getUsageCount()
    {
        return usageCount;
    }    
}
