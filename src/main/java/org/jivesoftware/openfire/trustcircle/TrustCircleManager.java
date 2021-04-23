package org.jivesoftware.openfire.trustcircle;

import java.util.Collection;
import java.util.Collections;

import org.apache.jcs.JCS;
import org.apache.jcs.access.exception.CacheException;
import org.jivesoftware.openfire.certificate.CertCacheFactory;
import org.jivesoftware.openfire.certificate.CertStoreCachePolicy;
import org.jivesoftware.openfire.certificate.CertificateManager.DefaultCertManagerCachePolicy;
import org.jivesoftware.openfire.domain.Domain;
import org.jivesoftware.openfire.domain.DomainManager;
import org.jivesoftware.openfire.trustanchor.TrustAnchor;
import org.jivesoftware.openfire.trustbundle.TrustBundle;
import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.SystemProperty;
import org.jivesoftware.util.SystemProperty.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrustCircleManager
{
	/*
	 * For S2S connections, trust circles are loaded with all anchors and bundles.  To help performance under heavy load,
	 * trust circles will be cached for a lookup of trust circles by domain or for all trust circles.
	 */
	public static final String XMPP_TRUST_CIRCLE_MANAGER_CACHE_MAX_ITEMS = "xmpp.trustcirclemanager.cache.maxitems";
	
	public static final String XMPP_TRUST_CIRCLE_CACHE_TTL = "xmpp.trustcirclemanager.cache.ttl";	
	
	private static final String TRUST_CIRLCE_DOMAIN_CACHE_NAME = "TRUST_CIRLCE_MANAGER_DOMAIN_CACHE";
	
	private static final String ALL_TRUST_CIRLCES_CACHE_NAME = "TRUST_CIRLCE_MANAGER_ALL_CIRCLES_CACHE";
	
	protected static final String DEFAULT_MAX_CAHCE_ITEMS = "200";
	
	protected static final String DEFAULT_CACHE_TTL = "300"; // 5 minutes	
	
	private static final Logger Log = LoggerFactory.getLogger(TrustCircleManager.class);	
	@SuppressWarnings("rawtypes")
	public static final SystemProperty<Class> TRUST_CIRCLE_PROVIDER;
	private static TrustCircleProvider provider;
	
	private static TrustCircleManager INSTANCE;
	
	protected JCS domainTrustCircleCache;
	
	protected JCS allTrustCircleCache;
	
	static
	{
		TRUST_CIRCLE_PROVIDER = Builder.ofType(Class.class)
				.setKey("provider.trustcircle.className").setBaseClass(TrustCircleProvider.class)
				.setDefaultValue(DefaultTrustCircleProvider.class).addListener(TrustCircleManager::initProvider).setDynamic(true)
				.build();
	
	}
	
    private static void initProvider(final Class<?> clazz) 
    {
        if (provider == null || !clazz.equals(provider.getClass())) 
        {
            try 
            {
                provider = (TrustCircleProvider) clazz.newInstance();
            }
            catch (final Exception e) 
            {
                Log.error("Error loading trust bundle provider: " + clazz.getName(), e);
                provider = new DefaultTrustCircleProvider();
            }
        }
    }
    
    public static synchronized TrustCircleManager getInstance() 
    {
        if (INSTANCE == null)
        	INSTANCE = new TrustCircleManager();
        
        return INSTANCE;
    }
    
    private TrustCircleManager() 
    {
    	initProvider(TRUST_CIRCLE_PROVIDER.getValue());
    	
		createCaches();
    }
    
	private void createCaches()
	{
		try
		{
			// create instances
			domainTrustCircleCache = CertCacheFactory.getInstance().getCertCache(TRUST_CIRLCE_DOMAIN_CACHE_NAME, getDefaultCertCachePolicy());	
			allTrustCircleCache = CertCacheFactory.getInstance().getCertCache(ALL_TRUST_CIRLCES_CACHE_NAME, getDefaultCertCachePolicy());	
		}
		///CLOVER:OFF
		catch (CacheException e)
		{
			Log.warn("CertificateManager - Could not create certificate caches", e);
		}
		///CLOVER:ON
	} 
	
	private synchronized JCS getDomainTrustCircleCache()
	{
		if (domainTrustCircleCache == null)
			createCaches();
		
		return domainTrustCircleCache;
	}
	
	private synchronized JCS getAllTrustCirclesCache()
	{
		if (allTrustCircleCache == null)
			createCaches();
		
		return allTrustCircleCache;
	}
    
    public int getTrustCircleCount() 
    {
    	try
    	{
    		return provider.getTrustCircles(false, false).size();
    	}
    	catch (TrustCircleException e)
    	{
    		Log.error("Coulnd not get trust circle count.", e);
    		return 0;
    	}
    }
    
    @SuppressWarnings("unchecked")
	public Collection<TrustCircle> getTrustCircles(boolean loadBundles, boolean loadAnchors)
    {
    	Collection<TrustCircle> retVal = null;
    	final JCS cache = getAllTrustCirclesCache();
    	// only use the cache of requests of loading bundles and loading anchors
    	if (cache != null && loadBundles && loadAnchors)
    	{
    		retVal = (Collection<TrustCircle>)cache.get("ALL");
    	}
    	
    	if (cache == null || (retVal == null || retVal.size() == 0))
    	{
	    	try
			{
				retVal =  provider.getTrustCircles(loadBundles, loadAnchors);
				
	    		if (retVal != null && retVal.size() != 0)
	    		{
	    			try
	    			{
	    				cache.put("ALL", retVal);
	    			}
	    			catch (Exception e)
	    			{
	    				Log.warn("Failed to insert trust circles into all circles cache.", e);
	    			}
	    		}
			} 
	    	catch (TrustCircleException e)
			{
	    		Log.error("Could not get trust circles for all domains.", e);
	    		return Collections.emptyList();
			}
    	}
    	
    	return retVal;
    }
    
    public TrustCircle getTrustCircle(String circleName, boolean loadBundles, boolean loadAnchors)  
    {
    	try
		{
			return provider.getTrustCircle(circleName, loadBundles, loadAnchors);
		} 
    	catch (TrustCircleException e)
		{
    		Log.error("Coulnd not get trust circle.", e);
    		return null;
		}
    }
    
    public int getTrustCircleCountInDomain(String domain) throws TrustCircleException
    {
    	return provider.getCirclesByDomain(domain, false, false).size();
    }
    
    public boolean isRegisteredTrustCircle(String circleName)
    {
		try
		{
			return provider.getTrustCircle(circleName, false, false) != null;
		}
		catch(TrustCircleException e)
		{
			return false;
		}
    }
    
    public TrustCircle addTrustCircle(String circleName, Collection<String> anchorThumbprints) throws TrustCircleException
    {
    	final TrustCircle circle = new TrustCircle();
    	circle.setName(circleName);
    	
    	return provider.addTrustCircle(circleName, anchorThumbprints);
    }
    
    public TrustCircle addTrustBundleToCircle(String circleName, String bundleName) throws TrustCircleException
    {
    	return provider.addTrustBundleToCircle(circleName, bundleName);
    }
    
    public void deleteTrustCircle(String circleName) throws TrustCircleException
    {
    	// Delete the anchors and the bundle from the circle
    	TrustCircle circle = provider.getTrustCircle(circleName, true, true);
    	for (TrustBundle bundle : circle.getTrustBundles())
    	{
    		provider.deleteTrustBundlesFromCircle(circle.getName(), Collections.singletonList(bundle.getBundleName()));
    	}
    	
    	for (TrustAnchor anchor : circle.getAnchors())
    	{
    		provider.deleteAnchorFromCircle(circleName, anchor.getThumbprint());
    	}
    	
    	// remove associations from domains
    	for (Domain domain : DomainManager.getInstance().getDomainsByTrustCircle(circleName))
    	{
    		provider.deleteCirclesFromDomain(domain.getDomainName(), Collections.singletonList(circleName));
    	}
    	
    	provider.deleteCircle(circleName);
    }
    
    public TrustCircle deleteTrustBundlesFromCircle(String circleName, Collection<String> bundleNames) throws TrustCircleException
    {
    	return provider.deleteTrustBundlesFromCircle(circleName, bundleNames);
    }
    
    public TrustCircle addTrustAnchorToCircle(String circleName, String thumbprint) throws TrustCircleException
    {
    	return provider.addAnchorToCircle(circleName, thumbprint);
    }
    
    public TrustCircle deleteTrustAnchorFromCircle(String circleName, String thumbprint) throws TrustCircleException
    {
    	return provider.deleteAnchorFromCircle(circleName, thumbprint);
    }  
    
    @SuppressWarnings("unchecked")
	public Collection<TrustCircle> getCirclesByDomain(String domainName, boolean loadBundles, boolean loadAnchors) throws TrustCircleException
    {
    	Collection<TrustCircle> retVal = null;
    	final JCS cache = getDomainTrustCircleCache();
    	// only use the cache of requests of loading bundles and loading anchors
    	if (cache != null && loadBundles && loadAnchors)
    	{
    		retVal = (Collection<TrustCircle>)cache.get(domainName.toUpperCase());
    	}
    	
    	if (cache == null || (retVal == null || retVal.size() == 0))
    	{
	    	try
			{
				retVal =  provider.getCirclesByDomain(domainName, loadBundles, loadAnchors);
				
	    		if (retVal != null && retVal.size() != 0)
	    		{
	    			try
	    			{
	    				cache.put(domainName.toUpperCase(), retVal);
	    			}
	    			catch (Exception e)
	    			{
	    				Log.warn("Failed to insert trust circles into circles cache for domain {}.", domainName, e);
	    			}
	    		}
			} 
	    	catch (TrustCircleException e)
			{
	    		Log.error("Could not get trust circle for domain {}.", domainName, e);
	    		throw e;
			}
    	}
    	
    	return retVal;    	

    }
    
    public void deleteCirclesFromDomain(String domainName, Collection<String> circleNames) throws TrustCircleException
    {
    	provider.deleteCirclesFromDomain(domainName, circleNames);
    }
    
    public void addCirclesToDomain(String domainName, Collection<String> circleNames) throws TrustCircleException
    {
    	provider.addCirclesToDomain(domainName, circleNames);
    }
    
	private CertStoreCachePolicy getDefaultCertCachePolicy()
	{
		return new DefaultCertManagerCachePolicy();
	}
	
	public static class DefaultTrustCirlceManagerCachePolicy implements CertStoreCachePolicy
	{
		protected final int maxItems;
		protected final int subjectTTL;
		
		public DefaultTrustCirlceManagerCachePolicy()
		{
			final String maxItemsString = JiveGlobals.getProperty(XMPP_TRUST_CIRCLE_MANAGER_CACHE_MAX_ITEMS, DEFAULT_MAX_CAHCE_ITEMS);
			maxItems =  Integer.parseInt(maxItemsString);
			
			final String maxTTLString = JiveGlobals.getProperty(XMPP_TRUST_CIRCLE_CACHE_TTL, DEFAULT_CACHE_TTL);
			subjectTTL =  Integer.parseInt(maxTTLString);
		}
		
		public int getMaxItems() 
		{
			return maxItems;
		}

		public int getSubjectTTL() 
		{
			return subjectTTL;
		}
		
	}	    
}
