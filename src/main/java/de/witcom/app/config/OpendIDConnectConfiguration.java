package de.witcom.app.config;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.mitre.jwt.signer.service.impl.JWKSetCacheService;
import org.mitre.oauth2.model.ClientDetailsEntity.AuthMethod;
import org.mitre.oauth2.model.RegisteredClient;
import org.mitre.openid.connect.client.service.impl.PlainAuthRequestUrlBuilder;
import org.mitre.openid.connect.client.service.impl.StaticAuthRequestOptionsService;
import org.mitre.openid.connect.client.service.impl.StaticClientConfigurationService;
import org.mitre.openid.connect.client.service.impl.StaticServerConfigurationService;
import org.mitre.openid.connect.client.service.impl.StaticSingleIssuerService;
import org.mitre.openid.connect.config.ServerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;

import de.witcom.app.domain.ExternalAccountProvider;
import de.witcom.app.security.oidc.OIDCAuthProvider;
import de.witcom.app.security.oidc.OIDCAuthenticationSuccessHandler;
import de.witcom.app.security.oidc.OIDCConfigurer;


@Configuration
public class OpendIDConnectConfiguration implements EnvironmentAware{
	
	private static final String ENV_OPENIDCONNECT = "openidconnect.";
	private static final String PROP_ISSUERURI = "issuer-uri";
	private static final String PROP_ISSUER = "issuer";
	private static final String PROP_AUTHEPURI = "authorizationEndpointUri";
	private static final String PROP_TOKENEPURI = "tokenEndpointUri";
	private static final String PROP_USEREPURI = "userInfoUri";
	private static final String PROP_CLIENTID = "clientId";
	private static final String PROP_CLIENTSECRET = "clientSecret";
	private static final String PROP_REDIRECTURI = "redirectUri";
	
	private final Logger log = LoggerFactory.getLogger(OpendIDConnectConfiguration.class);

    private RelaxedPropertyResolver propertyResolver;

    private Environment env;

    @Override
    public void setEnvironment(Environment env) {
        this.env = env;
        this.propertyResolver = new RelaxedPropertyResolver(env, ENV_OPENIDCONNECT);
    }
   
    @Bean
    public OIDCAuthProvider openIdConnectAuthenticationProvider(){
    	
    	OIDCAuthProvider impl = new OIDCAuthProvider();
    	impl.setProvider(ExternalAccountProvider.DUMMYSSO);
    	return impl;
    }
    
    @Bean
    public OIDCConfigurer oidConfigurer(){
		return new OIDCConfigurer();
   	
    }
    
    @Bean
    public OIDCAuthenticationSuccessHandler mySuccessHandler(){
    	OIDCAuthenticationSuccessHandler impl = new OIDCAuthenticationSuccessHandler();
    	impl.setTargetUrl("/");
    	
		return impl;
    	
    	
    }
        
    @Bean
    public StaticSingleIssuerService staticIssuerService(){
    	log.debug("Configuring staticIssuerService");
    	StaticSingleIssuerService issuerService = new StaticSingleIssuerService();
    	issuerService.setIssuer(propertyResolver.getProperty(PROP_ISSUERURI));
		return issuerService;
    	
    }
    
    @Bean
    public StaticServerConfigurationService staticServerConfigurationService(){
    	log.debug("Configuring staticServerConfigurationService");
    	StaticServerConfigurationService serverConfig = new StaticServerConfigurationService();
    	
    	ServerConfiguration serverCfg = new ServerConfiguration();
    	serverCfg.setIssuer(propertyResolver.getProperty(PROP_ISSUER));
    	serverCfg.setTokenEndpointUri(propertyResolver.getProperty(PROP_TOKENEPURI));
    	serverCfg.setAuthorizationEndpointUri(propertyResolver.getProperty(PROP_AUTHEPURI));
    	serverCfg.setUserInfoUri(propertyResolver.getProperty(PROP_USEREPURI));
    	Map<String, ServerConfiguration> servers = new HashMap<String, ServerConfiguration>();
		servers.put(propertyResolver.getProperty(PROP_ISSUERURI), serverCfg);
    	
		serverConfig.setServers(servers);
    	
    	return serverConfig;
    }
    
    @Bean
    public StaticClientConfigurationService staticClientConfigurationService(){
    	log.debug("Configuring staticClientConfigurationService");
    	
    	StaticClientConfigurationService clientConfig = new StaticClientConfigurationService();
    	Map<String, RegisteredClient> clients = new HashMap<String,RegisteredClient>();
    	
    	RegisteredClient clientCfg = new RegisteredClient();
    	clientCfg.setClientId(propertyResolver.getProperty(PROP_CLIENTID));
    	clientCfg.setClientSecret(propertyResolver.getProperty(PROP_CLIENTSECRET));
    	clientCfg.setTokenEndpointAuthMethod(AuthMethod.SECRET_BASIC);

    	Set<String> scopes = new HashSet<String>();
    	scopes.add("openid");
		clientCfg.setScope(scopes);
		
		Set<String> redirectUris = new HashSet<String>();
		redirectUris.add(propertyResolver.getProperty(PROP_REDIRECTURI));
		clientCfg.setRedirectUris(redirectUris);
    	
		clients.put(propertyResolver.getProperty(PROP_ISSUER), clientCfg);
    	clientConfig.setClients(clients);
    	
    	return clientConfig;
    	
    }
    
    @Bean
    public StaticAuthRequestOptionsService staticAuthRequestOptionsService(){
    	log.debug("Configuring StaticAuthRequestOptionsService");
		return new StaticAuthRequestOptionsService();
    }
    
    @Bean
    public PlainAuthRequestUrlBuilder plainAuthRequestUrlBuilder(){
    	log.debug("Configuring PlainAuthRequestUrlBuilder");
    	return new PlainAuthRequestUrlBuilder();
    	
    }
    
    @Bean
    public JWKSetCacheService validatorCache(){
    	return new JWKSetCacheService();
    	
    }

 

}
