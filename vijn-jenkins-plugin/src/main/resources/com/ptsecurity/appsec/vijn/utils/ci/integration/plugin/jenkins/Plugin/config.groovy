package com.ptsecurity.appsec.vijn.utils.ci.integration.plugin.jenkins.Plugin

import com.ptsecurity.appsec.vijn.utils.ci.integration.Resources
import lib.FormTagLib

def f = namespace(FormTagLib)

f.entry(
        title: "${ Resources.i18n_vijn_settings_fileName_label()}",
        field: 'fileName') {
    f.textbox()
}
