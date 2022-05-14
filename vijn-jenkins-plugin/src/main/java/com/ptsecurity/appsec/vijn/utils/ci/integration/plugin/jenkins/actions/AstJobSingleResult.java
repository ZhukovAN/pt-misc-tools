package com.ptsecurity.appsec.vijn.utils.ci.integration.plugin.jenkins.actions;

import com.ptsecurity.appsec.vijn.utils.ci.integration.Resources;
import com.ptsecurity.appsec.vijn.utils.ci.integration.plugin.jenkins.Plugin;
import com.ptsecurity.appsec.vijn.utils.ci.integration.plugin.jenkins.charts.GenericChartDataModel;
import com.ptsecurity.appsec.vijn.utils.ci.integration.plugin.jenkins.charts.PieChartDataModel;
import com.ptsecurity.appsec.vijn.utils.ci.integration.plugin.jenkins.domain.Issue;
import com.ptsecurity.appsec.vijn.utils.ci.integration.plugin.jenkins.domain.Result;
import com.ptsecurity.appsec.vijn.utils.ci.integration.plugin.jenkins.misc.BaseJsonHelper;
import com.ptsecurity.appsec.vijn.utils.ci.integration.plugin.jenkins.misc.PackedData;
import hudson.model.Action;
import hudson.model.Run;
import jenkins.model.RunAction2;
import jenkins.tasks.SimpleBuildStep;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;

import static com.ptsecurity.appsec.vijn.utils.ci.integration.plugin.jenkins.charts.BaseJsonChartDataModel.*;
import static com.ptsecurity.appsec.vijn.utils.ci.integration.plugin.jenkins.misc.I18nHelper.*;
import static com.ptsecurity.appsec.vijn.utils.ci.integration.plugin.jenkins.misc.PackedData.Type.VIJN_RESULT_V1;

@Slf4j
@RequiredArgsConstructor
public class AstJobSingleResult implements RunAction2, SimpleBuildStep.LastBuildAction {
    @NonNull
    @Getter
    private transient Run run;

    @Override
    public String getIconFileName() {
        return Plugin.getPluginUrl() + "/icons/logo.svg";
    }

    public String getLogo48() {
        return Plugin.getPluginUrl() + "/icons/logo.48x48.svg";
    }

    @Override
    public String getDisplayName() {
        return Resources.i18n_vijn_result_charts_scan_label();
    }

    @Override
    public String getUrlName() {
        return "vijn";
    }

    @Getter
    @Setter
    protected PackedData packedData;

    protected transient Result scanResult = null;

    public Result loadScanResult() {
        if (null != scanResult) return scanResult;

        if (null == packedData) return null;
        if (VIJN_RESULT_V1 != packedData.getType()) return null;
        scanResult = packedData.unpackData(Result.class);
        /*
        Reports.Locale locale = Export.ExportDescriptor.getDefaultLocale();
        Comparator<ScanBriefDetailed.Details.ChartData.BaseIssueCount> compareLevelTypeAndCount = Comparator
                .comparing(ScanBriefDetailed.Details.ChartData.BaseIssueCount::getLevel, Comparator.comparingInt(BaseIssue.Level::getValue).reversed())
                .thenComparing(ScanBriefDetailed.Details.ChartData.BaseIssueCount::getCount, Comparator.reverseOrder())
                .thenComparing(brief -> brief.getTitle().get(locale), Comparator.reverseOrder());
        */
        return scanResult;
    }

    @Override
    public void onAttached(Run<?, ?> r) {
        this.run = r;
    }

    @Override
    public void onLoad(Run<?, ?> r) {
        this.run = r;
    }

    @Getter
    @RequiredArgsConstructor
    protected static class Couple {
        protected final Issue.Level level;
        protected final Long count;
    }

    @Getter
    @Builder
    @RequiredArgsConstructor
    protected static class Triple {
        protected final Issue.Level level;
        protected final String caption;
        protected final Long count;
    }

    public boolean isEmpty() {
        loadScanResult();
        return (null == scanResult || scanResult.getIssues().isEmpty());
    }

