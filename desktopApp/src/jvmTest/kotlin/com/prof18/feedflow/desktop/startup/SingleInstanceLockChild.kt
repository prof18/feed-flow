package com.prof18.feedflow.desktop.startup

import java.io.File

/**
 * Entry point of the child JVM in [SingleInstanceLockTest]: holds the lock and listens for activation
 * requests until stdin closes.
 */
fun main(args: Array<String>) {
    val directory = File(args[0])
    val result = SingleInstanceLock().acquire(directory)
    val listening = InstanceActivation().listen(directory) {
        println("ACTIVATED")
        System.out.flush()
    }
    println(if (result == SingleInstanceLock.Result.Acquired && listening) "LOCKED" else "NOT_LOCKED $result")
    System.out.flush()
    System.`in`.read()
}
