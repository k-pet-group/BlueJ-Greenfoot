/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015 Michael Kölling and John Rosenberg 
 
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
package bluej.stride.framedjava.frames;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.ParsingException;
import bluej.stride.framedjava.ast.Loader;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.generic.Frame;
import bluej.stride.generic.InteractionManager;
import bluej.stride.operations.CopyFrameOperation;
import bluej.stride.operations.CutFrameOperation;
import bluej.stride.operations.FrameOperation;
import bluej.utility.Debug;

public class GreenfootFrameUtil
{
    public static List<FrameOperation> cutCopyPasteOperations(InteractionManager editor)
    {
        return Arrays.asList(new CutFrameOperation(editor), new CopyFrameOperation(editor));
    }

    public static List<CodeElement> getClipboardElements()
    {
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        // TODO: use mime type
        if (clipboard.hasString()) {
            return getElements(clipboard.getString());
        }

        return null;
    }

    public static List<CodeElement> getElements(String xmlString)
    {
        Builder parser = new Builder();
        Document doc = null;
        try {
            doc = parser.build(xmlString, null);
        }
        catch (ParsingException | IOException e) {
            Debug.reportError(e);
        }
        
        if (doc == null) {
            return null;
        }

        Element root = doc.getRootElement();
        if (!root.getLocalName().equals("frames")) {
            return null;
        }
        
        List<CodeElement> elements = new ArrayList<CodeElement>();
        for (int i = 0; i < root.getChildElements().size(); i++) {
            elements.add(Loader.loadElement(root.getChildElements().get(i)));
        }
        return elements;
    }
    
    public static String getXmlForMultipleFrames(List<Frame> frames)
    {
        Element framesEl = new Element("frames");
        for (Frame f : frames) {
            if (f instanceof CodeFrame) {
                CodeElement c = ((CodeFrame<?>) f).getCode();
                framesEl.appendChild(c.toXML());
            }
        }
        return framesEl.toXML();
    }

    public static List<CodeElement> getElementsForMultipleFrames(List<Frame> frames)
    {
        return getElements(getXmlForMultipleFrames(frames));
    }

    public static void doCopy(List<Frame> frames)
    {
        // Nothing to copy if nothing selected:
        if (frames.size() == 0)
            return;

        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        // TODO copy code as stored XML with own mime type
        content.putString(getXmlForMultipleFrames(frames));
        content.putImage(Frame.takeShot(frames, null));
        clipboard.setContent(content);
    }
}
