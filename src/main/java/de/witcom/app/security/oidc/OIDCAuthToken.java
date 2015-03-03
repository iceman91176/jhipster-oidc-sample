package de.witcom.app.security.oidc;

import java.util.ArrayList;
import java.util.Collection;

import org.mitre.openid.connect.config.ServerConfiguration;
import org.mitre.openid.connect.model.UserInfo;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

public class OIDCAuthToken extends AbstractAuthenticationToken {
	private static final long serialVersionUID = 22100073066377804L;

	private final ImmutableMap<String, String> principal;
	private final String idTokenValue; // string representation of the id token
	private final String accessTokenValue; // string representation of the access token
	private final String refreshTokenValue; // string representation of the refresh token
	private final String issuer; // issuer URL (parsed from the id token)
	private final String sub; // user id (parsed from the id token)

	private final transient ServerConfiguration serverConfiguration; // server configuration used to fulfill this token, don't serialize it
	private final UserInfo userInfo; // user info container, don't serialize it b/c it might be huge and can be re-fetched

	/**
	 * Constructs OIDCAuthenticationToken with a full set of authorities, marking this as authenticated.
	 * 
	 * Set to authenticated.
	 * 
	 * Constructs a Principal out of the subject and issuer.
	 * @param subject
	 * @param authorities
	 * @param principal
	 * @param idToken
	 */
	public OIDCAuthToken(String subject, String issuer,
			UserInfo userInfo, Collection<? extends GrantedAuthority> authorities,
			String idTokenValue, String accessTokenValue, String refreshTokenValue) {

		super(authorities);
		String preferredUsername = subject;
		
		if (!Strings.isNullOrEmpty(userInfo.getPreferredUsername())){
			preferredUsername = userInfo.getPreferredUsername();
		}
		this.principal = ImmutableMap.of("sub", subject, "iss", issuer,"preferredUsername",preferredUsername);
		this.userInfo = userInfo;
		this.sub = subject;
		this.issuer = issuer;
		this.idTokenValue = idTokenValue;
		this.accessTokenValue = accessTokenValue;
		this.refreshTokenValue = refreshTokenValue;

		this.serverConfiguration = null; // we don't need a server config anymore

		setAuthenticated(true);
	}

	/**
	 * Constructs OIDCAuthenticationToken for use as a data shuttle from the filter to the auth provider.
	 * 
	 * Set to not-authenticated.
	 * 
	 * Constructs a Principal out of the subject and issuer.
	 * @param sub
	 * @param idToken
	 */
	public OIDCAuthToken(String subject, String issuer,
			ServerConfiguration serverConfiguration,
			String idTokenValue, String accessTokenValue, String refreshTokenValue) {

		super(new ArrayList<GrantedAuthority>(0));
		
		String preferredUsername = subject;
		
		this.principal = ImmutableMap.of("sub", subject, "iss", issuer,"preferredUsername",preferredUsername);

		//this.principal = ImmutableMap.of("sub", subject, "iss", issuer);
		this.sub = subject;
		this.issuer = issuer;
		this.idTokenValue = idTokenValue;
		this.accessTokenValue = accessTokenValue;
		this.refreshTokenValue = refreshTokenValue;

		this.userInfo = null; // we don't have a UserInfo yet

		this.serverConfiguration = serverConfiguration;


		setAuthenticated(false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.security.core.Authentication#getCredentials()
	 */
	@Override
	public Object getCredentials() {
		return accessTokenValue;
	}

	/**
	 * Get the principal of this object, an immutable map of the subject and issuer.
	 */
	@Override
	public Object getPrincipal() {
		return principal;
	}

	public String getSub() {
		return sub;
	}

	/**
	 * @return the idTokenValue
	 */
	public String getIdTokenValue() {
		return idTokenValue;
	}

	/**
	 * @return the accessTokenValue
	 */
	public String getAccessTokenValue() {
		return accessTokenValue;
	}

	/**
	 * @return the refreshTokenValue
	 */
	public String getRefreshTokenValue() {
		return refreshTokenValue;
	}

	/**
	 * @return the serverConfiguration
	 */
	public ServerConfiguration getServerConfiguration() {
		return serverConfiguration;
	}

	/**
	 * @return the issuer
	 */
	public String getIssuer() {
		return issuer;
	}

	/**
	 * @return the userInfo
	 */
	public UserInfo getUserInfo() {
		return userInfo;
	}


}
