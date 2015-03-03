package de.witcom.app.security.oidc;

import java.util.ArrayList;
import java.util.Collection;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.mitre.openid.connect.client.NamedAdminAuthoritiesMapper;
import org.mitre.openid.connect.client.SubjectIssuerGrantedAuthority;
import org.mitre.openid.connect.model.OIDCAuthenticationToken;
import org.mitre.openid.connect.model.UserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import de.witcom.app.domain.Authority;
import de.witcom.app.domain.ExternalAccount;
import de.witcom.app.domain.ExternalAccountProvider;
import de.witcom.app.domain.User;
import de.witcom.app.repository.AuthorityRepository;
import de.witcom.app.repository.UserRepository;
import de.witcom.app.security.SecurityUtils;
import de.witcom.app.security.UserDetailsService;
import de.witcom.app.security.UserNotActivatedException;
import de.witcom.app.service.UserService;
import de.witcom.app.web.rest.dto.UserDTO;


public class OIDCAuthProvider implements AuthenticationProvider {

	private Logger logger = LoggerFactory.getLogger(getClass());
	
	private OIDCUserInfoFetcher userInfoFetcher = new OIDCUserInfoFetcher();

	private GrantedAuthoritiesMapper authoritiesMapper = new NamedAdminAuthoritiesMapper();
	
	private ExternalAccountProvider provider;
	
	@Inject
    private UserRepository userRepository;
	
	@Inject
	private AuthorityRepository authorityRepository;

    @Inject
    private UserService userService;
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.security.authentication.AuthenticationProvider#
	 * authenticate(org.springframework.security.core.Authentication)
	 */
	@Override
	@Transactional
	public Authentication authenticate(final Authentication authentication)
			throws AuthenticationException {

		if (!supports(authentication.getClass())) {
			return null;
		}

		if (authentication instanceof OIDCAuthToken) {
			logger.debug("Starte authentifizierung");
			
			User loginUser = null;
			UserDetails userDetails = null;

			OIDCAuthToken token = (OIDCAuthToken) authentication;

			Collection<SubjectIssuerGrantedAuthority> authorities = Lists.newArrayList(new SubjectIssuerGrantedAuthority(token.getSub(), token.getIssuer()));
			
			UserInfo userInfo = userInfoFetcher.loadUserInfo(token);

			
			if (userInfo == null) {
				// TODO: user Info not found -- error?
			} else {
				if (!Strings.isNullOrEmpty(userInfo.getSub()) && !userInfo.getSub().equals(token.getSub())) {
					// the userinfo came back and the user_id fields don't match what was in the id_token
					throw new UsernameNotFoundException("user_id mismatch between id_token and user_info call: " + token.getSub() + " / " + userInfo.getSub());
				}
				
				logger.debug("User aus Datenbank holen, bzw. anlegen");
				userInfo.setSub(token.getSub());
				try {
					logger.debug("Try to load User from database");
					userDetails = loadUserByOIDCId(token.getSub(), provider);
					logger.debug("Loaded {} from database",userDetails.getUsername());
				} catch (UsernameNotFoundException ex){
					logger.debug("User {} not found, creating new one");
					try {
						this.registerExternalAccount(userInfo);
						userDetails = loadUserByOIDCId(token.getSub(), provider);
						
					} catch (Exception e) {
						logger.error("Fehler {}",e);
						return null;
					}
				} catch (Exception e) {
					logger.error("Fehler {}",e);
					return null;
					
				}
				
			}
			
			return new OIDCAuthToken(token.getSub(),
					token.getIssuer(),
					userInfo, userDetails.getAuthorities(),
					token.getIdTokenValue(), token.getAccessTokenValue(), token.getRefreshTokenValue());
		}

		return null;
	}
	
	
	private Collection<GrantedAuthority> getGrantedAuthorities(User user) {
		logger.debug("Loading authorities");
        Collection<GrantedAuthority> grantedAuthorities = new ArrayList<>();
        
        for (Authority authority : user.getAuthorities()) {
        	logger.debug("Found Authority {}",authority.getName());
            GrantedAuthority grantedAuthority = new SimpleGrantedAuthority(authority.getName());
            grantedAuthorities.add(grantedAuthority);
        }
        return grantedAuthorities;
	}
	
