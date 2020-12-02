package org.jivesoftware.openfire.trustanchor;


import java.security.cert.X509Certificate;
import java.util.Collection;

import org.jivesoftware.openfire.trustcircle.TrustCircle;
import org.jivesoftware.openfire.trustcircle.TrustCircleManager;
import org.jivesoftware.util.SystemProperty;
import org.jivesoftware.util.SystemProperty.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrustAnchorManager
{
	private static final Logger Log = LoggerFactory.getLogger(TrustAnchorManager.class);	
	@SuppressWarnings("rawtypes")
	public static final SystemProperty<Class> TRUST_ANCHOR_PROVIDER;
	private static TrustAnchorProvider provider;
	
	static
	{
		TRUST_ANCHOR_PROVIDER = Builder.ofType(Class.class)
				.setKey("provider.trustanchor.className").setBaseClass(TrustAnchorProvider.class)
				.setDefaultValue(DefaultTrustAnchorProvider.class).addListener(TrustAnchorManager::initProvider).setDynamic(true)
				.build();
	
	}
	
    private static class TrustBundleManagerContainer 
    {
        private static TrustAnchorManager instance = new TrustAnchorManager();
    }
	
    private static void initProvider(final Class<?> clazz) 
    {
        if (provider == null || !clazz.equals(provider.getClass())) 
        {
            try 
            {
                provider = (TrustAnchorProvider) clazz.newInstance();
            }
            catch (final Exception e) 
            {
                Log.error("Error loading trust bundle provider: " + clazz.getName(), e);
                provider = new DefaultTrustAnchorProvider();
            }
        }
    }
    
    public static TrustAnchorManager getInstance() 
    {
        return TrustBundleManagerContainer.instance;
    }
    
    private TrustAnchorManager() 
    {
    	initProvider(TRUST_ANCHOR_PROVIDER.getValue());
    	
    }
    
	public Collection<TrustAnchor> getAnchors() throws TrustAnchorException
	{
		return provider.getAnchors();
	}
	
	public TrustAnchor getAnchorByThumbprint(String thumbprint) throws TrustAnchorException
	{
		return provider.getAnchorByThumbprint(thumbprint);
	}
	
	public Collection<TrustAnchor> getAnchorsByThumbprints(Collection<String> thumbprints)  throws TrustAnchorException
	{
		return provider.getAnchorsByThumbprints(thumbprints);
	}
	

	public TrustAnchor addTrustAnchor(X509Certificate anchor) throws TrustAnchorException
	{
		return provider.addTrustAnchor(anchor);
	}
	

	public void deleteTrustAnchors(Collection<String> thumbprints) throws TrustAnchorException
	{
		provider.deleteTrustAnchors(thumbprints);
	}
	
	public void deleteTrustAnchor(String thumbprint) throws TrustAnchorException
	{
		// removed the anchor from all circles, not really a fast implementation (could be O^2), 
		// but there should not really be that many trust circles and anchors
		Collection<TrustCircle> circles = TrustCircleManager.getInstance().getTrustCircles(false, true);
		for (TrustCircle circle : circles)
		{
			for (TrustAnchor anchor : circle.getAnchors())
			{
				if (anchor.getThumbprint().toString().equals(thumbprint))
				{
					try
					{
						TrustCircleManager.getInstance().deleteTrustAnchorFromCircle(circle.getName(), thumbprint);
					}
					catch (Exception e)
					{
						throw new TrustAnchorException("Failed to delete anchor bundle " + thumbprint);
					}
						
					break;
				}
			}
		}
		
		
		provider.deleteTrustAnchor(thumbprint);
	}
}
