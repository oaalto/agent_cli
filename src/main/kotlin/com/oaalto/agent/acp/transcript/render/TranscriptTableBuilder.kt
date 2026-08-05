package com.oaalto.agent.acp.transcript.render

internal object TranscriptTableBuilder {
    fun buildTableHtml(table: RenderedBlock.Table): String {
        val escapedHeaders = table.headers.map { TranscriptRenderHelpers.escapeHtml(it) }
        val escapedRows = table.rows.map { row -> row.map { TranscriptRenderHelpers.escapeHtml(it) } }
        val alignStyles =
            table.alignments.map { align ->
                when (align) {
                    TableAlignment.LEFT -> "text-align:left"
                    TableAlignment.CENTER -> "text-align:center"
                    TableAlignment.RIGHT -> "text-align:right"
                }
            }
        val cellStyle = "border:1px solid #555;padding:4px"
        val headerCells =
            escapedHeaders
                .mapIndexed { i, h ->
                    val align = alignStyles.getOrElse(i) { "text-align:left" }
                    "<th style='$cellStyle;$align'>$h</th>"
                }.joinToString("")
        val bodyCells =
            escapedRows.joinToString("\n") { row ->
                "<tr>${
                    row.mapIndexed { i, cell ->
                        val align = alignStyles.getOrElse(i) { "text-align:left" }
                        "<td style='$cellStyle;$align'>$cell</td>"
                    }.joinToString("")
                }</tr>"
            }
        return (
            "<div style='margin-left:20px;overflow-x:auto'>" +
                "<table style='border-collapse:collapse;width:auto'>" +
                "<thead><tr>$headerCells</tr></thead>" +
                "<tbody>$bodyCells</tbody>" +
                "</table></div>"
        )
    }
}