    public UserDetails loadUserByOIDCId(final String login,final ExternalAccountProvider provider){
    	
    	logger.debug("Authenticating {} from Provider {}",login,provider);
		logger.debug("Repository {}",userRepository);
		if (userRepository == null){
			throw new UsernameNotFoundException("UserRepository is NULL");
		}
        String lowercaseLogin = login.toLowerCase();
        User userFromDatabase = userRepository.getUserByExternalAccount(provider, lowercaseLogin);
        
        if (userFromDatabase == null) {
            throw new UsernameNotFoundException("User " + lowercaseLogin + " was not found in the database");
        } else if (!userFromDatabase.getActivated()) {
            throw new UserNotActivatedException("User " + lowercaseLogin + " was not activated");
        }
        
        logger.debug("Got user {}",userFromDatabase.getLogin());
        
        Collection<GrantedAuthority> grantedAuthorities = getGrantedAuthorities(userFromDatabase);
        logger.debug("Login successful");
        return new org.springframework.security.core.userdetails.User(userFromDatabase.getLogin(), "n/a", grantedAuthorities);
    }
	
	//@Transactional(readOnly = true)
	private User registerExternalAccount(UserInfo userInfo) throws Exception{
		
		UserDTO externalAuthDTO = oidcAuthAsUserDTO(userInfo);

        // check that there isn't already another account linked to the current external account
        ExternalAccount externalAccount = externalAuthDTO.getExternalAccounts().iterator().next();
        User existingUser = userRepository.getUserByExternalAccount(externalAccount.getExternalProvider(), externalAccount.getExternalId());
        if (existingUser != null)
           throw new Exception("User already registered with different external account");

        logger.debug("Start creating user....");
        User user = userService.createUserInformation(
        		userInfo.getPreferredUsername(),
            externalAuthDTO.getFirstName(), externalAuthDTO.getLastName(),
            externalAuthDTO.getEmail().toLowerCase(),
            "en",
            externalAccount
        );
        logger.debug("End creating user.... {}",user);

		return user;
		
	}
	
	private UserDTO oidcAuthAsUserDTO(UserInfo userInfo) throws Exception{
		
		
		 // build a new UserDTO from the external provider's version of the User
        //UserProfile profile = connection.fetchUserProfile();
        String firstName = userInfo.getGivenName();
        String lastName = userInfo.getFamilyName();
        String email = userInfo.getEmail();
        
        // build the ExternalAccount from the ConnectionKey
        
        
        ExternalAccount externalAccount = new ExternalAccount(this.provider, userInfo.getSub());

        // check that we got the information we needed
        if (StringUtils.isBlank(firstName) || StringUtils.isBlank(lastName) || StringUtils.isBlank(email))
        	throw new Exception(this.provider.name() + " provider failed to return required attributes");

        UserDTO userDTO = new UserDTO(firstName, lastName, email, externalAccount);

       
        logger.debug("Retrieved details from {} for user '{}'", this.provider, userInfo);

		
		return userDTO;
		
		
	}

	/**
	 * @param authoritiesMapper
	 */
	public void setAuthoritiesMapper(GrantedAuthoritiesMapper authoritiesMapper) {
		this.authoritiesMapper = authoritiesMapper;
	}
	
	public void setUserInfoFetcher(OIDCUserInfoFetcher userInfoFetcher) {
		this.userInfoFetcher = userInfoFetcher;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.security.authentication.AuthenticationProvider#supports
	 * (java.lang.Class)
	 */
	@Override
	public boolean supports(Class<?> authentication) {
		return OIDCAuthToken.class.isAssignableFrom(authentication);
	}

	
	public ExternalAccountProvider getProvider() {
		return provider;
	}

	public void setProvider(ExternalAccountProvider providerName) {
		this.provider = providerName;
	}
}

