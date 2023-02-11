package com.prof18.feedflow

import kotlinx.coroutines.Dispatchers
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module


fun initKoin(modules: List<Module>): KoinApplication {
    return startKoin {
        modules(modules + coreModule + platformModule)
    }
}

private val coreModule = module {
    single {
        DatabaseHelper(
            get(),
            Dispatchers.Default
        )
    }
}

expect val platformModule: Module
