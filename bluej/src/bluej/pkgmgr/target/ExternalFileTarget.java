/*
 This file is part of the BlueJ program.
 Copyright (C) 2021  Michael Kolling and John Rosenberg

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
package bluej.pkgmgr.target;

import bluej.Config;
import bluej.editor.Editor;
import bluej.extensions2.ExternalFileLauncher;
import bluej.extensions2.PackageNotFoundException;
import bluej.extensions2.ProjectNotOpenException;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.utility.Debug;
import bluej.utility.DialogManager;
import bluej.utility.javafx.AbstractOperation;
import bluej.utility.javafx.JavaFXUtil;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Window;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Target class for external files not natively supported by BlueJ
 * Only extensions can be used in BlueJ to handle those external files.
 * If no extension can handle a file, the target isn't shown at all in BlueJ.
 */
public class ExternalFileTarget extends NonCodeEditableTarget
{
    // The external file
    private final File extFile;

    // Menu entries' label
    private static final String openStr = Config.getString("pkgmgr.extfilemenu.open");

    public ExternalFileTarget(Package aPackage, File extFile){
        super(aPackage, extFile.getName(), "");
        this.extFile = extFile;

        JavaFXUtil.addStyleClass(pane, "externalfile-target");
        JavaFXUtil.addStyleClass(pane, "externalfile-target-id-" + extFile.getName().replace(".", "-"));
        Label name = new Label(extFile.getName());
        BorderPane.setAlignment(name, Pos.CENTER);
        pane.setTop(name);
    }

    private static class OpenExternalFileAction extends AbstractOperation<Target>
    {
        public OpenExternalFileAction()
        {
            super("openExtFile", Combine.ALL, null);
        }

        @Override
        public void activate(List<Target> targets)
        {
            for (Target target : targets)
            {
                if (target instanceof ExternalFileTarget)
                {
                    String fileName = ((ExternalFileTarget) target).getSourceFile().getName();
                    String fileExtension = fileName.substring(fileName.lastIndexOf(".")); // safe to do, we already know the extension file is correct
                    try
                    {
                        // We retrieve the launchers from the mapping saved for that project. Note that at this stage,
                        // the CompletableFuture object should be ready since we show the targets in the UI once retrieving
                        // the BlueJ extensions' launchers.
                        ExternalFileLauncher.OpenExternalFileHandler launcher = target.getPackage().getProject().getProjectExternalFileOpenMap().get(fileExtension);
                        // If for some strange reason the launcher isn't picked (is null), the exception will be caught in the try/catch
                        launcher.openFile(((ExternalFileTarget) target).getSourceFile().getAbsolutePath());
                    }
                    catch (Exception e)
                    {
                        Debug.message("A problem occurred while trying to open the external target "+fileName+": " + e.getMessage());
                        // Alert the user something went wrong (the parent isn't necessary but it provides a nicer UI...)
                        Window parent = null;
                        try
                        {
                            parent = target.getPackage().getBPackage().getWindow();
                        }
                        catch(PackageNotFoundException | ProjectNotOpenException pe)
                        {
                            Debug.message("A problem occurred while getting the current external target's related package window: " + pe.getMessage());
                        }

                        DialogManager.showErrorFX(parent, "external-file-open-issue");
                    }
                }
            }
        }

        @Override
        public List<ItemLabel> getLabels()
        {
            return List.of(new ItemLabel(new ReadOnlyStringWrapper(openStr), MenuItemOrder.EDIT));
        }
    }

    // The deletion has nothing special for external files, but we want to show a consistent UI in the menu
    private static class RemoveExternalFileAction extends AbstractOperation<Target>
    {
        public RemoveExternalFileAction()
        {
            super("removeExtFile", Combine.ALL, null);
        }

        @Override
        public void activate(List<Target> targets)
        {
            List<ExternalFileTarget> extTargets = targets.stream().flatMap(t -> t instanceof ExternalFileTarget ? Stream.of((ExternalFileTarget)t) : Stream.empty()).collect(Collectors.toList());
            if (!extTargets.isEmpty())
            {
                PkgMgrFrame pmf = PkgMgrFrame.findFrame(extTargets.get(0).getPackage());
                if (pmf != null && pmf.askRemoveFiles())
                {
                    for (ExternalFileTarget extTarget : extTargets)
                    {
                        extTarget.remove();
                    }
                }
            }
        }

        @Override
        public List<ItemLabel> getLabels()
        {
            return List.of(new ItemLabel(new ReadOnlyStringWrapper(removeStr), MenuItemOrder.REMOVE));
        }
    }


    @Override
    // Double click will activate the launcher retrieved by BlueJ in available extensions.
    public @OnThread(Tag.FXPlatform) void doubleClick(boolean openInNewWindow)
    {
        OpenExternalFileAction openAction = new OpenExternalFileAction();
        openAction.activate(Arrays.asList(this));
    }

    @Override
    public List<? extends AbstractOperation<Target>> getContextOperations()
    {
        return List.of(new OpenExternalFileAction(), new RemoveExternalFileAction());
    }

    @Override
    public void remove()
    {
        getPackage().removeTarget(this);
        extFile.delete();
    }

    @Override
    protected File getSourceFile()
    {
        return extFile;
    }

    @Override
    public Editor getEditor()
    {
        // Useless for external file, as they are not handled by BlueJ
        return editor;
    }

    @Override
    public @OnThread(Tag.FX) boolean isResizable()
    {
        return true;
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public void save(Properties props, String prefix)
    {
        super.save(props, prefix);
        props.put(prefix + ".type", "ExternalFileTarget");
    }
}
