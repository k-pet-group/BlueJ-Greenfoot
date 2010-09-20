/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010  Poul Henriksen and Michael Kolling 
 
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
package rmiextension.wrappers;

import java.awt.EventQueue;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.rmi.RemoteException;
import java.util.Iterator;

import javax.swing.text.BadLocationException;

import bluej.editor.moe.MoeEditor;
import bluej.editor.moe.MoeIndent;
import bluej.editor.moe.MoeSyntaxDocument;
import bluej.extensions.BClass;
import bluej.extensions.BConstructor;
import bluej.extensions.BField;
import bluej.extensions.BMethod;
import bluej.extensions.BPackage;
import bluej.extensions.ClassNotFoundException;
import bluej.extensions.CompilationNotStartedException;
import bluej.extensions.ExtensionBridge;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;
import bluej.extensions.editor.Editor;
import bluej.parser.nodes.ParsedNode;
import bluej.parser.nodes.NodeTree.NodeAndPosition;
import bluej.utility.Debug;

/**
 * Implementation of the remote class interface.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 */
public class RClassImpl extends java.rmi.server.UnicastRemoteObject
    implements RClass
{
    BClass bClass;
    
    private static ProjectNotOpenException pnoe;
    private static PackageNotFoundException pnfe;
    private boolean booleanResult;
    
    /**
     * Package-private constructor. Use WrapperPool to instantiate.
     */
    RClassImpl(BClass bClass)
        throws RemoteException
    {
        this.bClass = bClass;
        if (bClass == null) {
            throw new NullPointerException("Argument can't be null");
        }
    }

    public RClassImpl()
        throws RemoteException
    {

    }

    /**
     * @return
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     */
    public void compile(boolean waitCompileEnd, boolean forceQuiet)
        throws ProjectNotOpenException, PackageNotFoundException, CompilationNotStartedException
    {
        bClass.compile(waitCompileEnd, forceQuiet);
    }
    
    public boolean hasSourceCode() throws ProjectNotOpenException, PackageNotFoundException
    {
        return ExtensionBridge.hasSourceCode(bClass);
    }

    public void edit()
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException
    {
        synchronized (RClassImpl.class) {
            pnoe = null;
            pnfe = null;
            
            EventQueue.invokeLater(new Runnable() {
                public void run()
                {
                    try {
                        Editor editor = bClass.getEditor();
                        if (editor != null) {
                            editor.setVisible(true);
                        }
                    }
                    catch (ProjectNotOpenException e) {
                        pnoe = e;
                    }
                    catch (PackageNotFoundException e) {
                        pnfe = e;
                    }
                }
            });
            
            if (pnoe != null) throw pnoe;
            if (pnfe != null) throw pnfe;
        }
    }
    
    /**
     * Closes the editor (sets the editor to not visible)
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     * @throws RemoteException
     */
    public void closeEditor() throws ProjectNotOpenException, PackageNotFoundException, RemoteException
    {
        synchronized (RClassImpl.class) {
            pnoe = null;
            pnfe = null;

            EventQueue.invokeLater(new Runnable() {
                public void run()
                {
                    try {
                        Editor editor = bClass.getEditor();
                        if (editor != null) {
                            editor.setVisible(false);
                        }
                    }
                    catch (ProjectNotOpenException e) {
                        pnoe = e;
                    }
                    catch (PackageNotFoundException e) {
                        pnfe = e;
                    }
                }
            });

            if (pnoe != null) throw pnoe;
            if (pnfe != null) throw pnfe;
        }
    }
    
    public void showMessage(final String message) throws RemoteException,
            ProjectNotOpenException, PackageNotFoundException
    {
        final Editor e = bClass.getEditor();
        EventQueue.invokeLater(new Runnable() {
            public void run()
            {
                e.showMessage(message);
            }
        });
    }

    public void insertAppendMethod(final String comment, final String access, final String methodName, final String methodBody, final boolean showEditorOnCreate, final boolean showEditorOnAppend) throws ProjectNotOpenException, PackageNotFoundException, RemoteException
    {
        final Editor e = bClass.getEditor();
        EventQueue.invokeLater(new Runnable() {
            public void run()
            {
                MoeEditor bje = (MoeEditor)bluej.extensions.editor.EditorBridge.getEditor(e);
                MoeSyntaxDocument doc = bje.getSourceDocument();
                
                
                NodeAndPosition<ParsedNode> classNode = findClassNode(doc);
                if (classNode == null)
                    return;
                NodeAndPosition<ParsedNode> existingMethodNode = findMethodNode(methodName, classNode);
        
                if (existingMethodNode != null) {
                    //Append to existing method:
                    appendTextToNode(e, bje, existingMethodNode, methodBody);
                    if (showEditorOnAppend)
                        e.setVisible(true);
                } else {
                    //Make a new method:
                    String fullMethod = comment + "    " + access + " void " + methodName + "()\n    {\n" + methodBody + "    }\n";
                    appendTextToNode(e, bje, classNode, fullMethod);
                    if (showEditorOnCreate)
                        e.setVisible(true);
                }
            }
        });
    }
    
    private NodeAndPosition<ParsedNode> findClassNode(MoeSyntaxDocument doc)
    {
        NodeAndPosition<ParsedNode> root = new NodeAndPosition<ParsedNode>(doc.getParser(), 0, doc.getParser().getSize());
        for (NodeAndPosition<ParsedNode> nap : iterable(root)) {
            if (nap.getNode().getNodeType() == ParsedNode.NODETYPE_TYPEDEF)
                return nap;
        }
        return null;
    }

    public void insertMethodCallInConstructor(final String methodName, final boolean showEditor)
            throws ProjectNotOpenException, PackageNotFoundException,
            RemoteException
    {
        final Editor e = bClass.getEditor();
        EventQueue.invokeLater(new Runnable() {
            public void run()
            {
                MoeEditor bje = (MoeEditor)bluej.extensions.editor.EditorBridge.getEditor(e);
                MoeSyntaxDocument doc = bje.getSourceDocument();
                
                NodeAndPosition<ParsedNode> classNode = findClassNode(doc);
                if (classNode == null)
                    return;
                NodeAndPosition<ParsedNode> constructor = findMethodNode(bClass.getName(), classNode);
                if (constructor != null && false == hasMethodCall(doc, methodName, constructor, true)) {
                    //Add at the end of the constructor:
                    appendTextToNode(e, bje, constructor, "\n        " + methodName + "();\n    ");
                }
                
                if (showEditor)
                    e.setVisible(true);
            }
        });
    }
    
    // Appends text to a node that ends in a curly bracket:
    private void appendTextToNode(Editor e, MoeEditor bjEditor, NodeAndPosition<ParsedNode> node, String text)
    {
        //The node may have whitespace at the end, so we look for the last closing brace and
        //insert before that:
        for (int pos = node.getEnd() - 1; pos >= 0; pos--) {
            if ("}".equals(e.getText(e.getTextLocationFromOffset(pos), e.getTextLocationFromOffset(pos+1)))) {
                bjEditor.undoManager.beginCompoundEdit();
                int originalLength = node.getSize();
                // First insert the text:
                e.setText(e.getTextLocationFromOffset(pos), e.getTextLocationFromOffset(pos), text);
                // Then auto-indent the method to make sure our indents were correct:
                int oldPos = bjEditor.getSourcePane().getCaretPosition();
                MoeIndent.calculateIndentsAndApply(bjEditor.getSourceDocument(), node.getPosition(), node.getPosition() + originalLength + text.length(), oldPos);
                bjEditor.undoManager.endCompoundEdit();
                e.setCaretLocation(e.getTextLocationFromOffset(pos));
                return;
            }
        }
        Debug.message("Could not find end of node to append to: \"" + e.getText(e.getTextLocationFromOffset(node.getPosition()), e.getTextLocationFromOffset(node.getEnd())) + "\"");
    }
    
    // This really returns an iterator, but wrapping it into an iterable means that
    // we can use Java's nice for-each loops:
    private Iterable<NodeAndPosition<ParsedNode>> iterable(final NodeAndPosition<ParsedNode> parent)
    {
        return new Iterable<NodeAndPosition<ParsedNode>>()
        {
            public Iterator<NodeAndPosition<ParsedNode>> iterator()
            {
                return parent.getNode().getChildren(parent.getPosition());
            };
        };
    }
    
    private boolean hasMethodCall(MoeSyntaxDocument doc, String methodName, NodeAndPosition<ParsedNode> methodNode, boolean root)
    {
        for (NodeAndPosition<ParsedNode> nap : iterable(methodNode)) {
            // Method nodes have comments as children, and the body:
            if (nap.getNode().getNodeType() == ParsedNode.NODETYPE_NONE && root) {
                return hasMethodCall(doc, methodName, nap, false);
            }
            
            try {
                if (nap.getNode().getNodeType() == ParsedNode.NODETYPE_EXPRESSION && doc.getText(nap.getPosition(), nap.getSize()).startsWith(methodName)) {
                    return true;
                }
            }
            catch (BadLocationException e) {
            }            
        }
        
        return false;
    }
    
    private NodeAndPosition<ParsedNode> findMethodNode(String methodName, NodeAndPosition<ParsedNode> start)
    {
        for (NodeAndPosition<ParsedNode> nap : iterable(start)) {
            if (nap.getNode().getNodeType() == ParsedNode.NODETYPE_NONE) {
                NodeAndPosition<ParsedNode> r = findMethodNode(methodName, nap);
                if (r != null)
                    return r;
            }
            if (nap.getNode().getNodeType() == ParsedNode.NODETYPE_METHODDEF && nap.getNode().getName().equals(methodName)) {
                return nap;
            }
        }
        
        return null;
    }

    /**
     * @param signature
     * @return
     * @throws ProjectNotOpenException
     * @throws ClassNotFoundException
     */
    public RConstructor getConstructor(Class<?>[] signature)
        throws ProjectNotOpenException, ClassNotFoundException, RemoteException
    {

        BConstructor bConstructor = bClass.getConstructor(signature);

        RConstructor rConstructor = WrapperPool.instance().getWrapper(bConstructor);
        return rConstructor;
    }

    /**
     * @return
     * @throws ProjectNotOpenException
     * @throws ClassNotFoundException
     */
    public RConstructor[] getConstructors()
        throws ProjectNotOpenException, ClassNotFoundException, RemoteException
    {

        BConstructor[] bConstructors = bClass.getConstructors();
        int length = bConstructors.length;
        RConstructor[] rConstructors = new RConstructor[length];
        for (int i = 0; i < length; i++) {
            rConstructors[i] = WrapperPool.instance().getWrapper(bConstructors[i]);
        }

        return rConstructors;
    }

    /**
     * @param methodName
     * @param params
     * @return
     * @throws ProjectNotOpenException
     * @throws ClassNotFoundException
     */
    public BMethod getDeclaredMethod(String methodName, Class<?>[] params)
        throws ProjectNotOpenException, ClassNotFoundException
    {
        return null;
    }

    /**
     * @return
     * @throws ProjectNotOpenException
     * @throws ClassNotFoundException
     */
    public BMethod[] getDeclaredMethods()
        throws ProjectNotOpenException, ClassNotFoundException
    {
        return bClass.getDeclaredMethods();

    }

    /**
     * @param fieldName
     * @return
     * @throws ProjectNotOpenException
     * @throws ClassNotFoundException
     */
    public RField getField(String fieldName)
        throws ProjectNotOpenException, ClassNotFoundException, RemoteException
    {

        BField wrapped = bClass.getField(fieldName);
        RField wrapper = WrapperPool.instance().getWrapper(wrapped);
        return wrapper;

    }

    /**
     * @return
     * @throws ProjectNotOpenException
     * @throws ClassNotFoundException
     */
    public BField[] getFields()
        throws ProjectNotOpenException, ClassNotFoundException
    {
        return bClass.getFields();
    }

    /**
     * @return
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     */
    public RPackage getPackage()
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException
    {

        BPackage wrapped = bClass.getPackage();
        RPackage wrapper = WrapperPool.instance().getWrapper(wrapped);
        return wrapper;
    }

    /**
     * Gets the superclass of this class if it is a part of the project.
     * 
     * @see #getSuperclassName()
     * @return
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     * @throws ClassNotFoundException
     */
    public RClass getSuperclass()
        throws ProjectNotOpenException, PackageNotFoundException, ClassNotFoundException, RemoteException
    {
        BClass wrapped = bClass.getSuperclass();
        RClass wrapper = WrapperPool.instance().getWrapper(wrapped);
        return wrapper;
    }

    /**
     * @return
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     */
    public boolean isCompiled()
        throws ProjectNotOpenException, PackageNotFoundException
    {
        synchronized (RClassImpl.class) {
            pnoe = null;
            pnfe = null;
            try {
                EventQueue.invokeAndWait(new Runnable() {
                    public void run()
                    {
                        try {
                            booleanResult = bClass.isCompiled();
                        } catch (ProjectNotOpenException e) {
                            pnoe = e;
                        } catch (PackageNotFoundException e) {
                            pnfe = e;
                        }
                    }                
                });
            }
            catch (InterruptedException ie) {
                ie.printStackTrace();
            }
            catch (InvocationTargetException ite) {
                ite.printStackTrace();
            }
            
            if (pnoe != null) throw pnoe;
            if (pnfe != null) throw pnfe;
            
            return booleanResult;
        }
    }

    public String getToString()
    {
        return bClass.getName();
    }

    public String getQualifiedName()
        throws RemoteException
    {
        return bClass.getName();
    }

    public File getJavaFile()
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException
    {
        return bClass.getJavaFile();
    }

    public void remove() throws ProjectNotOpenException, PackageNotFoundException, ClassNotFoundException, RemoteException
    {
        bClass.remove();
    }


    public void setReadOnly(boolean b) throws RemoteException, ProjectNotOpenException, PackageNotFoundException 
    {
        if(bClass != null && bClass.getEditor() != null) {
            bClass.getEditor().setReadOnly(b);
        }
    }

}
