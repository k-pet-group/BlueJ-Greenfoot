package bluej.collect;

import java.util.ArrayList;
import java.util.Map;

import org.apache.http.entity.mime.MultipartEntity;

import bluej.collect.DataSubmitter.FileKey;

/**
 * An Event with no diffs to construct.  Package-visible.
 */
class PlainEvent implements DataSubmitter.Event
{
    private MultipartEntity mpe;
    
    public PlainEvent(MultipartEntity mpe)
    {
        this.mpe = mpe;
    }

    @Override
    public MultipartEntity makeData(
            Map<FileKey, ArrayList<String>> fileVersions)
    {
        return mpe;
    }

    @Override
    public void success(Map<FileKey, ArrayList<String>> fileVersions)
    {
    }
}