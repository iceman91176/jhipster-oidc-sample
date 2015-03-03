package de.witcom.app.domain;

import org.apache.commons.lang.StringUtils;

/**
 * Supported externa providers.  
 */
public enum ExternalAccountProvider {
	DUMMYSSO;

    public static ExternalAccountProvider caseInsensitiveValueOf(String value) {
        if (StringUtils.isNotBlank(value))
            return ExternalAccountProvider.valueOf(value.toUpperCase());
        else
            return null;
    }
}
