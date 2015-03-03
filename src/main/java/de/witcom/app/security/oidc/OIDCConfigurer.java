package de.witcom.app.security.oidc;

import javax.inject.Inject;

import org.mitre.openid.connect.client.service.impl.PlainAuthRequestUrlBuilder;
import org.mitre.openid.connect.client.service.impl.StaticAuthRequestOptionsService;
import org.mitre.openid.connect.client.service.impl.StaticClientConfigurationService;
import org.mitre.openid.connect.client.service.impl.StaticServerConfigurationService;
import org.mitre.openid.connect.client.service.impl.StaticSingleIssuerService;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.SecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

import de.witcom.app.config.OpendIDConnectConfiguration;
import de.witcom.app.domain.ExternalAccountProvider;

public class OIDCConfigurer extends SecurityConfigurerAdapter<DefaultSecurityFilterChain, HttpSecurity>{
	
	
	private StaticSingleIssuerService staticIssuerService;
	private StaticAuthRequestOptionsService staticAuthRequestOptionsService;
	private PlainAuthRequestUrlBuilder plainAuthRequestUrlBuilder;
	private StaticClientConfigurationService staticClientConfigurationService;
	private StaticServerConfigurationService staticServerConfigurationService;
	
	private ExternalAccountProvider externalProvider=null;
	private String postLoginUrl;
	private String postFailureUrl;
	private boolean alwaysUsePostLoginUrl = false;
	
	@Inject
	private AuthenticationProvider openIdConnectAuthenticationProvider;

    @Inject
    private OpendIDConnectConfiguration oidConfig;

	
	public OIDCConfigurer(){
		
		
	}
	
	@Override
	public void configure(HttpSecurity http) throws Exception {
		
		OIDCAuthFilter filter = new OIDCAuthFilter();
		filter.setAuthenticationManager(http.getSharedObject(AuthenticationManager.class));
		filter.setAuthenticationSuccessHandler(oidConfig.mySuccessHandler());
		filter.setIssuerService(oidConfig.staticIssuerService());
		filter.setAuthRequestUrlBuilder(oidConfig.plainAuthRequestUrlBuilder());
		filter.setClientConfigurationService(oidConfig.staticClientConfigurationService());
		filter.setServerConfigurationService(oidConfig.staticServerConfigurationService());
		filter.setAuthRequestOptionsService(oidConfig.staticAuthRequestOptionsService());
		
		http.authenticationProvider(
				openIdConnectAuthenticationProvider)
				.addFilterBefore(postProcess(filter), AbstractPreAuthenticatedProcessingFilter.class);

	}
	
	private <T> T getDependency(ApplicationContext applicationContext, Class<T> dependencyType) {
		try {
			T dependency = applicationContext.getBean(dependencyType);
			return dependency;
		} catch (NoSuchBeanDefinitionException e) {
			throw new IllegalStateException("OIDCConfigurer depends on " + dependencyType.getName() +". No single bean of that type found in application context.", e);
		}
	}

	public StaticSingleIssuerService getStaticIssuerService() {
		return staticIssuerService;
	}

	public void setStaticIssuerService(StaticSingleIssuerService staticIssuerService) {
		this.staticIssuerService = staticIssuerService;
	}

	public StaticAuthRequestOptionsService getStaticAuthRequestOptionsService() {
		return staticAuthRequestOptionsService;
	}

	public void setStaticAuthRequestOptionsService(
			StaticAuthRequestOptionsService staticAuthRequestOptionsService) {
		this.staticAuthRequestOptionsService = staticAuthRequestOptionsService;
	}

	public PlainAuthRequestUrlBuilder getPlainAuthRequestUrlBuilder() {
		return plainAuthRequestUrlBuilder;
	}

	public void setPlainAuthRequestUrlBuilder(
			PlainAuthRequestUrlBuilder plainAuthRequestUrlBuilder) {
		this.plainAuthRequestUrlBuilder = plainAuthRequestUrlBuilder;
	}

	public StaticClientConfigurationService getStaticClientConfigurationService() {
		return staticClientConfigurationService;
	}

	public void setStaticClientConfigurationService(
			StaticClientConfigurationService staticClientConfigurationService) {
		this.staticClientConfigurationService = staticClientConfigurationService;
	}

	public StaticServerConfigurationService getStaticServerConfigurationService() {
		return staticServerConfigurationService;
	}

	public void setStaticServerConfigurationService(
			StaticServerConfigurationService staticServerConfigurationService) {
		this.staticServerConfigurationService = staticServerConfigurationService;
	}

	public void setExternalProvider(ExternalAccountProvider externalProvider) {
		this.externalProvider = externalProvider;
	}
	

}
