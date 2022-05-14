package com.ptsecurity.appsec.vijn.utils.ci.integration.plugin.jenkins.actions.Utils

import com.ptsecurity.appsec.vijn.utils.ci.integration.Resources
import groovy.xml.MarkupBuilder

abstract class AbstractUI {
    def charts = []
    def chartsMap = [:]

    AbstractUI(String prefix) {
        addCharts(prefix)
        for (Chart chart : charts)
            chartsMap[chart.type] = chart
    }

    abstract addCharts(String prefix);
}
