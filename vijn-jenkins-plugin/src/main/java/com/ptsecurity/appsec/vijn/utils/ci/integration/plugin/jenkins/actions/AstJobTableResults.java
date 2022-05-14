package com.ptsecurity.appsec.vijn.utils.ci.integration.plugin.jenkins.actions;

import com.ptsecurity.appsec.vijn.utils.ci.integration.Resources;
import com.ptsecurity.appsec.vijn.utils.ci.integration.plugin.jenkins.Plugin;
import com.ptsecurity.appsec.vijn.utils.ci.integration.plugin.jenkins.actions.AstJobMultipleResults.BuildScanResult;
import com.ptsecurity.appsec.vijn.utils.ci.integration.plugin.jenkins.charts.GenericChartDataModel;
import com.ptsecurity.appsec.vijn.utils.ci.integration.plugin.jenkins.domain.Issue;
import com.ptsecurity.appsec.vijn.utils.ci.integration.plugin.jenkins.domain.Result;
import com.ptsecurity.appsec.vijn.utils.ci.integration.plugin.jenkins.misc.BaseJsonHelper;
import com.ptsecurity.appsec.vijn.utils.ci.integration.plugin.jenkins.misc.PackedData;
import hudson.model.Action;
import hudson.model.Job;
import hudson.model.Run;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.ptsecurity.appsec.vijn.utils.ci.integration.plugin.jenkins.charts.BaseJsonChartDataModel.*;

/**
 * Class generates detailed statistics chart models that are used on a project level
 */
@Slf4j
@RequiredArgsConstructor
public class AstJobTableResults implements Action {
    /**
     * Project that statistics belongs to
     */
    @Getter
    @NonNull
    private final Job<?, ?> project;

    @NonNull
    public List<BuildScanResult> getLatestScanResults(final int number) {
        log.trace("Load latest {} scan results", number);
        final List<? extends Run<?, ?>> builds = project.getBuilds();
        final List<BuildScanResult> scanResults = new ArrayList<>();

        int count = 0;
        for (Run<?, ?> build : builds) {
            Result scanResult = null;
            // noinspection ConstantConditions
            do {
                final AstJobSingleResult action = build.getAction(AstJobSingleResult.class);
                if (null == action || null == action.getPackedData()) break;
                PackedData packedData = action.getPackedData();
                if (!packedData.getType().equals(PackedData.Type.VIJN_RESULT_V1)) break;
                scanResult = PackedData.unpackData(packedData.getData(), Result.class);
            } while (false);

            scanResults.add(new BuildScanResult(build.getNumber(), scanResult));
            // Only chart the last N builds (max)
            count++;
            if (count == number) break;
        }
        return scanResults;
    }

    protected GenericChartDataModel.Series createTotalIssuesCountSeries(@NonNull final List<BuildScanResult> buildScanResults) {
        final String totalVulnerabilitiesItemCaption = Resources.i18n_misc_enums_vulnerability_total();
        GenericChartDataModel.Series res = GenericChartDataModel.Series.builder()
                .name(totalVulnerabilitiesItemCaption)
                .itemStyle(GenericChartDataModel.Series.DataItem.ItemStyle.builder()
                        .color("#d0d0d0")
                        .build())
                .build();
        for (BuildScanResult buildScanResult : buildScanResults) {
            long count = 0;
            // noinspection ConstantConditions
            do {
                Result scanResult = buildScanResult.getScanResult();
                if (null == scanResult || scanResult.getIssues().isEmpty()) break;
                count = scanResult.getIssues().size();
            } while (false);
            res.getData().add(GenericChartDataModel.Series.DataItem.builder().value(count).build());
        }
        return res;
    }

