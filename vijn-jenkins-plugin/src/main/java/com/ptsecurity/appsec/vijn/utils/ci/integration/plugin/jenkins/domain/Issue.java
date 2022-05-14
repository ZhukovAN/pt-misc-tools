package com.ptsecurity.appsec.vijn.utils.ci.integration.plugin.jenkins.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import static com.ptsecurity.appsec.vijn.utils.ci.integration.plugin.jenkins.misc.I18nHelper.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Issue {
    protected String url;

    @JsonProperty("name")
    protected String title;

    protected String category;

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public enum Level {
        NONE(0),
        POTENTIAL(1),
        LOW(2),
        MEDIUM(3),
        HIGH(4);

        @Getter
        private final int value;

        public String i18n() {
            return LEVEL_SUPPLIER_MAP.get(this).get();
        }
    }

    @JsonProperty("severity")
    protected Level level;

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public enum FalsePositiveState {
        FP(true),
        TP(false);

        @Getter
        private final boolean value;

        public String i18n() {
            return FALSE_POSITIVE_SUPPLIER_MAP.get(this).get();
        }
    }

    protected boolean falsePositive;

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public enum FixedState {
        YES(true),
        NO(false);

        @Getter
        private final boolean value;

        public String i18n() {
            return FIXED_SUPPLIER_MAP.get(this).get();
        }
    }

    protected boolean fixed;
}
