package com.ptsecurity.appsec.vijn.utils.ci.integration.plugin.jenkins.actions.Utils

class Chart {
    enum Type {
        LEVELS_BAR, FALSE_POSITIVE_PIE, FIXED_ISSUES_PIE, LEVELS_CATEGORY_BAR, LEVELS_TITLE_BAR,
        LEVELS_HISTORY_BAR, FALSE_POSITIVE_HISTORY_BAR, FIXED_ISSUES_HISTORY_BAR
    }
    Type type

    String divId, noDataDivId
    int col, row, width
    String name, title

    Chart(Type type, int col, int row, int width, String prefix, String name, String title) {
        this.type = type
        this.divId = "${prefix}-${name}"
        this.noDataDivId = "${prefix}-${name}-no-data"
        this.col = col
        this.row = row
        this.width = width
        this.name = name
        this.title = title
    }
}


