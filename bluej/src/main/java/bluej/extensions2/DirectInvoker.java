/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2019  Michael Kolling and John Rosenberg
 
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
package bluej.extensions2;

import bluej.BlueJEvent;
import bluej.debugger.DebuggerObject;
import bluej.debugger.ExceptionDescription;
import bluej.debugmgr.ExecutionEvent;
import bluej.debugmgr.Invoker;
import bluej.debugmgr.ResultWatcher;
import bluej.debugmgr.objectbench.ObjectWrapper;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.testmgr.record.InvokerRecord;
import bluej.utility.Utility;
import bluej.views.CallableView;
import bluej.views.ConstructorView;
import bluej.views.MethodView;

/**
 * Provides a gateway to invoke methods on objects using a specified set of parameters.
 *
 * @author     Damiano Bolla, University of Kent at Canterbury, 2003,2004
 * @author     Clive Miller, University of Kent at Canterbury, 2002
 */
class DirectInvoker
{
    private final PkgMgrFrame pkgFrame;
    private String resultName;


    /**
     * @param  i_pkgFrame  A {@link PkgMgrFrame} object to be associated with this DirectInvoker.
     */
    DirectInvoker(PkgMgrFrame i_pkgFrame)
    {
        pkgFrame = i_pkgFrame;
    }


    /**
     * Extensions should call this method to invoke a constructor.
     *
     * <p>The arguments passed in the args array may have any type,
     * but the type will determine exactly what is passed to the
     * constructor:
     * 
     * <ul>
     * <li>String - the String will be passed directly to the constructor
     * <li>BObject - the object will be passed directly to the constructor,
     *               though it must be on the object bench for this to work
     * <li>Anything else - toString() is called on the object and the
     *               result is treated as a Java expression, which is
     *               evaluated and passed to the constructor.
     * </ul>
     * 
     * <p>An attempt is made to ensure that the argument types are suitable
     * for the constructor. InvocationArgumentException will be thrown if
     * the arguments are clearly unsuitable, however some cases will
     * generate an InvocationErrorException instead. In such cases no
     * expression arguments will be evaluated.
     *
     * @param  callable         a {@link ConstructorView} object referring to the constructor to invoke.
     * @param  args             an array of {@link Object} objets containing the arguments to the constructor.
     * @return                  A {@link DebuggerObject} containing referring to the object created by the invocation of the constructor.
     * @throws InvocationArgumentException   if the argument list is not consistent with the signature
     * @throws InvocationErrorException      if there is a system error
     */
    DebuggerObject invokeConstructor(ConstructorView callable, Object[] args)
             throws InvocationArgumentException, InvocationErrorException
    {
        if (!paramsAlmostMatch(args, callable.getParameters())) {
            throw new InvocationArgumentException("invokeConstructor: bad arglist");
        }

        DirectResultWatcher watcher = new DirectResultWatcher();
        Invoker invoker = new Invoker(pkgFrame, callable, watcher);
        String [] argStrings = convObjToString(args);
        invoker.invokeDirect(argStrings);

        // this will wait() on the invoke to finish
        DebuggerObject result = watcher.getResult();
        
        String resultType = watcher.getResultType();
        if (resultType != null) {
            ExecutionEvent ee = new ExecutionEvent(pkgFrame.getPackage(), callable.getClassName(), null);
            raiseEvent(ee, callable, argStrings, watcher);
        }

        if (watcher.isFailed()) {
            throw new InvocationErrorException("invokeConstructor: Error=" + watcher.getError());
        }

        if (result == null) {
            // This is most likely an error, but not of the sort above. Unlikely to happen
            throw new InvocationErrorException("invokeConstructor: ERROR: result==null");
        }

        resultName = watcher.getResultName();
        return result;
    }


