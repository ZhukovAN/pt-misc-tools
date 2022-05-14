package com.ptsecurity.appsec.vijn.utils.ci.integration.plugin.jenkins.actions.AstJobSingleResult

import com.ptsecurity.appsec.vijn.utils.ci.integration.Resources
import lib.FormTagLib

def t = namespace('/lib/hudson')

t.summary(icon: my.getIconFileName()) {
    def scanResult = my.loadScanResult()
    div() {
        b(Resources.i18n_vijn_plugin_label())
        ul() {
            li() {
                text(Resources.i18n_vijn_result_url() + ": ")
                a(href: scanResult.url) { text(scanResult.url) }
            }
            li() {
                text(Resources.i18n_vijn_result_sharedlink() + ": ")
                a(href: scanResult.sharedLink) { text(scanResult.sharedLink) }
            }
        }
    }
}