package org.jivesoftware.openfire.certificate;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jcs.JCS;
import org.apache.jcs.access.exception.CacheException;
import org.apache.jcs.engine.behavior.ICompositeCacheAttributes;
import org.apache.jcs.engine.behavior.IElementAttributes;

/**
 * Factory class for creating instances of JCS based certificate caches.  Caches are keyed by name (case sensitive).
 * <br>
 * The factory implements a singleton pattern for both the factory itself and named caches.
 * @author Greg Meyer
 */
public class CertCacheFactory 
{
	@SuppressWarnings("deprecation")
	private static final Log LOGGER = LogFactory.getFactory().getInstance(CertCacheFactory.class);
	
	protected static CertCacheFactory INSTANCE;
	
	protected final Map<String, JCS> certCacheMap;
	
	/**
	 * Gets the instance of the cache factory.
	 * @return The cache factory.
	 */
	public static synchronized CertCacheFactory getInstance()
	{
		if (INSTANCE == null)
			INSTANCE = new CertCacheFactory();
		
		return INSTANCE;
	}
	
	/*
	 * private contructor
	 */
	private CertCacheFactory()
	{
		certCacheMap = new HashMap<String, JCS>();
	}
	
	/**
	 * Retrieves a cert cache by name.  Caches are created using a singleton pattern meaning one and only once instance of a cache for a given name
	 * is ever created.
	 * @param cacheName The name of the cache to retrieve.
	 * @param cachePolicy Policy to apply to the cache
	 * @return The certificate cache for the given cache name.
	 * @throws CacheException Thrown if the cache cannot be created.
	 */
	public synchronized JCS getCertCache(String cacheName, CertStoreCachePolicy cachePolicy) throws CacheException
	{
		JCS retVal = certCacheMap.get(cacheName);
		
		if (retVal == null)
		{
			try
			{
				// create instance
				retVal = JCS.getInstance(cacheName);
				if (cachePolicy != null)
					applyCachePolicy(retVal, cachePolicy);
				
				certCacheMap.put(cacheName, retVal);
			}
			catch (CacheException e)
			{
				LOGGER.warn("Failed to create JCS cache " + cacheName, e);
				throw e;
			}
		}
		
		return retVal;
	}
	
	public synchronized void flushAll()
	{
		for (Entry<String, JCS> entry : certCacheMap.entrySet())
		{
			try
			{
				LOGGER.info("Flushing cache " + entry.getKey());
				entry.getValue().clear();
			}
			catch (CacheException e) {/* no-op */}
		}
	}
	
	/*
	 * Apply a policy to the cache
	 */
	private void applyCachePolicy(JCS cache, CertStoreCachePolicy policy) throws CacheException
	{

		ICompositeCacheAttributes attributes = cache.getCacheAttributes();
		attributes.setMaxObjects(policy.getMaxItems());
		attributes.setUseLateral(false);
		attributes.setUseRemote(false);
		cache.setCacheAttributes(attributes);
		
		IElementAttributes eattributes = cache.getDefaultElementAttributes();
		eattributes.setMaxLifeSeconds(policy.getSubjectTTL());
		eattributes.setIsEternal(false);
		eattributes.setIsLateral(false);
		eattributes.setIsRemote(false);		
		
		cache.setDefaultElementAttributes(eattributes);

	}
	
}
