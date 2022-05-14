package com.ptsecurity.appsec.vijn.utils.ci.integration.plugin.jenkins;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ptsecurity.appsec.vijn.utils.ci.integration.Resources;
import com.ptsecurity.appsec.vijn.utils.ci.integration.plugin.jenkins.actions.AstJobSingleResult;
import com.ptsecurity.appsec.vijn.utils.ci.integration.plugin.jenkins.descriptor.PluginDescriptor;
import com.ptsecurity.appsec.vijn.utils.ci.integration.plugin.jenkins.domain.Result;
import com.ptsecurity.appsec.vijn.utils.ci.integration.plugin.jenkins.misc.PackedData;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.Builder;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.*;

import static org.apache.commons.lang3.StringUtils.trimToNull;

@Slf4j
@ToString
public class Plugin extends Builder implements SimpleBuildStep {
    private static final String CONSOLE_PREFIX = "AbstractTool.DEFAULT_LOG_PREFIX";

    @Getter
    private final String fileName;

    @DataBoundConstructor
    public Plugin(final String fileName) {
        this.fileName = fileName;
    }

    private TreeMap<String, String> getEnvironmentVariables(final Run<?, ?> build, final TaskListener listener) {
        try {
            final TreeMap<String, String> env = build.getEnvironment(listener);
            if (build instanceof AbstractBuild<?,?>) {
                AbstractBuild<?, ?> abstractBuild = (AbstractBuild<?, ?>) build;
                env.putAll(abstractBuild.getBuildVariables());
            }
            return env;
        } catch (Exception e) {
            throw new RuntimeException(Resources.i18n_vijn_result_status_failed_environment_label(), e);
        }
    }

    @Override
    public void perform(@Nonnull Run<?, ?> build, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws IOException, InterruptedException {
        FilePath filePath = workspace.child(fileName);
        Result result = new ObjectMapper().readValue(filePath.read(), Result.class);

        PackedData packedData = new PackedData(PackedData.Type.VIJN_RESULT_V1, PackedData.packData(result));
        AstJobSingleResult action = new AstJobSingleResult(build);
        action.setPackedData(packedData);
        build.addAction(action);
    }

    @NonNull
    public static String getPluginUrl() {
        return "/plugin/" + Objects.requireNonNull(Jenkins.get().getPluginManager().getPlugin("vijn-jenkins-plugin")).getShortName();
    }

    @Override
    public PluginDescriptor getDescriptor() {
        return Jenkins.get().getDescriptorByType(PluginDescriptor.class);
    }

    protected static String getCurrentItem(Run<?, ?> run, String currentItem){
        String runItem = null;
        String curItem = trimToNull(currentItem);
        if(run != null && run.getParent() != null)
            runItem = trimToNull(run.getParent().getFullName());

        if(runItem != null && curItem != null) {
            if(runItem.equals(curItem)) {
                return runItem;
            } else {
                throw new IllegalArgumentException(String.format("Current Item ('%s') and Parent Item from Run ('%s') differ!", curItem, runItem));
            }
        } else if(runItem != null) {
            return runItem;
        } else if(curItem != null) {
            return curItem;
        } else {
            throw new IllegalArgumentException("Both null, Run and Current Item!");
        }
    }

    protected List<Action> projectActions;

    @Override
    @NonNull
    public Collection<? extends Action> getProjectActions(AbstractProject<?, ?> project) {
        if (null == projectActions) projectActions = new ArrayList<>();
        return projectActions;
    }
}