    @SneakyThrows
    @SuppressWarnings("unused") // Called by groovy view
    public String getFalsePositiveHistoryChart(final int resultsNumber) {
        final List<BuildScanResult> buildScanResults = getLatestScanResults(resultsNumber);
        // Prepare X-axis
        GenericChartDataModel.Axis xAxis = GenericChartDataModel.Axis.builder().build();
        GenericChartDataModel.Axis yAxis = GenericChartDataModel.Axis.builder().build();
        GenericChartDataModel.Legend legend = GenericChartDataModel.Legend.builder().build();
        log.trace("Sort scan results by build number");
        buildScanResults.sort(Comparator.comparing(BuildScanResult::getBuildNumber));

        List<GenericChartDataModel.Series> chartSeries = new ArrayList<>();
        log.trace("Add 'total issues count' series and legend item");
        GenericChartDataModel.Series totalVulnerabilityCountSeries = createTotalIssuesCountSeries(buildScanResults);
        chartSeries.add(totalVulnerabilityCountSeries);
        legend.data.add(totalVulnerabilityCountSeries.getName());

        for (BuildScanResult buildScanResult : buildScanResults)
            xAxis.getData().add(buildScanResult.getBuildNumber().toString());

        for (Issue.FalsePositiveState state : Issue.FalsePositiveState.values()) {
            GenericChartDataModel.Series valueSeries
                    = GenericChartDataModel.Series.builder()
                    .name(state.i18n())
                    .itemStyle(GenericChartDataModel.Series.DataItem.ItemStyle.builder()
                            .color("#" + Integer.toHexString(FALSE_POSITIVE_COLORS.get(state.isValue())))
                            .build())
                    .build();
            // Prepare series to fill with data
            for (BuildScanResult buildScanResult : buildScanResults) {
                long count = 0;
                // noinspection ConstantConditions
                do {
                    Result scanResult = buildScanResult.getScanResult();
                    if (null == scanResult || null == scanResult.getIssues() || scanResult.getIssues().isEmpty()) break;
                    count = scanResult.getIssues().stream()
                            .filter(issue -> state.isValue() == issue.isFalsePositive())
                            .count();
                } while (false);
                valueSeries.getData().add(GenericChartDataModel.Series.DataItem.builder().value(count).build());
            }
            // Do not skip series with no data
            // if (valueSeries.getData().stream().noneMatch(i -> i.getValue() != 0)) continue;
            chartSeries.add(valueSeries);
            legend.data.add(state.i18n());
        }

        GenericChartDataModel chartDataModel = GenericChartDataModel.builder()
                .legend(legend)
                .xaxis(Collections.singletonList(xAxis))
                .yaxis(Collections.singletonList(yAxis))
                .series(chartSeries)
                .build();
        return BaseJsonHelper.createObjectMapper().writeValueAsString(chartDataModel);
    }

    @SneakyThrows
    @SuppressWarnings("unused") // Called by groovy view
    public String getFixedStateHistoryChart(final int resultsNumber) {
        final List<BuildScanResult> buildScanResults = getLatestScanResults(resultsNumber);
        // Prepare X-axis
        GenericChartDataModel.Axis xAxis = GenericChartDataModel.Axis.builder().build();
        GenericChartDataModel.Axis yAxis = GenericChartDataModel.Axis.builder().build();
        GenericChartDataModel.Legend legend = GenericChartDataModel.Legend.builder().build();
        log.trace("Sort scan results by build number");
        buildScanResults.sort(Comparator.comparing(BuildScanResult::getBuildNumber));

        List<GenericChartDataModel.Series> chartSeries = new ArrayList<>();
        log.trace("Add 'total issues count' series and legend item");
        GenericChartDataModel.Series totalVulnerabilityCountSeries = createTotalIssuesCountSeries(buildScanResults);
        chartSeries.add(totalVulnerabilityCountSeries);
        legend.data.add(totalVulnerabilityCountSeries.getName());

        for (BuildScanResult buildScanResult : buildScanResults)
            xAxis.getData().add(buildScanResult.getBuildNumber().toString());

        for (Issue.FixedState state : Issue.FixedState.values()) {
            GenericChartDataModel.Series valueSeries
                    = GenericChartDataModel.Series.builder()
                    .name(state.i18n())
                    .itemStyle(GenericChartDataModel.Series.DataItem.ItemStyle.builder()
                            .color("#" + Integer.toHexString(FIX_STATE_COLORS.get(state.isValue())))
                            .build())
                    .build();
            // Prepare series to fill with data
            for (BuildScanResult buildScanResult : buildScanResults) {
                long count = 0;
                // noinspection ConstantConditions
                do {
                    Result scanResult = buildScanResult.getScanResult();
                    if (null == scanResult || null == scanResult.getIssues() || scanResult.getIssues().isEmpty()) break;
                    count = scanResult.getIssues().stream()
                            .filter(issue -> state.isValue() == issue.isFalsePositive())
                            .count();
                } while (false);
                valueSeries.getData().add(GenericChartDataModel.Series.DataItem.builder().value(count).build());
            }
            // Do not skip series with no data
            // if (valueSeries.getData().stream().noneMatch(i -> i.getValue() != 0)) continue;
            chartSeries.add(valueSeries);
            legend.data.add(state.i18n());
        }

        GenericChartDataModel chartDataModel = GenericChartDataModel.builder()
                .legend(legend)
                .xaxis(Collections.singletonList(xAxis))
                .yaxis(Collections.singletonList(yAxis))
                .series(chartSeries)
                .build();
        return BaseJsonHelper.createObjectMapper().writeValueAsString(chartDataModel);
    }

