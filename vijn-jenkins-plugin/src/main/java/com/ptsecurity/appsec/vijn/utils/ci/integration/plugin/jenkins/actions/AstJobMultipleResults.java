package com.ptsecurity.appsec.vijn.utils.ci.integration.plugin.jenkins.actions;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ptsecurity.appsec.vijn.utils.ci.integration.plugin.jenkins.charts.BaseJsonChartDataModel;
import com.ptsecurity.appsec.vijn.utils.ci.integration.plugin.jenkins.charts.GenericChartDataModel;
import com.ptsecurity.appsec.vijn.utils.ci.integration.plugin.jenkins.domain.Result;
import com.ptsecurity.appsec.vijn.utils.ci.integration.plugin.jenkins.misc.PackedData;
import hudson.model.Action;
import hudson.model.Job;
import hudson.model.Run;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import java.util.ArrayList;
import java.util.List;

/**
 * Class implements project-scope basic chart that is shown at project page as vulnerabilities trend
 */
@Slf4j
@RequiredArgsConstructor
public class AstJobMultipleResults implements Action {
    @NonNull
    private final Job<?, ?> project;

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public String getUrlName() {
        return "vijnTrend";
    }

    @Getter
    @Setter
    @RequiredArgsConstructor
    public static class BuildScanResult {
        @JsonProperty
        protected final Integer buildNumber;
        @JsonProperty
        protected final Result scanResult;
    }

    @NonNull
    protected List<BuildScanResult> getLatestBuildScanResults(final int number) {
        log.trace("Get {} project builds", project.getName());
        final List<? extends Run<?, ?>> builds = project.getBuilds();
        final List<BuildScanResult> scanResults = new ArrayList<>();

        int count = 0;
        for (Run<?, ?> build : builds) {
            Result result = null;
            // noinspection ConstantConditions
            do {
                log.trace("trying to extract Vijn results from {} build", build.getNumber());
                final AstJobSingleResult action = build.getAction(AstJobSingleResult.class);
                if (null == action) break;
                if (null == action.getPackedData()) break;
                PackedData packedData = action.getPackedData();
                if (!packedData.getType().equals(PackedData.Type.VIJN_RESULT_V1)) break;
                result = PackedData.unpackData(packedData.getData(), Result.class);
                log.trace("Vijn results extraction success");
            } while (false);

            scanResults.add(new BuildScanResult(build.getNumber(), result));
            // Only chart the last N builds (max)
            count++;
            if (count == number) break;
        }
        return scanResults;
    }

    /**
     * Returns the UI model for an ECharts stacked area chart that shows the issues stacked by severity.
     * @return the UI model as JSON
     */
    @JavaScriptMethod
    @SuppressWarnings("unused") // Called by groovy view
    public JSONObject getVulnerabilityLevelTrendChart(final int resultsNumber) {
        final List<BuildScanResult> buildScanResults = getLatestBuildScanResults(resultsNumber);
        return BaseJsonChartDataModel.convertObject(GenericChartDataModel.create(buildScanResults));
    }
}
