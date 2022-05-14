package com.ptsecurity.appsec.vijn.utils.ci.integration.plugin.jenkins.actions.AstJobSingleResult

import com.ptsecurity.appsec.vijn.utils.ci.integration.Resources
import com.ptsecurity.appsec.vijn.utils.ci.integration.plugin.jenkins.actions.AstJobSingleResult
import com.ptsecurity.appsec.vijn.utils.ci.integration.plugin.jenkins.actions.Utils.AbstractUI
import com.ptsecurity.appsec.vijn.utils.ci.integration.plugin.jenkins.actions.Utils.Chart
import lib.LayoutTagLib

def l = namespace(LayoutTagLib)
def st = namespace("jelly:stapler")

link(rel: 'stylesheet', href: "${rootURL}/plugin/vijn-jenkins-plugin/css/plugin.css")
script(src: "${rootURL}/plugin/vijn-jenkins-plugin/webjars/echarts/echarts.min.js")
script(src: "${rootURL}/plugin/vijn-jenkins-plugin/js/utils.js")
script(src: "${rootURL}/plugin/vijn-jenkins-plugin/js/charts.js")

/**
 * Set AST settings table cell class in accordance with its coordinates. Like bold
 * line for left border in the leftmost cells etc.
 * @param x Table column number
 * @param y Table row number
 * @param w Table width to check if cell is a leftmost etc.
 * @param h Table height
 * @return Cell class
 */
static def astSettingsTableCellClass(int x, int y, int w, int h) {
    def clazz = "vijn-cell "
    // Set left border for leftmost cell
    if (0 == x) clazz += "vijn-cell-left "
    // ... and right for rightmost
    if (w - 1 == x) clazz += "vijn-cell-right "
    // The same for topmost
    if (0 == y) clazz += "vijn-cell-top "
    /// ... and lowermost
    if (h - 1 == y) clazz += "vijn-cell-bottom "

    return clazz
}

/**
 * Add table that holds AST settings
 * @param data Map that contains name : value pairs for AST settings
 * @param boldLine Color of bold line that represents left table border
 * @param border Table external border color (top, right and bottom)
 * @param background table background color
 * @return Table with AST settings
 */
def showAstSettingsTable(data) {
    table(class: "vijn-main-content vijn-settings-table") {
        colgroup() {
            col(width: "300px")
        }
        tbody() {
            // For each map element create table cell with dynamically generated style that corresponds its position
            data.eachWithIndex{key, value, i ->
                tr() {
                    [0, 1].each { j ->
                        td(align: "left", class: "${astSettingsTableCellClass(j, i, 2, data.size())}") {
                            text(0 == j ? key : value)
                        }
                    }
                }
            }
        }
    }
}

def createChartPlaceholder(int col, int row, int width, String prefix, String name, String title) {
    String style = "grid-area: ${row} / ${col} / span 1 / span ${width}; "

    // Need to add grid cells spacing if we have multiple charts in row
    String clazz = ""
    if (1 == width)
        clazz = 1 == col ? "vijn-chart-left" : "vijn-chart-right"
    div(style: style, class: clazz) {
        h3(title)
        div(
                id: "${prefix}-${name}",
                class: "graph-cursor-pointer vijn-chart ${1 == width ? "vijn-small-chart" : "vijn-big-chart"} ") {
        }
        div(id : "${prefix}-${name}-no-data", class: "h3 vijn-no-data") {
            text(Resources.i18n_vijn_result_charts_message_nodata_label().toUpperCase())
        }
    }
}

def createChartPlaceholder(AstJobSingleResult owner, Chart chart) {
    createChartPlaceholder(chart.col, chart.row, chart.width, owner.urlName, chart.name, chart.title)
}

class UI extends AbstractUI {
    UI(String prefix) {
        super(prefix)
    }

