// #import <Foundation/Foundation.h>
// #import <jni.h>
//
#import <Foundation/Foundation.h>
#import <jni.h>

// Function to get iCloud documents directory path
JNIEXPORT jstring JNICALL Java_com_prof18_feedflow_desktop_icloud_NativeBridge_getICloudDirectory(JNIEnv *env, jobject obj) {
    NSURL *icloudURL = [[NSFileManager defaultManager] URLForUbiquityContainerIdentifier:nil];
    if (icloudURL) {
        NSString *icloudPath = [[icloudURL path] stringByAppendingPathComponent:@"Documents"];
        return (*env)->NewStringUTF(env, [icloudPath UTF8String]);
    } else {
        return NULL; // iCloud is not available
    }
}


// Function to get iCloud folder URL for a specific container and database
// language=obj-c
// JNIEXPORT jstring JNICALL Java_com_prof18_feedflow_desktop_icloud_NativeBridge_getICloudFolderURL(JNIEnv *env, jobject obj, jstring containerIdentifier, jstring databaseName) {
//     // Convert Java strings to Objective-C strings
//     const char *containerIdentifierCString = (*env)->GetStringUTFChars(env, containerIdentifier, 0);
//     const char *databaseNameCString = (*env)->GetStringUTFChars(env, databaseName, 0);
//
//     NSString *containerIdentifierObjC = [NSString stringWithUTF8String:containerIdentifierCString];
//     NSString *databaseNameObjC = [NSString stringWithUTF8String:databaseNameCString];
//
//     printf("containerIdentifierObjC: %s\n", [containerIdentifierObjC UTF8String]);
//     printf("databaseNameObjC: %s\n", [databaseNameObjC UTF8String]);
//
//     // Get the URL for the iCloud container
//     NSURL *icloudURL = [[NSFileManager defaultManager] URLForUbiquityContainerIdentifier:containerIdentifierObjC];
//
//     // If iCloud is available and the container URL is retrieved, append the "Documents" folder and the database name
//     if (icloudURL) {
//         NSURL *documentsURL = [icloudURL URLByAppendingPathComponent:@"Documents"];
//         NSURL *databaseURL = [documentsURL URLByAppendingPathComponent:databaseNameObjC];
//
//         // Convert NSURL to string and return it
//         NSString *databaseURLString = [databaseURL path];
//         (*env)->ReleaseStringUTFChars(env, containerIdentifier, containerIdentifierCString);
//         (*env)->ReleaseStringUTFChars(env, databaseName, databaseNameCString);
//
//         return (*env)->NewStringUTF(env, [databaseURLString UTF8String]);
//     } else {
//         // If iCloud is not available, return NULL
//         (*env)->ReleaseStringUTFChars(env, containerIdentifier, containerIdentifierCString);
//         (*env)->ReleaseStringUTFChars(env, databaseName, databaseNameCString);
//         return NULL;
//     }
// }
