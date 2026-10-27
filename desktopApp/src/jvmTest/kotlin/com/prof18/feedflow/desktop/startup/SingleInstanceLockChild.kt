package com.prof18.feedflow.desktop.startup

import java.io.File

/** Entry point of the child JVM in [SingleInstanceLockTest]: holds the lock until stdin closes. */
fun main(args: Array<String>) {
    val result = SingleInstanceLock().acquire(File(args[0]))
    println(if (result == SingleInstanceLock.Result.Acquired) "LOCKED" else "NOT_LOCKED $result")
    System.out.flush()
    System.`in`.read()
}
