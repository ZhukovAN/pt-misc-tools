package com.ptsecurity.appsec.vijn.utils.ci.integration.plugin.jenkins.utils;

import com.ptsecurity.appsec.vijn.utils.ci.integration.Resources;
import hudson.util.FormValidation;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

@Slf4j
public class Validator {
    public static boolean doCheckFieldNotEmpty(String value) {
        return !StringUtils.isEmpty(value);
    }

    protected static boolean checkViaException(@NonNull final Runnable call) {
        try { call.run(); return true; } catch (Exception e) { return false; }
    }

    public static boolean doCheckFieldInteger(Integer value) {
        return (null != value);
    }

    public static boolean doCheckFieldRegEx(String value) {
        return checkViaException(() -> Pattern.compile(value));
    }

    public static FormValidation doCheckFieldNotEmpty(String value, String errorMessage) {
        return doCheckFieldNotEmpty(value) ? FormValidation.ok() : FormValidation.error(errorMessage);
    }

    public static FormValidation doCheckFieldInteger(Integer value, String errorMessage) {
        return doCheckFieldInteger(value) ? FormValidation.ok() : FormValidation.error(errorMessage);
    }

    public static FormValidation doCheckFieldRegEx(String value, String errorMessage) {
        return doCheckFieldRegEx(value) ? FormValidation.ok() : FormValidation.error(errorMessage);
    }

    public static FormValidation error(Exception e) {
        String caption = e.getMessage();
        if (StringUtils.isEmpty(caption))
            return FormValidation.error(e, Resources.i18n_vijn_settings_test_message_failed());
        else {
            Throwable cause = e;
            return FormValidation.error(cause, Resources.i18n_vijn_settings_test_message_failed_details(caption));
        }
    }

    public static FormValidation error(@NonNull final String message, Exception e) {
        // log.log(Level.FINEST, "FormValidation error", e);
        Throwable cause = e;
        return FormValidation.error(cause, Resources.i18n_vijn_settings_test_message_failed_details(message));
    }

    public static FormValidation error(String message) {
        return FormValidation.error(message);
    }
}
