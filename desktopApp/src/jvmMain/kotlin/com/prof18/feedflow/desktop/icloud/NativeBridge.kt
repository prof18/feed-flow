package com.prof18.feedflow.desktop.icloud

import com.sun.jna.*;
import com.sun.jna.platform.mac.CoreFoundation;

class NativeBridge {

    /**
     * Native method to retrieve the iCloud folder URL.
     * @param containerIdentifier The iCloud container identifier (e.g., "iCloud.com.prof18.feedflow").
     * @param databaseName The name of the database file to append (e.g., "database.db").
     * @return The full iCloud folder URL as a String, or null if unavailable.
     */
//    external fun getICloudFolderURL(containerIdentifier: String, databaseName: String): String?
    external fun getICloudDirectory(): String?
}


//object ICloudAccess {
//    val iCloudContainerPath: String?
//        get() {
//            // Get URL to container
//            val url: CoreFoundation = CoreFoundation.INSTANCE.CFURLCreateFromFileSystemRepresentation(
//                null,
//                "~/Library/Mobile Documents/com~apple~CloudDocs".toByteArray(),
//                -1,
//                true
//            )
//
//            return if (url != null) url.getPath() else null
//        }
//}