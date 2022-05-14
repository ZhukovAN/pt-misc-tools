package com.ptsecurity.appsec.vijn.utils.ci.integration.plugin.jenkins.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Result {
    protected String url;

    @JsonProperty("vulns")
    protected List<Issue> issues;

    protected String sharedLink;
}
