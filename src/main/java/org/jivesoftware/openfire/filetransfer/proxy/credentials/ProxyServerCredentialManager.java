package org.jivesoftware.openfire.filetransfer.proxy.credentials;

import java.util.Arrays;
import java.util.Calendar;

import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.SystemProperty;
import org.jivesoftware.util.SystemProperty.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProxyServerCredentialManager
{
	private static final Logger Log = LoggerFactory.getLogger(ProxyServerCredentialManager.class);	
	
	@SuppressWarnings("rawtypes")
	public static final SystemProperty<Class> FT_PROXY_CREDENTIAL_PROVIDER;
	
	private static ProxyServerCredentialProvider provider;
	
	public static final String PROPERTY_PROXY_CREDENTIAL_TIMEOUT = "xmpp.proxy.credentials.timeout";
	
	protected static long credExpirationInSeconds;
	
	static
	{
		FT_PROXY_CREDENTIAL_PROVIDER = Builder.ofType(Class.class)
				.setKey("provider.ftproxycredential.className").setBaseClass(ProxyServerCredentialProvider.class)
				.setDefaultValue(DefaultProxyServerCredentialProvider.class).addListener(ProxyServerCredentialManager::initProvider).setDynamic(true)
				.build();
		
	}
	
    private static class ProxyServerCredentialManagerContainer 
    {
        private static ProxyServerCredentialManager instance = new ProxyServerCredentialManager();
    }
    
    private static void initProvider(final Class<?> clazz) 
    {
        if (provider == null || !clazz.equals(provider.getClass())) 
        {
            try 
            {
                provider = (ProxyServerCredentialProvider) clazz.newInstance();
            }
            catch (final Exception e) 
            {
                Log.error("Error loading proxy server credential provider: " + clazz.getName(), e);
                provider = new DefaultProxyServerCredentialProvider();
            }
        }
    }
    
    public static ProxyServerCredentialManager getInstance() 
    {
        return ProxyServerCredentialManagerContainer.instance;
    }
    
    private ProxyServerCredentialManager() 
    {
    	initProvider(FT_PROXY_CREDENTIAL_PROVIDER.getValue());
    	
    	credExpirationInSeconds = Integer.parseInt(JiveGlobals.getProperty( PROPERTY_PROXY_CREDENTIAL_TIMEOUT , "60"));
    }
    
    public ProxyServerCredential createCredential() throws ProxyCredentialException
    {
    	return provider.createCredential();
    }
    
    public ProxyServerCredential getCredential(String subject) throws ProxyCredentialNotFoundException
    {
    	return provider.getCredential(subject);
    }
    
    public boolean validateCredential(String subject, String secret)
    {
    	// Get the credential
    	ProxyServerCredential cred = null;
    	try
    	{
    		cred = getCredential(subject);
    	}
    	catch (ProxyCredentialNotFoundException e)
    	{
    		return false;
    	}
    	
    	// make sure the credential isn't expired
    	long credTimeElaspeseInSeconds = (System.currentTimeMillis() -  cred.getCreationDate().getTime()) / 1000;
    	
    	if (credTimeElaspeseInSeconds > credExpirationInSeconds)
    	{
    		// the credential has expired
    		// delete the credential and return false
    		try
    		{
    			this.deleteCredential(subject);
    			return false;
    		}
    		catch (Exception e)
    		{
    			return false;
    		}
    	}
    	
    	boolean isValid = false;
		try
		{
	    	byte[] secretHash = provider.generateSecretHash(secret);
	    	
	    	isValid = Arrays.equals(secretHash, cred.getSecretHash());
	    	if (isValid)
	    		this.deleteCredential(subject);
		}
		catch (Exception e) {/*no-op*/ }  		
    	
    	return isValid;
    }
    
    public void deleteCredential(String subject)
    {
    	provider.deleteCredential(subject);
    }
    
    public void pruneExpiredCredentials()
    {
    	// get the date x number of seconds back from now
    	// that will be our expiration time
    	
    	final Calendar exipiration = Calendar.getInstance();
    	exipiration.add(Calendar.SECOND, -((int)credExpirationInSeconds));
    	
    	provider.deleteExpiredCredentials(exipiration.getTime());
    }
}