    /**
     * Call a method on an object.
     * You need to pass the object where you want it applied.
     * 
     * <p>The arguments passed in the args array may have any type,
     * but the type will determine exactly what is passed to the
     * method:
     * 
     * <ul>
     * <li>String - the String will be passed directly to the method
     * <li>BObject - the object will be passed directly to the method,
     *               though it must be on the object bench for this to work
     * <li>Anything else - toString() is called on the object and the
     *               result is treated as a Java expression, which is
     *               evaluated and passed to the method.
     * </ul>
     * 
     * <p>An attempt is made to ensure that the argument types are suitable
     * for the method. InvocationArgumentException will be thrown if
     * the arguments are clearly unsuitable, however some cases will
     * generate an InvocationErrorException instead. In such cases no
     * expression arguments will be evaluated.
     *
     * @param  onThisObjectInstance             the method is called on this object
     * @param callable                          The method
     * @param  args                             The arguments for the method
     * @return                                  The result object; for a constructor call this is the
     *                                          constructed object; for any other invocation this will be
     *                                          an object with a 'result' field containing the actual result,
     *                                          or the null object (i.e. isNullObject() == true) if the result
     *                                          type is void.
     * @exception InvocationArgumentException  Thrown if the arglist is not consistent with the signature
     * @exception InvocationErrorException     Thrown if there is a system error
     */
    DebuggerObject invokeMethod(ObjectWrapper onThisObjectInstance, MethodView callable, Object[] args)
             throws InvocationArgumentException, InvocationErrorException
    {
        if (!paramsAlmostMatch(args, callable.getParameters())) {
            throw new InvocationArgumentException("invokeMethod: bad arglist");
        }

        DirectResultWatcher watcher = new DirectResultWatcher();
        Invoker invoker;
        if (callable.isStatic()) {
            invoker = new Invoker(pkgFrame, callable, watcher);
        }
        else {
            invoker = new Invoker(pkgFrame, (MethodView) callable, onThisObjectInstance.getName(), onThisObjectInstance.getObject(), watcher);
        }
        String [] argStrings = convObjToString(args);
        invoker.invokeDirect(convObjToString(args));

        // this will wait() on the invoke to finish
        DebuggerObject result = watcher.getResult();

        String resultType = watcher.getResultType();
        if (resultType != null) {
            ExecutionEvent ee = new ExecutionEvent(pkgFrame.getPackage(), callable.getClassName(),
                    (onThisObjectInstance==null)?null:onThisObjectInstance.getName());
            ee.setMethodName(callable.getName());
            raiseEvent(ee, callable, argStrings, watcher);
        }

        if (watcher.isFailed()) {
            throw new InvocationErrorException("invokeMethod: Error=" + watcher.getError());
        }

        // The "real" object is the first Field in this object.. BUT it is not always
        // an Object, it may be a primitive one...
        resultName = watcher.getResultName();
        return result;
    }

    /**
     * Raise an appropriate execution event, after a result has been received.
     */
    private static void raiseEvent(ExecutionEvent event, CallableView callable, String [] argStrings, 
            DirectResultWatcher watcher)
    {
        DebuggerObject result = watcher.getResult();
        String resultType = watcher.getResultType();
        
        event.setParameters(callable.getParamTypes(false), argStrings);
        event.setResult(resultType);
        if (resultType == ExecutionEvent.NORMAL_EXIT) {
            event.setResultObject(result);
            event.setObjectName(watcher.getResultName());
        }
        else if (resultType == ExecutionEvent.EXCEPTION_EXIT) {
            event.setException(watcher.getException());
        }
        
        BlueJEvent.raiseEvent(BlueJEvent.EXECUTION_RESULT, event);
    }

    /**
     * Returns the result object name of an invocation
     *
     * @return    The resultName value
     */
    String getResultName()
    {
        return resultName;
    }


    /**
     * Converts an array of Object into an array of String with java
     * expressions representing the objects, as per the convOneObj method.
     *
     * @param  i_array  Input object values
     * @return          Objects transformed into an array of strings
     */
    private String[] convObjToString(Object[] i_array)
    {
        if (i_array == null) {
            return null;
        }
        if (i_array.length <= 0) {
            return new String[0];
        }

        String[] o_array = new String[i_array.length];
        for (int index = 0; index < i_array.length; index++) {
            o_array[index] = convOneObj(i_array[index]);
        }

        return o_array;
    }


    /**
     * Does one conversion of a supplied object to a java expression
     * representing that object, according to the following rules:
     * 
     * <ul>
     * <li>String - the String will be quoted according to Java quoting
     *              rules, and enclosed in quotes
     * <li>BObject - the name of the BObject on the object bench will be
     *               returned 
     * <li>Anything else - toString() is called on the object and the
     *               result is treated as a Java expression, which is
     *               returned.
     * </ul>
     *
     * @param  i_obj  Input object to convert
     * @return        The resulting string representation
     */
    private String convOneObj(Object i_obj)
    {
        if (i_obj == null) {
            return null;
        }
        // A string should be quoted by a couple of "".
        if (i_obj instanceof String) {
            return "\"" + Utility.quoteString(i_obj.toString()) + "\"";
        }
        // An object reference is just the object instance name
        if (i_obj instanceof BObject) {
            return ((BObject) i_obj).getInstanceName();
        }
        // All the rest should be done by standard conversion...
        return i_obj.toString();
    }


