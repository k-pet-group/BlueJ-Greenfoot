/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kšlling and John Rosenberg 
 
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
package bluej.extensions;

import bluej.debugger.DebuggerObject;
import bluej.debugmgr.ExpressionInformation;
import bluej.debugmgr.Invoker;
import bluej.debugmgr.ResultWatcher;
import bluej.debugmgr.objectbench.ObjectWrapper;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.testmgr.record.InvokerRecord;
import bluej.utility.Utility;
import bluej.views.CallableView;
import bluej.views.MethodView;

/**
 * Provides a gateway to invoke methods on objects using a specified set of parameters.
 *
 * @author     Damiano Bolla, University of Kent at Canterbury, 2003,2004
 * @author     Clive Miller, University of Kent at Canterbury, 2002
 *
 * @version    $Id: DirectInvoker.java 6163 2009-02-19 18:09:55Z polle $
 */

class DirectInvoker
{
    private final PkgMgrFrame pkgFrame;
    private final CallableView callable;
    private String resultName;


    /**
     * For use by the bluej.extensions
     *
     * @param  i_pkgFrame  Description of the Parameter
     * @param  i_callable  Description of the Parameter
     */
    DirectInvoker(PkgMgrFrame i_pkgFrame, CallableView i_callable)
    {
        pkgFrame = i_pkgFrame;
        callable = i_callable;
    }


    /**
     * Call this if you want to call a constructor
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
     * @param  args                             Arguments to the constructor
     * @return                                  The newly created object
     * @throws  InvocationArgumentException   if the arglist is not consistent with the signature
     * @throws  InvocationErrorException      if there is a system error
     */
    DebuggerObject invokeConstructor(Object[] args)
             throws InvocationArgumentException, InvocationErrorException
    {
        if (!paramsAlmostMatch(args, callable.getParameters())) {
            throw new InvocationArgumentException("invokeConstructor: bad arglist");
        }

        DirectResultWatcher watcher = new DirectResultWatcher();
        Invoker invoker = new Invoker(pkgFrame, callable, watcher);
        invoker.invokeDirect(convObjToString(args));

        // this will wait() on the invoke to finish
        DebuggerObject result = watcher.getResult();

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
     * @param  args                             The arguments for the method
     * @return                                  The newly created object
     * @exception  InvocationArgumentException  Thrown if the arglist is not consistent with the signature
     * @exception  InvocationErrorException     Thrown if there is a system error
     */
    DebuggerObject invokeMethod(ObjectWrapper onThisObjectInstance, Object[] args)
             throws InvocationArgumentException, InvocationErrorException
    {
        if (!paramsAlmostMatch(args, callable.getParameters())) {
            throw new InvocationArgumentException("invokeMethod: bad arglist");
        }

        DirectResultWatcher watcher = new DirectResultWatcher();
        Invoker invoker;
        if (((MethodView) callable).isStatic())
            invoker = new Invoker(pkgFrame, callable, watcher);
        else
            invoker = new Invoker(pkgFrame, (MethodView) callable, onThisObjectInstance, watcher);
        invoker.invokeDirect(convObjToString(args));

        // this will wait() on the invoke to finish
        DebuggerObject result = watcher.getResult();

        if (watcher.isFailed()) {
            throw new InvocationErrorException("invokeMethod: Error=" + watcher.getError());
        }

        // The "real" object is the first Field in this object.. BUT it is not always
        // an Object, it may be a primitive one...
        resultName = watcher.getResultName();
        return result;
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
//        Debug.message("convert="+convOneObj ( i_array[index] ));
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
    private boolean paramsAlmostMatch(Object[] params, Class[] paramClass)
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

        private DebuggerObject result;
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
            resultName = anObjectName;
            resultReady = true;
            notifyAll();
        }


        /**
         * This is used to return an error.
         *
         * @param  error  The error message
         */
        public synchronized void putError(String error)
        {
            errorMsg = "Invocation: Error=" + error;
            isFailed = true;
            resultReady = true;
            notifyAll();
        }
        

        /**
         * Treat run-time error the same as compile-time error.
         * 
         * @param  msg  The exception message
         */
        public void putException(String msg)
        {
            putError(msg);
        }
        
        
        /**
         * Treat termination as an error
         */
        public void putVMTerminated()
        {
            putError("Terminated");
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
         * Gets the expressionInformation attribute of the DirectResultWatcher object
         * @see bluej.debugmgr.ResultWatcher#getExpressionInformation()
         * @return    The expressionInformation value
         */
        public ExpressionInformation getExpressionInformation()
        {
            // TODO Auto-generated method stub
            return null;
        }
    }

}
