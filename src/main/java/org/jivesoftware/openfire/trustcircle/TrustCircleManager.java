package org.jivesoftware.openfire.trustcircle;

import java.util.Collection;
import java.util.Collections;

import org.jivesoftware.openfire.domain.Domain;
import org.jivesoftware.openfire.domain.DomainManager;
import org.jivesoftware.openfire.trustanchor.TrustAnchor;
import org.jivesoftware.openfire.trustbundle.TrustBundle;
import org.jivesoftware.openfire.trustbundle.TrustBundleManager;
import org.jivesoftware.util.SystemProperty;
import org.jivesoftware.util.SystemProperty.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrustCircleManager
{
	private static final Logger Log = LoggerFactory.getLogger(TrustCircleManager.class);	
	@SuppressWarnings("rawtypes")
	public static final SystemProperty<Class> TRUST_CIRCLE_PROVIDER;
	private static TrustCircleProvider provider;
	
	private static TrustCircleManager INSTANCE;
	
	static
	{
		TRUST_CIRCLE_PROVIDER = Builder.ofType(Class.class)
				.setKey("provider.trustcircle.className").setBaseClass(TrustBundleManager.class)
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
    
    public Collection<TrustCircle> getTrustCircles(boolean loadBundles, boolean loadAnchors)
    {
    	try
		{
			return provider.getTrustCircles(loadBundles, loadAnchors);
		} 
    	catch (TrustCircleException e)
		{
    		Log.error("Coulnd not get trust circles.", e);
    		return Collections.emptyList();
		}
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
    
    public Collection<TrustCircle> getCirclesByDomain(String domainName, boolean loadBundles, boolean loadAnchors) throws TrustCircleException
    {
    	return provider.getCirclesByDomain(domainName, loadBundles, loadAnchors);
    }
    
    public void deleteCirclesFromDomain(String domainName, Collection<String> circleNames) throws TrustCircleException
    {
    	provider.deleteCirclesFromDomain(domainName, circleNames);
    }
    
    public void addCirclesToDomain(String domainName, Collection<String> circleNames) throws TrustCircleException
    {
    	provider.addCirclesToDomain(domainName, circleNames);
    }
}
