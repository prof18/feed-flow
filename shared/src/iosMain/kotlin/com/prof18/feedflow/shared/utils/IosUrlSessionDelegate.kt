package com.prof18.feedflow.shared.utils

import kotlinx.cinterop.*
import platform.Foundation.*
import platform.darwin.NSObject

/**
 * Custom NSURLSessionDelegate for iOS that intercepts HTTP responses
 * to extract cache control information
 */
class IosUrlSessionDelegate(
    private val cacheControlManager: IosCacheControlManager
) : NSObject(), NSURLSessionDataDelegateProtocol {
    
    /**
     * Called when the session receives a response from the server
     * This is where we extract cache control headers
     */
    override fun URLSession(
        session: NSURLSession,
        dataTask: NSURLSessionDataTask,
        didReceiveResponse: NSURLResponse,
        completionHandler: (NSURLSessionResponseDisposition) -> Unit
    ) {
        val httpResponse = didReceiveResponse as? NSHTTPURLResponse
        val url = didReceiveResponse.URL?.absoluteString
        
        if (httpResponse != null && url != null) {
            // Extract and store cache control information
            cacheControlManager.extractAndStoreCacheInfo(url, httpResponse)
        }
        
        // Allow the request to continue
        completionHandler(NSURLSessionResponseDisposition.NSURLSessionResponseAllow)
    }
    
    /**
     * Called when the session completes with success or failure
     */
    override fun URLSession(
        session: NSURLSession,
        task: NSURLSessionTask,
        didCompleteWithError: NSError?
    ) {
        // Handle completion if needed (logging, cleanup, etc.)
        if (didCompleteWithError != null) {
            // Could log network errors here if desired
        }
    }
}

/**
 * Factory to create NSURLSession configured with cache control interception
 */
object IosUrlSessionFactory {
    
    fun createSessionWithCacheControl(
        cacheControlManager: IosCacheControlManager
    ): NSURLSession {
        val delegate = IosUrlSessionDelegate(cacheControlManager)
        
        val configuration = NSURLSessionConfiguration.defaultSessionConfiguration().apply {
            HTTPAdditionalHeaders = mapOf(
                "User-Agent" to "FeedFlow (RSS Reader; +https://feedflow.dev)",
            )
        }
        
        return NSURLSession.sessionWithConfiguration(
            configuration = configuration,
            delegate = delegate,
            delegateQueue = NSOperationQueue.mainQueue
        )
    }
}