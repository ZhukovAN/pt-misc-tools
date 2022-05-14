package com.ptsecurity.appsec.vijn.utils.ci.integration.plugin.jenkins.Plugin

import com.ptsecurity.appsec.vijn.utils.ci.integration.plugin.jenkins.descriptor.PluginDescriptor
import lib.FormTagLib

def f = namespace(FormTagLib);

div() {
    text(_("about"))
}

div() {
    String version = PluginDescriptor.getVersion()
    text(_("version.info"))
    text(version)
}
