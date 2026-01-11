package com.prof18.feedflow.shared.test

import app.cash.sqldelight.db.SqlDriver

expect fun createInMemoryDriver(): SqlDriver