    @SneakyThrows
    public String getLevelHistoryChart(final int resultsNumber) {
        final List<BuildScanResult> buildScanResults = getLatestScanResults(resultsNumber);
        // Prepare X-axis
        GenericChartDataModel.Axis xAxis = GenericChartDataModel.Axis.builder().build();
        GenericChartDataModel.Axis yAxis = GenericChartDataModel.Axis.builder().build();
        GenericChartDataModel.Legend legend = GenericChartDataModel.Legend.builder().build();
        // Sort scan results by build number
        buildScanResults.sort(Comparator.comparing(BuildScanResult::getBuildNumber));

        List<GenericChartDataModel.Series> chartSeries = new ArrayList<>();
        // Add "total issues count" series and legend item
        GenericChartDataModel.Series totalVulnerabilityCountSeries = createTotalIssuesCountSeries(buildScanResults);
        chartSeries.add(totalVulnerabilityCountSeries);
        legend.data.add(totalVulnerabilityCountSeries.getName());

        for (BuildScanResult buildScanResult : buildScanResults)
            xAxis.getData().add(buildScanResult.getBuildNumber().toString());
        for (Issue.Level level : Issue.Level.values()) {
            GenericChartDataModel.Series valueSeries
                    = GenericChartDataModel.Series.builder()
                    .name(level.i18n())
                    .itemStyle(GenericChartDataModel.Series.DataItem.ItemStyle.builder()
                            .color("#" + Integer.toHexString(LEVEL_COLORS.get(level)))
                            .build())
                    .build();
            // Prepare series to fill with data
            for (BuildScanResult buildScanResult : buildScanResults) {
                long count = 0;
                // noinspection ConstantConditions
                do {
                    Result scanResult = buildScanResult.getScanResult();
                    if (null == scanResult || null == scanResult.getIssues() || scanResult.getIssues().isEmpty()) break;
                    count = scanResult.getIssues().stream()
                            .filter(baseIssue -> level == baseIssue.getLevel())
                            .count();
                } while (false);
                valueSeries.getData().add(GenericChartDataModel.Series.DataItem.builder().value(count).build());
            }
            // Do not skip series with no data
            // if (valueSeries.getData().stream().noneMatch(i -> i.getValue() != 0)) continue;
            chartSeries.add(valueSeries);
            legend.data.add(level.i18n());
        }

        GenericChartDataModel chartDataModel = GenericChartDataModel.builder()
                .legend(legend)
                .xaxis(Collections.singletonList(xAxis))
                .yaxis(Collections.singletonList(yAxis))
                .series(chartSeries)
                .build();
        return BaseJsonHelper.createObjectMapper().writeValueAsString(chartDataModel);
    }

    @Override
    public String getIconFileName() {
        return Plugin.getPluginUrl() + "/icons/logo.svg";
    }

    @Override
    public String getDisplayName() {
        return Resources.i18n_vijn_result_charts_statistics_label();
    }

    @Override
    public String getUrlName() {
        return "vijn";
    }
}