    /**
     * Simple utility to decide when two params list do not match.
     * The test is done on params length but not on the type.
     *
     * @param  params      The params given
     * @param  paramClass  The reference params array
     * @return             true if they match, false othervise
     */
    private boolean paramsAlmostMatch(Object[] params, Class<?>[] paramClass)
    {
        // A zero len param or a null one are the same !
        if (params != null && params.length < 1) {
            params = null;
        }
        if (paramClass != null && paramClass.length < 1) {
            paramClass = null;
        }

        if (params == null && paramClass == null) {
            return true;
        }

        // If ANY of them is null we are in trouble now. (They MUST be both NOT null)
        if (params == null || paramClass == null) {
            return false;
        }

        // Now I know that BOTH are NOT empty. They MUST be the same length
        if (params.length != paramClass.length) {
            return false;
        }

        // Yes, they are almost the same, the actual type is missing :-)
        return true;
    }



    /**
     * This is used to interface with the core BlueJ
     * This new version does return when there is an INTERRUPT
     */
    class DirectResultWatcher implements ResultWatcher
    {
        private boolean resultReady;
        private boolean isFailed;
        private String resultType;

        private DebuggerObject result;
        private ExceptionDescription exception;
        private String errorMsg;
        // When there is a fail this is the reason.
        private String resultName;


        /**
         * Constructor for the DirectResultWatcher object
         */
        public DirectResultWatcher()
        {
            resultReady = false;
            isFailed = false;
            result = null;
            errorMsg = null;
        }


        /**
         * This will try to get the result of an invocation.
         * null can be returned if the thread is interrupted !!!
         *
         * @return    The result value
         */
        public synchronized DebuggerObject getResult()
        {
            while (!resultReady) {
                try {
                    wait();
                }
                catch (InterruptedException exc) {
                    // This is correct, if someone wants to get me out of this I should
                    // obey to the oreder !
                    isFailed = true;
                    errorMsg = "getResult: Interrupt: Exception=" + exc.getMessage();
                    return null;
                }
            }

            return result;
        }


        /**
         * I need a way to reliably detect if there is an error or not.
         * Careful... should I look for resultReady too ?
         *
         * @return    The failed value
         */
        public synchronized boolean isFailed()
        {
            return isFailed;
        }

        /*
         * @see bluej.debugmgr.ResultWatcher#beginExecution()
         */
        public void beginCompile()
        {
            // Nothing needs doing.
        }
        
        /*
         * @see bluej.debugmgr.ResultWatcher#beginExecution()
         */
        public void beginExecution(InvokerRecord ir)
        {
            // Nothing needs doing.
        }
        
        /**
         * Used to return a result. We know that it is a good one.
         *
         * @param  aResult       The actual result object
         * @param  anObjectName  The object name in the object bench
         * @param  ir            Further parameter, see ResultWatcher
         */
        public synchronized void putResult(DebuggerObject aResult, String anObjectName, InvokerRecord ir)
        {
            result = aResult;
            resultType = ExecutionEvent.NORMAL_EXIT;
            resultName = anObjectName;
            resultReady = true;
            notifyAll();
        }


        /**
         * This is used to return an error.
         * @param  error  The error message
         */
        public synchronized void putError(String error, InvokerRecord ir)
        {
            errorMsg = "Invocation: Error=" + error;
            isFailed = true;
            resultReady = true;
            notifyAll();
        }
        

        /**
         * Treat run-time error the same as compile-time error.
         * @param exception  The exception message
         * @param ir The invocation record
         */
        public synchronized void putException(ExceptionDescription exception, InvokerRecord ir)
        {
            this.exception = exception;
            resultType = ExecutionEvent.EXCEPTION_EXIT;
            putError(exception.getText(), ir);
        }
        
        
        /**
         * Treat termination as an error
         */
        public void putVMTerminated(InvokerRecord ir, boolean terminatedByUserCode)
        {
            resultType = terminatedByUserCode ? ExecutionEvent.NORMAL_EXIT : ExecutionEvent.TERMINATED_EXIT;
            putError("Terminated", ir);
        }


        /**
         *  Gets the error attribute of the DirectResultWatcher object
         *
         * @return    The error value
         */
        public String getError()
        {
            return errorMsg;
        }


        /**
         *  Gets the resultName attribute of the DirectResultWatcher object
         *
         * @return    The resultName value
         */
        public String getResultName()
        {
            return resultName;
        }
        
        /**
         * Returns the result type
         * ExecutionEvent.NORMAL_EXIT if execution completed normally;<br>
         * ExecutionEvent.EXCEPTION_EXIT if an exception occurred in user code;<br>
         * ExecutionEvent.TERMINATED_EXIT if the user VM exited for any reason;<br>
         * null if compilation failure occurred.
         */
        public String getResultType()
        {
            return resultType;
        }
        
        /**
         * Get the exception which occurred (if result type == EXCEPTION_EXIT).
         */
        public ExceptionDescription getException()
        {
            return exception;
        }
    }

}