    @SneakyThrows
    @SuppressWarnings("unused") // Called by groovy view
    public String getVulnerabilityLevelDistribution() {
        log.trace("Create vulnerability level distribution chart for single scan result");
        loadScanResult();
        if (isEmpty()) return null;

        log.trace("Count true positive issues by their severity");
        Map<Issue.Level, Long> levelCountMap = scanResult.getIssues().stream()
                .filter(issue -> !issue.isFalsePositive())
                .collect(Collectors.groupingBy(
                        Issue::getLevel,
                        Collectors.counting()));
        log.trace("Create generic chart data model");
        GenericChartDataModel dataModel = GenericChartDataModel.builder()
                .xaxis(Collections.singletonList(GenericChartDataModel.Axis.builder().build()))
                .yaxis(Collections.singletonList(GenericChartDataModel.Axis.builder().build()))
                .series(Collections.singletonList(GenericChartDataModel.Series.builder().build()))
                .build();
        log.trace("Sort issues by severity and count");
        List<Couple> levelCount = new ArrayList<>();
        levelCountMap.forEach((level, count) -> levelCount.add(new Couple(level, count)));
        Comparator<Couple> c = Comparator.comparing(Couple::getLevel, Comparator.comparingInt(Issue.Level::getValue));
        levelCount.stream().sorted(c).forEach(t -> {
            dataModel.getYaxis().get(0).getData().add(t.level.i18n());
            dataModel.getSeries().get(0).getData().add(GenericChartDataModel.Series.DataItem.builder()
                    .value(levelCountMap.get(t.level))
                    .itemStyle(GenericChartDataModel.Series.DataItem.ItemStyle.builder()
                            .color("#" + Integer.toHexString(LEVEL_COLORS.get(t.level)))
                            .build())
                    .build());
        });
        return BaseJsonHelper.createObjectMapper().writeValueAsString(dataModel);
    }

    @SneakyThrows
    @SuppressWarnings("unused") // Called by groovy view
    public String getVulnerabilityTitleDistribution() {
        log.trace("Create vulnerability type distribution chart for single scan result");
        loadScanResult();
        if (isEmpty()) return null;

        log.trace("Count true positive issues by level and title");
        Map<Pair<Issue.Level, String>, Long> levelTitleCountMap = scanResult.getIssues().stream()
                .filter(issue -> !issue.isFalsePositive())
                .collect(Collectors.groupingBy(
                        issue -> new ImmutablePair<>(issue.getLevel(), issue.getTitle()),
                        Collectors.counting()));
        return getVulnerabilityLevelCaptionDistribution(levelTitleCountMap);
    }

    @SneakyThrows
    @SuppressWarnings("unused") // Called by groovy view
    public String getVulnerabilityCategoryDistribution() {
        log.trace("Create vulnerability category distribution chart for single scan result");
        loadScanResult();
        if (isEmpty()) return null;

        log.trace("Count true positive issues by level and category");
        Map<Pair<Issue.Level, String>, Long> levelTitleCountMap = scanResult.getIssues().stream()
                .filter(issue -> !issue.isFalsePositive())
                .collect(Collectors.groupingBy(
                        issue -> new ImmutablePair<>(issue.getLevel(), issue.getCategory()),
                        Collectors.counting()));
        return getVulnerabilityLevelCaptionDistribution(levelTitleCountMap);
    }

