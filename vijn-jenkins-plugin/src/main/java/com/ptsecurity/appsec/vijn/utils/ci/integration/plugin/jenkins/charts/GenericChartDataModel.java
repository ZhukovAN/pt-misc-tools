package com.ptsecurity.appsec.vijn.utils.ci.integration.plugin.jenkins.charts;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ptsecurity.appsec.vijn.utils.ci.integration.plugin.jenkins.actions.AstJobMultipleResults;
import com.ptsecurity.appsec.vijn.utils.ci.integration.plugin.jenkins.domain.Issue;
import com.ptsecurity.appsec.vijn.utils.ci.integration.plugin.jenkins.domain.Result;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

import static com.ptsecurity.appsec.vijn.utils.ci.integration.plugin.jenkins.misc.I18nHelper.LEVEL_SUPPLIER_MAP;

@Slf4j
@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GenericChartDataModel extends BaseJsonChartDataModel {
    @Getter
    @Setter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Legend {
        @NonNull
        @JsonProperty
        @Builder.Default
        public List<String> data = new ArrayList<>();
    }

    @NonNull
    @JsonProperty
    @Builder.Default
    protected Legend legend = Legend.builder().build();

    @Getter
    @Setter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Axis {
        @JsonProperty
        @Builder.Default
        protected List<String> data = new ArrayList<>();
    }

    // ECharts's stacked area chart data model uses "xAxis" and "yAxis"
    // names. But if we simply name field "xAxis" then Lombok's generated
    // getter will be named as getXAxis. During POJOPropertiesCollector.collectAll
    // call that getter will be recognized as matching to xaxis or XAxis field
    // name (it depends on USE_STD_BEAN_NAMING mapper feature) so that field
    // will be serialized twice; as xAxis and xaxis / XAxis. So we need to
    // xplicitly set JSON property name and use neutral field name
    @Builder.Default
    @JsonProperty("xAxis")
    protected List<Axis> xaxis = new ArrayList<>();

    @Builder.Default
    @JsonProperty("yAxis")
    protected List<Axis> yaxis = new ArrayList<>();

    @Getter
    @Setter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Series {
        @JsonProperty
        protected String name;

        @Getter
        @Setter
        @Builder
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class DataItem {
            @JsonProperty
            protected Long value;

            @Getter
            @Setter
            @Builder
            @JsonInclude(JsonInclude.Include.NON_NULL)
            public static class ItemStyle {
                @JsonProperty
                protected String color;
            }

            @JsonProperty
            protected ItemStyle itemStyle;
        }

        @JsonProperty
        @Builder.Default
        protected List<DataItem> data = new ArrayList<>();

        @JsonProperty
        protected DataItem.ItemStyle itemStyle;
    }

    @JsonProperty
    @Builder.Default
    protected List<Series> series = new ArrayList<>();

    public static GenericChartDataModel create(@NonNull final List<AstJobMultipleResults.BuildScanResult> results) {
        log.trace("Prepare X- and Y-axis");
        GenericChartDataModel.Axis xAxis = GenericChartDataModel.Axis.builder().build();
        GenericChartDataModel.Axis yAxis = GenericChartDataModel.Axis.builder().build();
        log.trace("Prepare legend");
        GenericChartDataModel.Legend legend = GenericChartDataModel.Legend.builder().build();
        log.trace("Sort scan results by build number");
        results.sort(Comparator.comparing(AstJobMultipleResults.BuildScanResult::getBuildNumber));
        log.trace("Prepare series to fill with data");
        List<Series> vulnerabilityTypeSeries = new ArrayList<>();
        for (Issue.Level level : Issue.Level.values()) {
            log.trace("Add {} level to legend", level.name());
            legend.data.add(level.i18n());
            log.trace("Create {} level data series", level.name());
            GenericChartDataModel.Series series = Series.builder()
                    // As this is the chart generation method we need to use i18n-ed names
                    // to show chart with user browser language preferences
                    .name(level.i18n())
                    .itemStyle(Series.DataItem.ItemStyle.builder()
                            .color("#" + Integer.toHexString(LEVEL_COLORS.get(level)))
                            .build())
                    .build();
            log.trace("Fill {} level series with issues", level.name());
            for (int i = 0 ; i < results.size() ; i++) {
                long count = 0;
                Result buildResult = results.get(i).getScanResult();
                if (null != buildResult && null != buildResult.getIssues()) {
                    // Count true positive vulnerabilities of a givel level
                    count = buildResult.getIssues().stream()
                            .filter(issue -> level.equals(issue.getLevel()))
                            .filter(issue -> !issue.isFalsePositive())
                            .count();
                }
                log.trace("Added {} true positive vulnerabilities of level {}", count, level.name());
                series.data.add(Series.DataItem.builder().value(count).build());
            }
            vulnerabilityTypeSeries.add(series);
        }

        log.trace("Add build numbers to chart X-axis");
        for (AstJobMultipleResults.BuildScanResult item : results)
            xAxis.data.add(item.getBuildNumber().toString());
        return GenericChartDataModel.builder()
                .legend(legend)
                .xaxis(Collections.singletonList(xAxis))
                .yaxis(Collections.singletonList(yAxis))
                .series(vulnerabilityTypeSeries)
                .build();
    }
}
