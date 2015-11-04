/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2012,2013,2014  Michael Kolling and John Rosenberg 
 
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
package greenfoot.blocks;

//import greenfoot.blocks.ast.ClassElement;
//import greenfoot.blocks.ast.CodeElement;
//import greenfoot.blocks.ast.JavaSource;
import greenfoot.core.GClass;
import greenfoot.core.GPackage;
import greenfoot.event.CompileListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.rmi.RemoteException;
import java.util.ArrayList;

import javax.swing.SwingUtilities;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.ParsingException;
import nu.xom.Serializer;
import rmiextension.wrappers.RClass;
import rmiextension.wrappers.event.RCompileEvent;
import bluej.extensions.CompilationNotStartedException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;
import bluej.utility.Debug;

/**
 * 
 * @author Fraser McKay
 */
public class BlockGClass {} /* extends GClass implements CompileListener
{ 
    private ClassElement source;
    private EditorFrame editor;
    private JavaSource javaSource;
    
    public BlockGClass(RClass cls, GPackage pkg, boolean inRemoteCallback)
    {
        super(cls, pkg, inRemoteCallback);
        pkg.getProject().addCompileListener(this);
    }
    
    public void edit()
    {
        try {
            if (SwingUtilities.isEventDispatchThread()) {
                getEditor().showInWindow("class " + getName(), null, null);
            }
            else {
            SwingUtilities.invokeAndWait(new Runnable() { public void run() {
                getEditor().showInWindow("class " + getName(), null, null);
            }});
            }
        } catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        //}
    }
    
    public void closeEditor()
    {
        
    }
    
    public void showMessage(String message)
    {
        
    }

    @Override
    public void compile(boolean waitCompileEnd, boolean forceQuiet)
            throws ProjectNotOpenException, PackageNotFoundException,
            RemoteException, CompilationNotStartedException
    {
        if (editor != null)
        {
            // If they've opened an editor, use that source:
            source = editor.getSource();
            saveToFile();
        }
        else if (source == null)
        {
            loadFromFile();
        }
        
        Debug.message("Compiling from new editor! :D");
        
        try
        {
            FileWriter w = new FileWriter(getFile("java"));
            javaSource = source.toJavaCode(null);
            javaSource.prependString("import greenfoot.*;", null); // TODO set handler
            w.write(javaSource.toJavaCode());
            Debug.message("Writing code:");
            Debug.message(javaSource.toJavaCode());
            w.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        
        super.compile(waitCompileEnd, forceQuiet);
    }

    private File getFile(String extension)
    {
        return new File(getName() + "." + extension);
    }

    @Override
    public boolean isCompiled() {
        return super.isCompiled();
    }

    private EditorFrame getEditor() {
        if (editor == null)
        {
            if (source == null)
            {
                loadFromFile();
            }
            editor = new EditorFrame(source);
        }
        return editor;
    }

    private void loadFromFile()
    {
        try
        {
            Document xml = new Builder().build(getFile("frame"));
            
            source = new ClassElement(xml.getRootElement());
        }
        catch (ParsingException | IOException e)
        {
            Debug.reportError(e);
            // Use blank class:
            source = new ClassElement(null, getName(), new ArrayList<CodeElement>());
        }
    }
    
    private void saveToFile()
    {
        try
        {
            Serializer s = new Serializer(new FileOutputStream(getFile("frame")));
            s.write(new Document(source.toXML()));
            s.flush();
        }
        catch (IOException e)
        {
            Debug.reportError(e);
        }
    }

    @Override public void compileStarted(RCompileEvent event) { }

    @Override
    public void compileError(final RCompileEvent event)
    {
        try
        {
            if (!event.getFiles()[0].getCanonicalFile().equals(getFile("java").getCanonicalFile()))
                return;
        }
        catch (IOException e)
        {
            Debug.reportError(e);
        } // Not our file
        
        SwingUtilities.invokeLater(new Runnable() { public void run() {
            // Make sure editor exists first:
            getEditor().invokeWhenLoaded(new Runnable() { public void run() {
                try
                {
                    Debug.message("Compiler error: " + event.getErrorMessage());
                    javaSource.handleError(event.getErrorStartLine(), event.getErrorStartColumn(),
                        event.getErrorEndLine(), event.getErrorEndColumn(), event.getErrorMessage());
                }
                catch (RemoteException e)
                {
                    Debug.reportError(e);
                }        
            }});
            //Now show editor:
            edit();            
        }});
    }

    @Override public void compileWarning(RCompileEvent event) { }

    @Override public void compileSucceeded(RCompileEvent event) { }
    
    @Override public void compileFailed(RCompileEvent event) { }
    
}
*/