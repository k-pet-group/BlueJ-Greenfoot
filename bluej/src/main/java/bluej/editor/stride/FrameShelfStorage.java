/*
 This file is part of the BlueJ program.
 Copyright (C) 2016,2018  Michael Kolling and John Rosenberg

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
package bluej.editor.stride;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.IdentityHashMap;
import javafx.collections.ListChangeListener;

import bluej.stride.framedjava.frames.GreenfootFrameUtil;
import bluej.stride.generic.Frame;
import bluej.utility.Debug;
import bluej.utility.Utility;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.ParsingException;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * The central storage (one per project) for frame shelf contents.  Each GUI FXTabbedWindow
 * has its own instance of FrameShelf.  This class is the authoritative storage for the GUI
 * shelves, and copies any changes in one shelf across to the other editor windows.
 */
public class FrameShelfStorage
{
    private final IdentityHashMap<FrameShelf, ListChangeListener<Frame>> shelves = new IdentityHashMap<>();
    /**
     * A <frames>...</frames> XML element with the content of the shelf.
     */
    private Element contentXML;
    private final File shelfFilename;
    private boolean updatingShelves = false;

    @OnThread(Tag.Any)
    public FrameShelfStorage(File projectDir)
    {
        shelfFilename = new File(projectDir, "shelf.xml");
        try
        {
            Document xml = new Builder().build(shelfFilename);
            contentXML = xml.getRootElement();
            if (!contentXML.getLocalName().equals("frames"))
            {
                throw new IOException("XML top-level element not \"frames\" as expected");
            }
        }
        catch (ParsingException | IOException e)
        {
            //Debug.reportError(e);
            // If there was a problem, make a frames element with no children:
            contentXML = new Element("frames");
        }
    }

    public void registerShelf(FrameShelf shelfInterface)
    {
        ListChangeListener<Frame> listener = c -> pullFrom(shelfInterface);
        shelves.put(shelfInterface, listener);
        shelfInterface.setContent(contentXML);
        shelfInterface.getContent().addListener(listener);
    }

    public void deregisterShelf(FrameShelf shelfInterface)
    {
        if (shelves.get(shelfInterface) != null)
        {
            shelfInterface.getContent().removeListener(shelves.remove(shelfInterface));
        }
    }

    private void pullFrom(FrameShelf changed)
    {
        if (updatingShelves)
        {
            // We are seeing an update because we are setting content, further down
            // this method.  Ignore
            return;
        }

        // Update contentXML:
        contentXML = GreenfootFrameUtil.getXmlElementForMultipleFrames(changed.getContent());
        updatingShelves = true;
        // Then push to all other shelves:
        shelves.forEach((shelf, listener) -> {
            if (shelf != changed)
                shelf.setContent(contentXML);
        });
        save();
        updatingShelves = false;
    }

    private void save()
    {
        try (FileOutputStream os = new FileOutputStream(shelfFilename)) {
            Utility.serialiseCodeTo(contentXML, os);
        }
        catch (IOException e)
        {
            Debug.reportError("Cannot save shelf contents", e);
        }
    }
}
