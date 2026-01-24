package com.prof18.feedflow.shared.test

import com.prof18.feedflow.shared.domain.csv.CsvInput
import com.prof18.feedflow.shared.domain.csv.CsvOutput
import com.prof18.feedflow.shared.domain.opml.OpmlInput
import com.prof18.feedflow.shared.domain.opml.OpmlOutput

expect fun createOpmlInput(content: String): OpmlInput

expect fun createOpmlOutput(): OpmlOutput

expect fun createCsvInput(content: String): CsvInput

expect fun createCsvOutput(): CsvOutput

expect fun createFailingCsvOutput(): CsvOutput