    @Override
    def addCharts(String prefix) {
        charts.add(new Chart(Chart.Type.LEVELS_BAR, 1, 1, 2, prefix, "levels-bar-chart", Resources.i18n_vijn_result_charts_level_no_fp_label()))
        charts.add(new Chart(Chart.Type.FALSE_POSITIVE_PIE, 1, 2, 1, prefix, "false-positive-pie-chart", Resources.i18n_vijn_result_charts_falsepositive_label()))
        charts.add(new Chart(Chart.Type.FIXED_ISSUES_PIE, 2, 2, 1, prefix, "fixed-issues-pie-chart", Resources.i18n_vijn_result_charts_fixed_label()))
        charts.add(new Chart(Chart.Type.LEVELS_CATEGORY_BAR, 1, 3, 2, prefix, "levels-category-bar-chart", Resources.i18n_vijn_result_charts_issuecategory_no_fp_label()))
        charts.add(new Chart(Chart.Type.LEVELS_TITLE_BAR, 1, 4, 2, prefix, "levels-title-bar-chart", Resources.i18n_vijn_result_charts_issuetitle_no_fp_label()))
    }
}

UI ui = new UI((my as AstJobSingleResult).urlName)

l.layout(title: "PT AI AST report") {
    l.side_panel() {
        st.include(page: "sidepanel.jelly", from: my.run, it: my.run, optional: true)
    }

    l.main_panel() {
        def scanResult = my.loadScanResult()

        h1(Resources.i18n_vijn_result_label())
        h2(Resources.i18n_vijn_settings_label())
        def scanSettings = [:]
        scanSettings[Resources.i18n_vijn_result_url()] = "${scanResult.url}"
        scanSettings[Resources.i18n_vijn_result_sharedlink()] = "${scanResult.sharedLink}"
        showAstSettingsTable(scanSettings)

        if (my.isEmpty()) return

        h2(Resources.i18n_vijn_result_breakdown_label())
        // Create main charts placeholder grid and initialize it with chart DIVs
        div(class: "vijn-main-content vijn-charts-div") {
            for (Chart chart : ui.charts) createChartPlaceholder(my, chart)
        }

        script """
            // Read / store big bar charts data and find widest Y-axis title width to shift all vertical axes to same position    
            var options = [];
            options["${Chart.Type.LEVELS_BAR.name()}"] = ${my.getVulnerabilityLevelDistribution()}
            options["${Chart.Type.LEVELS_CATEGORY_BAR.name()}"] = ${my.getVulnerabilityCategoryDistribution()}
            options["${Chart.Type.LEVELS_TITLE_BAR.name()}"] = ${my.getVulnerabilityTitleDistribution()}
            // Get widest Y-axis title width
            var strings = options["${Chart.Type.LEVELS_BAR.name()}"].yAxis[0].data
                .concat(options["${Chart.Type.LEVELS_CATEGORY_BAR.name()}"].yAxis[0].data)
                .concat(options["${Chart.Type.LEVELS_TITLE_BAR.name()}"].yAxis[0].data);
            var maxChartTextWidth = maxChartTextWidth(strings);
            
            createDistributionBarChart("${ui.chartsMap[Chart.Type.LEVELS_BAR].divId}", options["${Chart.Type.LEVELS_BAR.name()}"], maxChartTextWidth)
            createDistributionBarChart("${ui.chartsMap[Chart.Type.LEVELS_CATEGORY_BAR].divId}", options["${Chart.Type.LEVELS_CATEGORY_BAR.name()}"], maxChartTextWidth)
            createDistributionBarChart("${ui.chartsMap[Chart.Type.LEVELS_TITLE_BAR].divId}", options["${Chart.Type.LEVELS_TITLE_BAR.name()}"], maxChartTextWidth)
                 
            createDistributionPieChart(
                "${ui.chartsMap[Chart.Type.FALSE_POSITIVE_PIE].divId}", 
                ${my.getVulnerabilityFalsePositiveStatePie()});
            createDistributionPieChart(
                "${ui.chartsMap[Chart.Type.FIXED_ISSUES_PIE].divId}",
                ${my.getVulnerabilityFixedStatePie()});
        """
    }
}
