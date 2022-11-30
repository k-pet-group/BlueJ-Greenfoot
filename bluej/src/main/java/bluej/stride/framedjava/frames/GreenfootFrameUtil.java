/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016 Michael Kölling and John Rosenberg 
 
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
import java.util.List;
import java.util.stream.Collectors;

import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import bluej.parser.ParseFailure;
import bluej.stride.framedjava.ast.Parser;
import bluej.stride.framedjava.convert.ConversionWarning;
import bluej.stride.framedjava.convert.ConvertResultDialog;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.ParsingException;
import bluej.stride.framedjava.ast.Loader;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.generic.Frame;
import bluej.utility.Debug;
import threadchecker.OnThread;
import threadchecker.Tag;

public class GreenfootFrameUtil
{
    private static class XMLParseResult
    {
        public final List<CodeElement> elements; // null if error
        public final String error; // null if success

        public XMLParseResult(List<CodeElement> elements)
        {
            this.elements = elements;
            this.error = null;
        }

        public XMLParseResult(String error)
        {
            this.elements = null;
            this.error = error;
        }
    }

    // On FXPlatform thread because it may show dialog:
    @OnThread(Tag.FXPlatform)
    public static List<CodeElement> getClipboardElements(Parser.JavaContext context)
    {
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        if (clipboard.hasString()) {
            XMLParseResult strideParseResult = getElements(clipboard.getString());
            if (strideParseResult.elements != null)
                return strideParseResult.elements;
            
            try
            {
                Parser.ConversionResult javaConvertResult = Parser.javaToStride(clipboard.getString(), context, false);
                if (!javaConvertResult.getWarnings().isEmpty())
                {
                    new ConvertResultDialog(javaConvertResult.getWarnings().stream().map(ConversionWarning::getMessage).collect(Collectors.toList())).showAndWait();
                }
                return javaConvertResult.getElements();
            }
            catch (ParseFailure pf)
            {
                new ConvertResultDialog(strideParseResult.error, pf.getMessage()).showAndWait();
                return null;
            }
        }

        return null;
    }

    private static XMLParseResult getElements(String xmlString)
    {
        Builder parser = new Builder();
        Document doc = null;
        try {
            doc = parser.build(xmlString, null);
        }
        catch (ParsingException | IOException e) {
            Debug.reportError(e);
            return new XMLParseResult(e.getMessage());
        }
        
        if (doc == null) {
            return new XMLParseResult("Unknown error");
        }

        Element root = doc.getRootElement();
        if (!root.getLocalName().equals("frames")) {
            return new XMLParseResult("Outer element was not frames");
        }
        
        List<CodeElement> elements = new ArrayList<CodeElement>();
        for (int i = 0; i < root.getChildElements().size(); i++) {
            elements.add(Loader.loadElement(root.getChildElements().get(i)));
        }
        return new XMLParseResult(elements);
    }
    
    public static String getXmlForMultipleFrames(List<Frame> frames)
    {
        Element framesEl = getXmlElementForMultipleFrames(frames);
        return framesEl.toXML();
    }

    public static Element getXmlElementForMultipleFrames(List<Frame> frames)
    {
        Element framesEl = new Element("frames");
        for (Frame f : frames) {
            if (f instanceof CodeFrame) {
                ((CodeFrame<?>) f).regenerateCode();
                CodeElement c = ((CodeFrame<?>) f).getCode();
                framesEl.appendChild(c.toXML());
            }
        }
        return framesEl;
    }

    @OnThread(Tag.FXPlatform)
    private static String getJavaForMultipleFrames(List<Frame> frames)
    {
        StringBuilder java = new StringBuilder();
        for (Frame f : frames) {
            if (f instanceof CodeFrame) {
                CodeElement c = ((CodeFrame<?>) f).getCode();
                java.append(c.toJavaSource().toTemporaryJavaCodeString());
            }
        }
        return java.toString();
    }

    public static List<CodeElement> getElementsForMultipleFrames(List<Frame> frames)
    {
        return getElements(getXmlForMultipleFrames(frames)).elements;
    }

    public static void doCopyAsStride(List<Frame> frames)
    {
        // Nothing to copy if nothing selected:
        if (frames.size() == 0)
            return;

        final ClipboardContent content = new ClipboardContent();
        content.putString(getXmlForMultipleFrames(frames));
        Clipboard.getSystemClipboard().setContent(content);
    }

    public static void doCopyAsImage(List<Frame> frames)
    {
        // Nothing to copy if nothing selected:
        if (frames.size() == 0)
            return;

        final ClipboardContent content = new ClipboardContent();
        content.putImage(Frame.takeShot(frames, null));
        Clipboard.getSystemClipboard().setContent(content);
    }

    @OnThread(Tag.FXPlatform)
    public static void doCopyAsJava(List<Frame> frames)
    {
        // Nothing to copy if nothing selected:
        if (frames.size() == 0)
            return;

        final ClipboardContent content = new ClipboardContent();
        content.putString(getJavaForMultipleFrames(frames));
        Clipboard.getSystemClipboard().setContent(content);
    }
}
