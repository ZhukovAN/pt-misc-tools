package com.ptsecurity.appsec.vijn.utils.ci.integration.plugin.jenkins.actions.AstJobTableResults

import com.ptsecurity.appsec.vijn.utils.ci.integration.Resources
import com.ptsecurity.appsec.vijn.utils.ci.integration.plugin.jenkins.actions.AstJobTableResults
import com.ptsecurity.appsec.vijn.utils.ci.integration.plugin.jenkins.actions.Utils.AbstractUI
import com.ptsecurity.appsec.vijn.utils.ci.integration.plugin.jenkins.actions.Utils.Chart
import lib.FormTagLib
import lib.LayoutTagLib

def f = namespace(FormTagLib)
def l = namespace(LayoutTagLib)
def st = namespace("jelly:stapler")

def widthOffset = 100;
def smallChartHeight = 200;
def smallChartMinWidth = 450;
def smallChartGap = 16;
def bigChartMinWidth = smallChartMinWidth * 2 + smallChartGap;
def smallChartStyle = "min-width: ${smallChartMinWidth}px; background-color: #f8f8f8f8; ";
def bigChartStyle = "min-width: " + bigChartMinWidth + "px; background-color: #f8f8f8f8; ";
def bigDivStyle = "width: ${widthOffset}%; margin: 0 auto; min-width: " + bigChartMinWidth + "px; display: grid; grid-template-columns: 50% 50%; ";
def tableStyle = "width: ${widthOffset}%; margin: 0 auto; min-width: ${bigChartMinWidth}px; border-collapse: collapse; margin-top: 10px; "

def historyLength = 10;

// Make groovy values available for JavaScript
script """
    const smallChartHeight = ${smallChartHeight};
    const smallChartMinWidth = ${smallChartMinWidth};
    const smallChartGap = ${smallChartGap};
    const bigChartMinWidth = ${bigChartMinWidth};
    const smallChartStyle = '${smallChartStyle}';
    const bigChartStyle = '${bigChartStyle}';
"""

link(rel: 'stylesheet', href: "${rootURL}/plugin/vijn-jenkins-plugin/css/plugin.css")
script(src: "${rootURL}/plugin/vijn-jenkins-plugin/webjars/echarts/echarts.min.js")
script(src: "${rootURL}/plugin/vijn-jenkins-plugin/js/utils.js")
script(src: "${rootURL}/plugin/vijn-jenkins-plugin/js/charts.js")

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

def createChartPlaceholder(AstJobTableResults owner, Chart chart) {
    createChartPlaceholder(chart.col, chart.row, chart.width, owner.urlName, chart.name, chart.title)
}

class UI extends AbstractUI {
    UI(String prefix) {
        super(prefix)
    }

    @Override
    def addCharts(String prefix) {
        charts.add(new Chart(Chart.Type.LEVELS_HISTORY_BAR, 1, 1, 2, prefix, "levels-history-bar-chart", Resources.i18n_vijn_result_charts_level_label()))
        charts.add(new Chart(Chart.Type.FALSE_POSITIVE_HISTORY_BAR, 1, 2, 1, prefix, "false-positive-history-bar-chart", Resources.i18n_vijn_result_charts_falsepositive_label()))
        charts.add(new Chart(Chart.Type.FIXED_ISSUES_HISTORY_BAR, 2, 2, 1, prefix, "fixed-issues-history-bar-chart", Resources.i18n_vijn_result_charts_fixed_label()))
    }
}

UI ui = new UI((my as AstJobTableResults).urlName)

l.layout(title: "PT AI AST report") {
    l.side_panel() {
        st.include(page: "sidepanel.jelly", it: my.project)
    }
    l.main_panel() {
        h1(Resources.i18n_vijn_result_charts_statistics_label())
        def latestResults = my.getLatestScanResults(historyLength)
        if (null == latestResults || latestResults.isEmpty()) {
            div(id: "${my.urlName}-no-data", class: "h2 vijn-no-data") {
                text("${Resources.i18n_vijn_result_charts_message_noscans_label().toUpperCase()}")
            }
            return
        }
        h2(id: "h2", Resources.i18n_vijn_result_statistics_breakdown_label())
        div(class: "vijn-main-content vijn-charts-div") {
            for (Chart chart : ui.charts) createChartPlaceholder(my, chart)
        }
        script """
            createBuildHistoryChart(
                "${ui.chartsMap[Chart.Type.LEVELS_HISTORY_BAR].divId}", 
                ${my.getLevelHistoryChart(historyLength)}, null);
            
            createBuildHistoryChart(
                "${ui.chartsMap[Chart.Type.FALSE_POSITIVE_HISTORY_BAR].divId}", 
                ${my.getFalsePositiveHistoryChart(historyLength)}, null);

            createBuildHistoryChart(
                "${ui.chartsMap[Chart.Type.FIXED_ISSUES_HISTORY_BAR].divId}", 
                ${my.getFixedStateHistoryChart(historyLength)}, null);
            
        """
    }
}
