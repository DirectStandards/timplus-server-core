package org.jivesoftware.openfire.trustcircle;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.jivesoftware.openfire.trustanchor.TrustAnchor;
import org.jivesoftware.openfire.trustbundle.TrustBundle;



public class TrustCircle
{
	private String id;
	
	private String name;
	
	private Collection<TrustAnchor> anchors;
	
	private Collection<TrustBundle> bundles;
	
	private Instant creationDate;
	
	/**
	 * Empty constructor
	 */
	public TrustCircle()
	{
		
	}
	
	
	
	public String getId()
	{
		return id;
	}



	public void setId(String id)
	{
		this.id = id;
	}



	/**
	 * Sets the trust circle name.
	 * @param name The trust circle name.
	 */
	public void setName(String name)
	{
		this.name = name;
	}
	
	/**
	 * Gets the trust circle name.
	 * @return
	 */
	public String getName()
	{
		return name;
	}
	
	/**
	 * Sets the anchors in the trust circles.
	 * @param anchors The anchors in the trust circle.
	 */
	public void setAnchors(Collection<TrustAnchor> anchors)
	{
		this.anchors = new ArrayList<TrustAnchor>(anchors);
	}
	
	/**
	 * Gets the anchors in the trust circle.
	 * @return The anchors in the trust circle.
	 */
	public Collection<TrustAnchor> getAnchors()
	{
		if (anchors == null)
			anchors = Collections.emptyList();
		
		return Collections.unmodifiableCollection(anchors);
	}
	
	/**
	 * Sets the bundles in the trust circles.
	 * @param bundles The trust bundles in the trust circle.
	 */
	public void setTrustBundles(Collection<TrustBundle> bundles)
	{
		this.bundles = new ArrayList<TrustBundle>(bundles);
	}
	
	/**
	 * Gets the trust bundles in the trust circle.
	 * @return The trust bundles in the trust circle.
	 */
	public Collection<TrustBundle> getTrustBundles()
	{
		if (bundles == null)
			bundles = Collections.emptyList();
		
		return Collections.unmodifiableCollection(bundles);
	}

	public Instant getCreationDate()
	{
		return creationDate;
	}

	public void setCreationDate(Instant creationDate)
	{
		this.creationDate = creationDate;
	}
	
	
}
