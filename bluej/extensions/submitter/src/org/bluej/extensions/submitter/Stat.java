package org.bluej.extensions.submitter;


import org.bluej.utility.*;
import bluej.extensions.*;

import org.bluej.extensions.submitter.properties.TreeData;

/**
 *  Description of the Class
 */
public class Stat
{
    /**
     *  Description of the Field
     */
    public Flexdbg aDbg = null;
    /**
     *  Description of the Field
     */
    public BlueJ bluej = null;

    /**
     *  Description of the Field
     */
    public GlobalProp globalProp = null;
    // Properties that are global go here
    /**
     *  Description of the Field
     */
    public TreeData treeData = null;
    // This holds the data
    /**
     *  Description of the Field
     */
    public SubmitDialog submitDialog = null;
    // This allows user interaction

            /**
     *  Description of the Field
     */
    public final static int SVC_PROP = 0x00000001;
    /**
     *  Description of the Field
     */
    public final static int SVC_BUTTON = 0x00000002;
    /**
     *  Description of the Field
     */
    public final static int SVC_PARSER = 0x00000004;
    /**
     *  Description of the Field
     */
    public final static int SVC_TREE = 0x00000008;
}
