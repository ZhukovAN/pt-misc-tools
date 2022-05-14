package com.ptsecurity.appsec.vijn.utils.ci.integration.plugin.jenkins.descriptor;

import com.ptsecurity.appsec.vijn.utils.ci.integration.Resources;
import com.ptsecurity.appsec.vijn.utils.ci.integration.plugin.jenkins.Plugin;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

@Slf4j
@Extension
@Symbol("vijn")
public class PluginDescriptor extends BuildStepDescriptor<Builder> {

    public PluginDescriptor() {
        super(Plugin.class);
        load();
    }

    private int lastElementId = 0;

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> jobType) {
        return true;
    }

    @Override
    @Nonnull
    public String getDisplayName() {
        return Resources.i18n_vijn_plugin_label();
    }

    protected static Map<String, String> versionInfo = null;

    @NonNull
    public static String getVersion() {
        Map<String, String> version = getVersionInfo();
        StringBuilder builder = new StringBuilder();
        if (StringUtils.isNotEmpty(version.get("Implementation-Version")))
            builder.append(" v.").append(version.get("Implementation-Version"));
        if (StringUtils.isNotEmpty(version.get("Implementation-Git-Hash")))
            builder.append("-").append(version.get("Implementation-Git-Hash"));
        if (StringUtils.isNotEmpty(version.get("Build-Time")))
            builder.append(" built on ").append(version.get("Build-Time"));
        return builder.toString();
    }

    public static Map<String, String> getVersionInfo() {
        if (null != versionInfo) return versionInfo;
        versionInfo = new HashMap<>();
        try {
            Enumeration<URL> res = PluginDescriptor.class.getClassLoader().getResources("META-INF/MANIFEST.MF");
            while (res.hasMoreElements()) {
                URL url = res.nextElement();
                Manifest manifest = new Manifest(url.openStream());

                if (!isApplicableManifest(manifest)) continue;
                Attributes attr = manifest.getMainAttributes();
                versionInfo.put("Implementation-Version", get(attr, "Implementation-Version").toString());
                versionInfo.put("Implementation-Git-Hash", get(attr, "Implementation-Git-Hash").toString());
                versionInfo.put("Build-Time", get(attr, "Build-Time").toString());
                break;
            }
        } catch (IOException e) {
            log.warn("Failed to get build info from plugin metadata");
            log.debug("Exception details", e);
        }
        return versionInfo;
    }

    private static boolean isApplicableManifest(Manifest manifest) {
        Attributes attributes = manifest.getMainAttributes();
        return "com.ptsecurity.appsec.vijn.utils.ci.integration".equals(get(attributes, "Implementation-Vendor-Id"));
    }

    private static Object get(Attributes attributes, String key) {
        return attributes.get(new Attributes.Name(key));
    }
}
