package com.ptsecurity.appsec.vijn.utils.ci.integration.plugin.jenkins.misc;

import com.ptsecurity.appsec.vijn.utils.ci.integration.Resources;
import com.ptsecurity.appsec.vijn.utils.ci.integration.plugin.jenkins.domain.Issue;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Need to map enum values like {@link com.ptsecurity.appsec.vijn.utils.ci.integration.plugin.jenkins.domain.Issue.Level}
 * to localized string names. As those strings are to be localized, there's no way to map enum value to string value: these
 * values are to be calculated in runtime. So we need to map enum values to {@link java.util.function.Supplier} lambda
 */
public class I18nHelper {
    public static final Map<Issue.Level, Supplier<String>> LEVEL_SUPPLIER_MAP = new HashMap<>();
    public static final Map<Issue.FixedState, Supplier<String>> FIXED_SUPPLIER_MAP = new HashMap<>();
    public static final Map<Issue.FalsePositiveState, Supplier<String>> FALSE_POSITIVE_SUPPLIER_MAP = new HashMap<>();

    static {
        LEVEL_SUPPLIER_MAP.put(Issue.Level.HIGH, Resources::i18n_misc_enums_vulnerability_severity_high);
        LEVEL_SUPPLIER_MAP.put(Issue.Level.MEDIUM, Resources::i18n_misc_enums_vulnerability_severity_medium);
        LEVEL_SUPPLIER_MAP.put(Issue.Level.LOW, Resources::i18n_misc_enums_vulnerability_severity_low);
        LEVEL_SUPPLIER_MAP.put(Issue.Level.POTENTIAL, Resources::i18n_misc_enums_vulnerability_severity_potential);
        LEVEL_SUPPLIER_MAP.put(Issue.Level.NONE, Resources::i18n_misc_enums_vulnerability_severity_none);

        FIXED_SUPPLIER_MAP.put(Issue.FixedState.YES, Resources::i18n_misc_enums_vulnerability_fixed_true);
        FIXED_SUPPLIER_MAP.put(Issue.FixedState.NO, Resources::i18n_misc_enums_vulnerability_fixed_false);

        FALSE_POSITIVE_SUPPLIER_MAP.put(Issue.FalsePositiveState.FP, Resources::i18n_misc_enums_vulnerability_falsepositive_true);
        FALSE_POSITIVE_SUPPLIER_MAP.put(Issue.FalsePositiveState.TP, Resources::i18n_misc_enums_vulnerability_falsepositive_false);
    }
}
