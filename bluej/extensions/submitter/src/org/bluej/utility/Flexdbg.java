package org.bluej.utility;


import java.io.PrintStream;

/**
 *  Description of the Class
 */
public class Flexdbg
{
    private int serviceMask;
    private int debugLevel;
    private PrintStream outStream;
    private String msgPrefix;
    // If you need to prepend always something... can be null

    /**
     * At this level we see the program flow, that is what function gets called
     * So, use this whan you want to trace where your program goes.
     */
    public final static int TRACE = 1;

    /**
     * At this level you see the VALUES of variables, of course of what you think is interesting to see
     * Remembar, this is YOU that put some code tryng to think what should be interesting to know.
     */
    public final static int DEBUG = 2;

    /**
     * You should use this level to report events that are unusual, something absolutely legal
     * but unusual, classic example is someone typing the wrong password...
     */
    public final static int NOTICE = 3;

    /**
     * Use this level when something goes WRONG (unexpected input or result) BUT you can recover it.
     * Maybe recovering it means asching the user something else, or deleting and redoing something
     * but at the end the function result should be OK
     */
    public final static int ERROR = 4;

    /**
     * The final one, when an event like this happens you reaport it and then usually
     * everything dies off. So, tipically the normal level of debugging is either NOTICE or ERROR
     * and by doing this you get all NOTICE->ERROR->FATAL reporting.
     * When you want to debug you set it to TRACE or just DEBUG.
     */
    public final static int FATAL = 5;

    /**
     * Well, services are really defined by somebody else. there is a service that is ALL
     */
    public final static int ALL_SERVICES = 0xffffffff;


    /**
     * I need the constructo just to have some meaningful values.
     * Especially I do NOT want to worry about outStream being null, othervise I will need
     * to put lots of try/catch or have to deal with errors on the debugger :-)
     * NOTE: This does NOT needs to be a singleton. You may want to have more than one
     * in your program and to send output to different places.
     * The way to use it is to share the instance to the classes you wish or desire.
     */
    public Flexdbg()
    {
        serviceMask = 0xffffffff;
        debugLevel = NOTICE;
        outStream = System.err;
        msgPrefix = null;
    }


    /**
     * This sets the stream where I want to write to.
     * NOTE: If I am using this class from various threads there are Sync problems.
     * Especially if I change the output while I am writing...
     * I think I cannot bear the weight of a syncronization... SO
     * if you use this YOU must be shure that you call it when nobody else is using
     *
     * @param  i_outStream  The new output value
     */
    public void setOutput(PrintStream i_outStream)
    {
        if (i_outStream == null) {
            error(ALL_SERVICES, "Flexdbg.setOutput: You cannot set a NULL outStream");
            return;
        }

        outStream = i_outStream;
    }


    /**
     * Setting the debug level result for all messages that are above or equal the current level
     * to be printed out. Of course there is a match with the service too ....
     *
     * @param  i_debugLevel  The new debugLevel value
     * @return               Description of the Return Value
     */
    public int setDebugLevel(int i_debugLevel)
    {
        return debugLevel = i_debugLevel;
    }


    /**
     * Use this one to set the serviceMask, this means that ONLY events that match
     * the current service mask WILL be reported. ALL the oters WILL be zapped !
     *
     * @param  i_serviceMask  The new serviceMask value
     * @return                Description of the Return Value
     */
    public int setServiceMask(int i_serviceMask)
    {
        return serviceMask = i_serviceMask;
    }


    /**
     * If you need to ALWAYS prepend some string you can use this.
     * If you set it to null nothing will be prepended.
     *
     * @param  i_msgPrefix  The new msgPrefix value
     */
    public void setMsgPrefix(String i_msgPrefix)
    {
        msgPrefix = i_msgPrefix;
    }


    /**
     * To avoid writing the same code over and over.
     *
     * @param  serviceId  Description of the Parameter
     * @param  message    Description of the Parameter
     */
    private void doPrint(int serviceId, String message)
    {
        if ((serviceId & serviceMask) == 0)
            return;

        // TIme to finally do some printing.
        if (msgPrefix != null)
            outStream.print(msgPrefix);
        outStream.println(message);
    }


    /**
     * What I want it to PRINT if the request is greater or equal of the current
     * if ( TRACE >= debugLevel ) print this becomes
     * if ( debugLevel > TRACE ) nothing to do
     *
     * @param  serviceId  Description of the Parameter
     * @param  message    Description of the Parameter
     */
    public void trace(int serviceId, String message)
    {
        if (debugLevel > TRACE)
            return;
        doPrint(serviceId, message);
    }


    /**
     *  Description of the Method
     *
     * @param  serviceId  Description of the Parameter
     * @param  message    Description of the Parameter
     */
    public void debug(int serviceId, String message)
    {
        if (debugLevel > DEBUG)
            return;
        doPrint(serviceId, message);
    }


    /**
     *  Description of the Method
     *
     * @param  serviceId  Description of the Parameter
     * @param  message    Description of the Parameter
     */
    public void notice(int serviceId, String message)
    {
        if (debugLevel > NOTICE)
            return;
        doPrint(serviceId, message);
    }


    /**
     *  Description of the Method
     *
     * @param  serviceId  Description of the Parameter
     * @param  message    Description of the Parameter
     */
    public void error(int serviceId, String message)
    {
        if (debugLevel > ERROR)
            return;
        doPrint(serviceId, message);
    }


    /**
     *  Description of the Method
     *
     * @param  serviceId  Description of the Parameter
     * @param  message    Description of the Parameter
     */
    public void fatal(int serviceId, String message)
    {
        if (debugLevel > FATAL)
            return;
        doPrint(serviceId, message);
    }
}
