package org.jivesoftware.openfire.domain;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.muc.MultiUserChatManager;
import org.jivesoftware.util.AlreadyExistsException;
import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.NotFoundException;
import org.jivesoftware.util.SystemProperty;
import org.jivesoftware.util.SystemProperty.Builder;
import org.jivesoftware.util.cache.Cache;
import org.jivesoftware.util.cache.CacheFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DomainManager
{
	private static final Logger Log = LoggerFactory.getLogger(DomainManager.class);	
	@SuppressWarnings("rawtypes")
	public static final SystemProperty<Class> DOMAIN_PROVIDER;
	private static DomainProvider provider;
	
	private static final String PROXY_SERVICE_NAME;
	
    private final Cache<String, Domain> domainCache;
    private final Cache<String, Boolean> remoteDomainCache;
    private final XMPPServer xmppServer;
	
	static
	{
		DOMAIN_PROVIDER = Builder.ofType(Class.class)
				.setKey("provider.domain.className").setBaseClass(DomainProvider.class)
				.setDefaultValue(DefaultDomainProvider.class).addListener(DomainManager::initProvider).setDynamic(true)
				.build();
		
		PROXY_SERVICE_NAME = JiveGlobals.getProperty("xmpp.proxy.service", "ftproxystream");
	}
	
    private static class DomainManagerContainer 
    {
        private static DomainManager instance = new DomainManager();
    }
	
    private static void initProvider(final Class<?> clazz) 
    {
        if (provider == null || !clazz.equals(provider.getClass())) 
        {
            try 
            {
                provider = (DomainProvider) clazz.newInstance();
            }
            catch (final Exception e) 
            {
                Log.error("Error loading domain provider: " + clazz.getName(), e);
                provider = new DefaultDomainProvider();
            }
        }
    }
    
    public static DomainManager getInstance() 
    {
        return DomainManagerContainer.instance;
    }
    
    private DomainManager() 
    {
    	this(XMPPServer.getInstance());
    	
    }
    
	DomainManager(final XMPPServer xmppServer) 
    {
		this.xmppServer = xmppServer;
		
        initProvider(DOMAIN_PROVIDER.getValue());
        
        this.domainCache = CacheFactory.createCache("Domain");
        this.remoteDomainCache = CacheFactory.createCache("Remote Users Existence");
        
        final DomainEventListener domainListener = new DomainEventListener() 
        {
            @Override
            public void domainCreated(final Domain domain, final Map<String, Object> params) 
            {
                domainCache.put(domain.getDomainName(), domain);
            }

            @Override
            public void domainDeleted(final Domain domain, final Map<String, Object> params) 
            {
            	domainCache.remove(domain.getDomainName());
            }

            @Override
            public void domainModified(final Domain domain, final Map<String, Object> params) 
            {
            	domainCache.put(domain.getDomainName(), domain);
            }
        };
        
        DomainEventDispatcher.addListener(domainListener);
    }
    
    public Domain createDomain(String domainName, boolean enabled) throws DomainAlreadyExistsException
    {
    	if (StringUtils.isEmpty(domainName))
    		throw new IllegalArgumentException("Domain name cannot be null or empty.");

    	domainName = domainName.toLowerCase();
    	
        final Domain domain = provider.createDomain(domainName, enabled);
        domainCache.put(domainName, domain);

        // Fire event.
        final Map<String,Object> params = Collections.emptyMap();
        DomainEventDispatcher.dispatchEvent(domain, DomainEventDispatcher.EventType.domain_created, params);

        // Each domain has certain services associated with it such as MUC and file transfers.  These should be created at the time a domain is created
        createDomainServices(domainName);

        return domain;
    }
    
    public boolean isRegisteredDomain(String domainName)
    {
    	if (StringUtils.isEmpty(domainName))
    		throw new IllegalArgumentException("Domain name cannot be null or empty");    	
    	
    	domainName = domainName.toLowerCase();
    	
    	// check the domain cache first
    	if (domainCache.containsKey(domainName))
    		return true;
    	
    	// check the remote domain cache next
    	if (remoteDomainCache.containsKey(domainName))
    		return false;
    	
    	return provider.isRegisteredDomain(domainName);
    }
    
    public boolean isRegisteredComponentDomain(String domainName)
    {
    	
    	if (StringUtils.isEmpty(domainName))
    		throw new IllegalArgumentException("Domain name cannot be null or empty");  
    	
    	String topDomain;
    	
    	if (domainName.startsWith(MultiUserChatManager.DEFAULT_MUC_SERVICE))
    		topDomain = domainName.substring(MultiUserChatManager.DEFAULT_MUC_SERVICE.length() + 1);
    	else if (domainName.startsWith(PROXY_SERVICE_NAME))
    		topDomain = domainName.substring(PROXY_SERVICE_NAME.length() + 1);
    	else 
    		return false;
    	
    	return isRegisteredDomain(topDomain);
    }
    
    public void deleteDomain(String domainName) throws DomainNotFoundException
    {
    	if (StringUtils.isEmpty(domainName))
    		throw new IllegalArgumentException("Domain name cannot be null or empty");    	
    	
    	domainName = domainName.toLowerCase();
    	
    	final Domain domain = provider.getDomain(domainName);
    	
        provider.deleteDomain(domainName);

        // Fire event.
        final Map<String,Object> params = Collections.emptyMap();
        DomainEventDispatcher.dispatchEvent(domain, DomainEventDispatcher.EventType.domain_deleted, params);

        // Remove the user from cache.
        domainCache.remove(domainName); 
        
        // Removed any service associated with the domain
        deleteDomainServices(domainName);

    }
    
    public Domain getDomain(String domainName) throws DomainNotFoundException
    {
    	if (StringUtils.isEmpty(domainName))
    		throw new IllegalArgumentException("Domain name cannot be null or empty");
    	
    	domainName = domainName.toLowerCase();
    	
    	Domain domain = domainCache.get(domainName);
        if (domain == null) 
        {
        	domain = provider.getDomain(domainName);
        	domainCache.put(domainName, domain);
        }
        
        return domain;    	
    }
    
    public Collection<Domain> getDomains(boolean enabledOnly)
    {
    	return provider.getDomains(enabledOnly);
    }
    
    public Collection<String> getDomainNames(boolean enabledOnly)
    {
    	return provider.getDomainNames(enabledOnly);
    }
    
    public Collection<Domain> findDomains(String searchStr, boolean enabledOnly)
    {
    	if (StringUtils.isEmpty(searchStr))
    		throw new IllegalArgumentException("Search string cannot be null or empty");    	
    	
    	searchStr = searchStr.toLowerCase();
    	
    	return provider.findDomains(searchStr, enabledOnly);
    }
    
    public void enableDomain(String domainName, boolean enable) throws DomainNotFoundException
    {
    	if (StringUtils.isEmpty(domainName))
    		throw new IllegalArgumentException("Search string cannot be null or empty"); 
    	
    	domainName = domainName.toLowerCase();
    	
    	provider.enableDomain(domainName, enable);
    	
    	final Domain domain = provider.getDomain(domainName);
    	domainCache.put(domainName, domain);
    	
        final Map<String,Object> params = Collections.emptyMap();
        DomainEventDispatcher.dispatchEvent(domain, DomainEventDispatcher.EventType.domain_modified, params);
        
        // Enabling or disabling a domain has an affect on the services that are managed within the that domain
        if (enable) 
        	createDomainServices(domainName);
        else
        	deleteDomainServices(domainName);
        
    }
    
    public int getDomainCount() 
    {
        return provider.getDomainCount();
    }
    
    protected void createDomainServices(String domainName)
    {
        // First lets create the MUC service
        final MultiUserChatManager chatManager = xmppServer.getMultiUserChatManager();

    	try
    	{
    		chatManager.createMultiUserChatService(MultiUserChatManager.DEFAULT_MUC_SERVICE, domainName, "", false);
    	}
    	catch (AlreadyExistsException e)
    	{
    		Log.warn("Tried to create new MUC service " + MultiUserChatManager.toFQDN(MultiUserChatManager.DEFAULT_MUC_SERVICE, domainName) + 
    				" but it already existed");
    	}    
    }
    
    protected void deleteDomainServices(String domainName)
    {
        // First delete the MUC service
        final MultiUserChatManager chatManager = xmppServer.getMultiUserChatManager();
        
        try
        {
        	chatManager.removeMultiUserChatService(MultiUserChatManager.DEFAULT_MUC_SERVICE, domainName);
        }
        catch (NotFoundException e)
        {
    		Log.warn("Tried to remove MUC service " + MultiUserChatManager.toFQDN(MultiUserChatManager.DEFAULT_MUC_SERVICE, domainName) + 
    				" but it didn't exist");
        }
    }
}
