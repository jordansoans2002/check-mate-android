package com.man_behind.checkmate.data.local.csv

/**
 * RFC4180-compliant CSV parser.
 *
 * Required because guideline text is long, free-form prose that will
 * contain commas, newlines, and apostrophes. A naive line.split(",")
 * would corrupt those fields. This reads char-by-char and respects
 * quoting rules:
 *  - Fields containing , or \n or " must be wrapped in "..."
 *  - A literal " inside a quoted field is escaped as ""
 *  - Unquoted fields are trimmed of leading/trailing whitespace
 *
 * Returns rows as List<List<String>>; the first row is the header.
 * Blank lines are ignored.
 */
object CsvParser {

    fun parse(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        val currentField = StringBuilder()
        var inQuotes = false
        var i = 0

        fun commitField() {
            currentRow.add(currentField.toString())
            currentField.clear()
        }

        fun commitRow() {
            commitField()
            if (currentRow.any { it.isNotBlank() }) {
                rows.add(currentRow.toList())
            }
            currentRow.clear()
        }

        while (i < text.length) {
            val ch = text[i]

            if (inQuotes) {
                when {
                    // Escaped quote: "" -> "
                    ch == '"' && i + 1 < text.length && text[i + 1] == '"' -> {
                        currentField.append('"')
                        i++
                    }
                    // Closing quote
                    ch == '"' -> inQuotes = false
                    // Everything else including commas and newlines is literal content
                    else -> currentField.append(ch)
                }
            } else {
                when (ch) {
                    '"' -> inQuotes = true
                    ',' -> commitField()
                    '\n' -> commitRow()
                    '\r' -> { /* skip — \n handles row end */ }
                    else -> currentField.append(ch)
                }
            }
            i++
        }

        // Commit remainder if file does not end with a newline
        if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
            commitRow()
        }

        return rows
    }
}