    /**
     * @param levelCaptionCountMap Dictionary that maps Severity:Caption pair to number of corresponding vulnerabilities
     * @return
     */
    @SneakyThrows
    protected String getVulnerabilityLevelCaptionDistribution(@NonNull Map<Pair<Issue.Level, String>, Long> levelCaptionCountMap) {
        List<Triple> levelCategoryCount = new ArrayList<>();
        levelCaptionCountMap.forEach((k, v) -> levelCategoryCount.add(Triple.builder()
                .level(k.getLeft())
                .caption(k.getRight())
                .count(v)
                .build()));
        Comparator<Triple> c = Comparator
                .comparing(Triple::getLevel, Comparator.comparingInt(Issue.Level::getValue))
                .thenComparing(Triple::getCount)
                .thenComparing(Triple::getCaption, Comparator.reverseOrder());

        GenericChartDataModel dataModel = GenericChartDataModel.builder()
                .xaxis(Collections.singletonList(GenericChartDataModel.Axis.builder().build()))
                .yaxis(Collections.singletonList(GenericChartDataModel.Axis.builder().build()))
                .series(Collections.singletonList(GenericChartDataModel.Series.builder().build()))
                .build();
        levelCategoryCount.stream().sorted(c).forEach(t -> {
            dataModel.getYaxis().get(0).getData().add(t.getCaption());
            dataModel.getSeries().get(0).getData().add(GenericChartDataModel.Series.DataItem.builder()
                    .value(t.getCount())
                    .itemStyle(GenericChartDataModel.Series.DataItem.ItemStyle.builder()
                            .color("#" + Integer.toHexString(LEVEL_COLORS.get(t.getLevel())))
                            .build())
                    .build());
        });
        return BaseJsonHelper.createObjectMapper().writeValueAsString(dataModel);
    }

    @SneakyThrows
    @SuppressWarnings("unused") // Called by groovy view
    public String getVulnerabilityFixedStatePie() {
        loadScanResult();
        if (isEmpty()) return null;

        PieChartDataModel dataModel = PieChartDataModel.builder()
                .series(Collections.singletonList(PieChartDataModel.Series.builder().build()))
                .build();
        for (Issue.FixedState fixed : Issue.FixedState.values()) {
            long count = scanResult.getIssues()
                    .stream()
                    .filter(issue -> fixed.isValue() == issue.isFixed())
                    .count();
            // I'd like to show legend items even if there's no corresponding data
            // if (0 == count) continue;
            PieChartDataModel.Series.DataItem typeItem = PieChartDataModel.Series.DataItem.builder()
                    .name(fixed.i18n())
                    .itemStyle(PieChartDataModel.Series.DataItem.ItemStyle.builder()
                            .color("#" + Integer.toHexString(FIX_STATE_COLORS.get(fixed.isValue())))
                            .build())
                    .value(count)
                    .build();
            dataModel.getSeries().get(0).getData().add(typeItem);
        }
        return BaseJsonHelper.createObjectMapper().writeValueAsString(dataModel);
    }

    @SneakyThrows
    @SuppressWarnings("unused") // Called by groovy view
    public String getVulnerabilityFalsePositiveStatePie() {
        loadScanResult();
        if (isEmpty()) return null;

        PieChartDataModel dataModel = PieChartDataModel.builder()
                .series(Collections.singletonList(PieChartDataModel.Series.builder().build()))
                .build();
        for (Issue.FalsePositiveState falsePositive : Issue.FalsePositiveState.values()) {
            long count = scanResult.getIssues()
                    .stream()
                    .filter(issue -> falsePositive.isValue() == issue.isFalsePositive())
                    .count();
            // I'd like to show legend items even if there's no corresponding data
            // if (0 == count) continue;
            PieChartDataModel.Series.DataItem typeItem = PieChartDataModel.Series.DataItem.builder()
                    .name(falsePositive.i18n())
                    .itemStyle(PieChartDataModel.Series.DataItem.ItemStyle.builder()
                            .color("#" + Integer.toHexString(FALSE_POSITIVE_COLORS.get(falsePositive.isValue())))
                            .build())
                    .value(count)
                    .build();
            dataModel.getSeries().get(0).getData().add(typeItem);
        }
        return BaseJsonHelper.createObjectMapper().writeValueAsString(dataModel);
    }

    protected List<Action> projectActions;

    @Override
    public Collection<? extends Action> getProjectActions() {
        if (null == projectActions) {
            projectActions = new ArrayList<>();
            projectActions.add(new AstJobMultipleResults(run.getParent()));
            projectActions.add(new AstJobTableResults(run.getParent()));
        }
        return projectActions;
    }
}
