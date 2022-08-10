package org.eclipse.sw360.exporter.utils;

import org.eclipse.sw360.datahandler.thrift.components.ReleaseLinkJSON;

import java.util.List;

public class ReleaseUtils {

    public static List<ReleaseLinkJSON> flattenRelease(ReleaseLinkJSON release, List<ReleaseLinkJSON> flatListReleaseLinkJSON) {
        if (release != null) {
            flatListReleaseLinkJSON.add(release);
        }

        List<ReleaseLinkJSON> children = release.getReleaseLink();
        for (ReleaseLinkJSON child : children) {
            if(child.getReleaseLink() != null) {
                flattenRelease(child, flatListReleaseLinkJSON);
            }
        }
        return flatListReleaseLinkJSON;
    }
}
