package org.jivesoftware.openfire.trustbundle;

import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;

import org.jivesoftware.openfire.trustbundle.processor.BundleRefreshProcessor;
import org.jivesoftware.openfire.trustbundle.processor.DefaultBundleRefreshProcessor;
import org.jivesoftware.openfire.trustcircle.TrustCircle;
import org.jivesoftware.openfire.trustcircle.TrustCircleManager;
import org.jivesoftware.util.SystemProperty;
import org.jivesoftware.util.SystemProperty.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrustBundleManager
{
	private static final Logger Log = LoggerFactory.getLogger(TrustBundleManager.class);	
	@SuppressWarnings("rawtypes")
	public static final SystemProperty<Class> TRUST_BUNDLE_PROVIDER;
	
	@SuppressWarnings("rawtypes")
	public static final SystemProperty<Class> TRUST_BUNDLE_REFRESH_PROVIDER;
	
	private static TrustBundleProvider provider;
	
	private static BundleRefreshProcessor refreshProvider;
	
	static
	{
		TRUST_BUNDLE_PROVIDER = Builder.ofType(Class.class)
				.setKey("provider.trustbundle.className").setBaseClass(TrustBundleProvider.class)
				.setDefaultValue(DefaultTrustBundleProvider.class).addListener(TrustBundleManager::initProvider).setDynamic(true)
				.build();
		
		TRUST_BUNDLE_REFRESH_PROVIDER = Builder.ofType(Class.class)
				.setKey("provider.trustbundlerefresh.className").setBaseClass(TrustBundleManager.class)
				.setDefaultValue(DefaultBundleRefreshProcessor.class).addListener(TrustBundleManager::initRefreshProvider).setDynamic(true)
				.build();
	
	}
	
    private static class TrustBundleManagerContainer 
    {
        private static TrustBundleManager instance = new TrustBundleManager();
    }
	
    private static void initProvider(final Class<?> clazz) 
    {
        if (provider == null || !clazz.equals(provider.getClass())) 
        {
            try 
            {
                provider = (TrustBundleProvider) clazz.newInstance();
            }
            catch (final Exception e) 
            {
                Log.error("Error loading trust bundle provider: " + clazz.getName(), e);
                provider = new DefaultTrustBundleProvider();
            }
        }     
    }
    
    private static void initRefreshProvider(final Class<?> clazz) 
    {
        if (refreshProvider == null || !clazz.equals(refreshProvider.getClass())) 
        {
            try 
            {
            	refreshProvider = (BundleRefreshProcessor) clazz.newInstance();
            }
            catch (final Exception e) 
            {
                Log.error("Error loading trust bundle refresh provider: " + clazz.getName(), e);
                refreshProvider = new DefaultBundleRefreshProcessor();
            }
        }     
    }
    
    public static TrustBundleManager getInstance() 
    {
        return TrustBundleManagerContainer.instance;
    }
    
    private TrustBundleManager() 
    {
    	initProvider(TRUST_BUNDLE_PROVIDER.getValue());
    	initRefreshProvider(TRUST_BUNDLE_REFRESH_PROVIDER.getValue());
    }
    
	public Collection<TrustBundle> getTrustBundles(boolean loadAnchors) throws TrustBundleException
	{
		return provider.getTrustBundles(loadAnchors);
	}
	
	public TrustBundle getTrustBundle(String bundleName) throws TrustBundleException
	{
		return provider.getTrustBundle(bundleName);
	}
	
    public boolean isRegisteredTrustBundle(String bundleName)
    {
		try
		{
			return provider.getTrustBundle(bundleName) != null;
		}
		catch(TrustBundleException e)
		{
			return false;
		}
    }
	
	public TrustBundle addTrustBundle(TrustBundle bundle) throws TrustBundleException
	{
		
		final TrustBundle newBundle = provider.addTrustBundle(bundle);
		
		refreshBundle(newBundle);
		
		return newBundle;
	}
	
	public TrustBundle updateTrustBundleAttributes(String bundleName, TrustBundle updatedBundle, boolean refreshBundle) throws TrustBundleException
	{
		final TrustBundle bundle = provider.updateTrustBundleAttributes(bundleName, updatedBundle);
		
		if (refreshBundle)
			refreshBundle(bundle);
		
		return bundle;
	}
	
	public void refreshBundle(TrustBundle bundle)
	{
		refreshProvider.refreshBundle(bundle);
	}
	
	public TrustBundle updateSigningCertificate(String bundleName, byte[] signingCert) throws TrustBundleException
	{
		return provider.updateSigningCertificate(bundleName, signingCert);
	}
	
	public void deleteTrustBundle(String bundleName) throws TrustBundleException
	{
		// removed the bundle from all circles, not really a fast implementation (could be O^2), 
		// but there should not really be that many trust circles and bundles
		Collection<TrustCircle> circles = TrustCircleManager.getInstance().getTrustCircles(true, false);
		for (TrustCircle circle : circles)
		{
			for (TrustBundle bundle : circle.getTrustBundles())
			{
				if (bundle.getBundleName().equals(bundleName))
				{
					try
					{
						TrustCircleManager.getInstance().deleteTrustBundlesFromCircle(circle.getName(), Collections.singletonList(bundle.getBundleName()));
					}
					catch (Exception e)
					{
						throw new TrustBundleException("Failed to delete trust bundle " + bundleName);
					}
						
					break;
				}
			}
		}
		
		provider.deleteTrustBundle(bundleName);
	}
	
	public void deleteAnchorsByBundleId(String bundleId) throws TrustBundleException
	{
		provider.deleteAnchorsByBundleId(bundleId);
	}
	
	public TrustBundleAnchor addTrustBundleAnchor(X509Certificate anchor, String trustBundleId) throws TrustBundleException
	{
		return provider.addTrustBundleAnchor(anchor, trustBundleId);
	}
}
