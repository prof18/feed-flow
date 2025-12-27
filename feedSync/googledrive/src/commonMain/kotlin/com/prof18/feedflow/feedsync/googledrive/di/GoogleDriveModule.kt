package com.prof18.feedflow.feedsync.googledrive.di

import com.prof18.feedflow.core.utils.AppEnvironment
import org.koin.core.module.Module

expect fun googleDriveModule(appEnvironment: AppEnvironment): Module
