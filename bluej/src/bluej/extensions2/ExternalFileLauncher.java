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
package bluej.extensions2;

/**
 * Class holding the information between a file type and an associated launcher.
 *
 * @see OpenExternalFileHandler
 */
public final class ExternalFileLauncher
{
    /**
     * This interface allows a mechanism for a BlueJ extension to write a launcher, and for BlueJ to call this launcher.
     * When implementing the interface, BlueJ extension writers must take a particular care that their action will not
     * block the BlueJ UI thread (JavaFX thread).
     */
    public interface OpenExternalFileHandler
    {
        /**
         * The BlueJ extension implements this method as a launcher for a file, specified by its name.
         * BlueJ will call it when trying to run the launcher associated with the file extension.
         *
         * Note: particular take must be taken when implementing this method as it could cause BlueJ UI thread
         * (JavaFX thread).
         *
         * @param filePath the file path to launch
         * @throws Exception
         */
        void openFile(String filePath) throws Exception;
    }

    private final String fileExtension;
    private final OpenExternalFileHandler launcher;

    public ExternalFileLauncher(String extension, OpenExternalFileHandler launcher)
    {
        this.fileExtension = extension;
        this.launcher = launcher;
    }

    /**
     *
     * @return the file extension defined in this ExternalFileLauncher object.
     */
    public String getFileExtension()
    {
        return fileExtension;
    }

    /**
     * @see OpenExternalFileHandler
     * @return the file launcher defined in this ExternalFileLauncher object.
     */
    public OpenExternalFileHandler getLauncher()
    {
        return launcher;
    }
}
