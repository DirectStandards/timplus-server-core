package org.jivesoftware.openfire.trustcircle;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.openfire.trustanchor.DefaultTrustAnchorProvider;
import org.jivesoftware.openfire.trustanchor.TrustAnchor;
import org.jivesoftware.openfire.trustbundle.DefaultTrustBundleProvider;
import org.jivesoftware.openfire.trustbundle.TrustBundle;

public class DefaultTrustCircleProvider implements TrustCircleProvider
{
	private static final String LOAD_CIRCLES = "SELECT * FROM ofTrustCircle";	
	
	private static final String LOAD_CIRCLE = "SELECT * FROM ofTrustCircle where UPPER(circleName) = ?";	
	
	private static final String LOAD_CIRCLE_BY_ID = "SELECT * FROM ofTrustCircle where id = ?";	
	
	private static final String LOAD_CIRCLE_ANCHORS = "SELECT trustAnchorId FROM ofTrustCircleAnchorReltn where trustCircleId = ?";	
	
	private static final String LOAD_CIRCLE_BUNDLES = "SELECT trustBundleId FROM ofTrustCircleBundleReltn where trustCircleId = ?";	
	
    private static final String INSERT_TRUST_CIRCLE =
            "INSERT INTO ofTrustCircle (id,circleName,createTime) " +
            "VALUES (?,?,?)"; 
	
    private static final String INSERT_TRUST_CIRCLE_BUNDLE_ASSOC =
            "INSERT INTO ofTrustCircleBundleReltn (trustCircleId,trustBundleId) " +
            "VALUES (?,?)"; 
    
	private static final String DELETE_TRUST_CIRCLE_BUNDLE_ASSOC = "DELETE FROM ofTrustCircleBundleReltn where trustCircleId = ? and trustBundleId = ?";	
    
    private static final String INSERT_TRUST_CIRCLE_ANCHOR_ASSOC =
            "INSERT INTO ofTrustCircleAnchorReltn (trustCircleId,trustAnchorId) " +
            "VALUES (?,?)"; 
	
	private static final String DELETE_TRUST_CIRCLE_ANCHOR_ASSOC = "DELETE FROM ofTrustCircleAnchorReltn where trustCircleId = ? and trustAnchorId = ?";
    
	private static final String DELETE_TRUST_CIRCLE = "DELETE from ofTrustCircle WHERE UPPER(circleName) = ?";
	
	private static final String LOAD_DOMAIN_CIRCLES = "SELECT trustCircleId FROM ofTrustCircleDomainReltn where UPPER(domainName) = ?";
	
	private static final String DELETE_TRUST_CIRCLE_DOMAIN_ASSOC = "DELETE FROM ofTrustCircleDomainReltn where trustCircleId = ? and domainName = ?";	
	
    private static final String INSERT_TRUST_CIRCLE_DOMAIN_ASSOC =
            "INSERT INTO ofTrustCircleDomainReltn (trustCircleId,domainName) " +
            "VALUES (?,?)"; 
	
	public DefaultTrustCircleProvider()
	{
		
	}

	@Override
	public Collection<TrustCircle> getTrustCircles(boolean loadBundles, boolean loadAnchors) throws TrustCircleException
	{
		   final List<TrustCircle> trustCircles = new ArrayList<>();
		   Connection con = null;
		   PreparedStatement pstmt = null;
		   ResultSet rs = null;
		   try 
		   {
			   con = DbConnectionManager.getConnection();

	           pstmt = con.prepareStatement(LOAD_CIRCLES);
	            // Set the fetch size. This will prevent some JDBC drivers from trying
	            // to load the entire result set into memory.
	            DbConnectionManager.setFetchSize(pstmt, 500);
	            rs = pstmt.executeQuery();
	            while (rs.next()) 
	            {
	            	final TrustCircle circle = trustCircleFromResultSet(rs);
	            	trustCircles.add(circle);
	            	
	            	if (loadAnchors)
	            	{
	            		circle.setAnchors(this.getTrustCircleAnchors(circle.getId()));
	            	}
	            	
	            	if (loadBundles)
	            	{
	            		circle.setTrustBundles(this.getTrustCircleBundles(circle.getId()));
	            	}	            	
	            }
		   }
	       catch (SQLException e) 
		   {
	    	   throw new TrustCircleException("Failed to load trust circles.", e);
	       }
		   finally 
		   {
			   DbConnectionManager.closeConnection(rs, pstmt, con);
		   }
		   
		   return trustCircles;
	}

	@Override
	public TrustCircle getTrustCircle(String circleName, boolean loadBundles, boolean loadAnchors) throws TrustCircleException
	{
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try 
        {
            con = DbConnectionManager.getConnection();
            pstmt = con.prepareStatement(LOAD_CIRCLE);
            pstmt.setString(1, circleName.toUpperCase());
            rs = pstmt.executeQuery();
            if (!rs.next()) 
            {
                throw new TrustCircleException();
            }

        	final TrustCircle circle = trustCircleFromResultSet(rs);
        	
        	if (loadAnchors)
        	{
        		circle.setAnchors(this.getTrustCircleAnchors(circle.getId()));
        	}
            
        	if (loadBundles)
        	{
        		circle.setTrustBundles(this.getTrustCircleBundles(circle.getId()));
        	}	
        	
            return circle;
        }
        catch (Exception e) 
        {
            throw new TrustCircleNotFoundException(e);
        }
        finally 
        {
            DbConnectionManager.closeConnection(rs, pstmt, con);
        }
	}

	@Override
	public TrustCircle addTrustCircle(String circleName, Collection<String> thumbprints) throws TrustCircleException
	{
		if (this.isExistingCircle(circleName))
			throw new TrustCircleAlreadyExistsException("Trust circle already exists.");

		final TrustCircle trustCircle = new TrustCircle();
		trustCircle.setId(UUID.randomUUID().toString());
		trustCircle.setName(circleName);
		trustCircle.setCreationDate(Instant.now());
		
        Connection con = null;
        PreparedStatement pstmt = null;
        try 
        {
            con = DbConnectionManager.getConnection();

            
            pstmt = con.prepareStatement(INSERT_TRUST_CIRCLE);
            pstmt.setString(1, trustCircle.getId());
            pstmt.setString(2, trustCircle.getName());
            pstmt.setLong(3, trustCircle.getCreationDate().toEpochMilli());
            pstmt.execute();
            
            if (thumbprints != null && thumbprints.size() > 0)
            	for (String thumbprint : thumbprints)
            		addAnchorToCircle(circleName, thumbprint);
        }
        catch (Exception e) 
        {
            throw new TrustCircleException("Failed to insert circle.");
        }
        finally 
        {
            DbConnectionManager.closeConnection(pstmt, con);
        }
        
        return trustCircle;	
	}

	@Override
	public TrustCircle addAnchorToCircle(String circleName, String thumbprint) throws TrustCircleException
	{
		final TrustCircle circle = getTrustCircle(circleName, false, false);
		final DefaultTrustAnchorProvider provider = new DefaultTrustAnchorProvider();
		TrustAnchor anchor = null;
		try
		{
			anchor = provider.getAnchorByThumbprint(thumbprint);
		}
		catch (Exception e)
		{
			throw new TrustCircleException("Trust anchor does not exist.", e);
		}
		
        Connection con = null;
        PreparedStatement pstmt = null;
        try 
        {
            con = DbConnectionManager.getConnection();

            
            pstmt = con.prepareStatement(INSERT_TRUST_CIRCLE_ANCHOR_ASSOC);
            pstmt.setString(1, circle.getId());
            pstmt.setString(2, anchor.getId());

            pstmt.execute();
       
        }
        catch (Exception e) 
        {
            throw new TrustCircleException("Failed to add anchor with thumbprint " + thumbprint + " to circle " + circleName);
        }
        finally 
        {
            DbConnectionManager.closeConnection(pstmt, con);
        }
        
        return getTrustCircle(circleName, false, false);	
	}

	@Override
	public TrustCircle deleteAnchorFromCircle(String circleName, String thumbprint)
			throws TrustCircleException
	{
		final TrustCircle circle = getTrustCircle(circleName, false, false);
		
		final DefaultTrustAnchorProvider provider = new DefaultTrustAnchorProvider();
		TrustAnchor anchor = null;

		try
		{
			anchor = provider.getAnchorByThumbprint(thumbprint);
		}
		catch (Exception e)
		{
			/* no-op */
		}

		if (anchor != null)
		{
	        Connection con = null;
	        PreparedStatement pstmt = null;
	        boolean abortTransaction = false;
	        try 
	        {	        	
	        	con = DbConnectionManager.getTransactionConnection();
	            pstmt = con.prepareStatement(DELETE_TRUST_CIRCLE_ANCHOR_ASSOC);
	            pstmt.setString(1, circle.getId());
	            pstmt.setString(2, anchor.getId());
	            pstmt.execute();
	        }
	        catch (Exception e) 
	        {
	            abortTransaction = true;
	            throw new TrustCircleException("Failed to remove anchor with thumbprint id " + thumbprint + " from circle " + circleName);
	        }
	        finally 
	        {
	            DbConnectionManager.closeStatement(pstmt);
	            DbConnectionManager.closeTransactionConnection(pstmt, con, abortTransaction);
	        }
		}
		
		return getTrustCircle(circleName, false, false);	
	}

	@Override
	public TrustCircle addTrustBundleToCircle(String circleName, String bundleName) throws TrustCircleException
	{
		final TrustCircle circle = getTrustCircle(circleName, false, false);
		final DefaultTrustBundleProvider provider = new DefaultTrustBundleProvider();
		TrustBundle bundle = null;
		try
		{
			bundle = provider.getTrustBundle(bundleName);
		}
		catch (Exception e)
		{
			throw new TrustCircleException("Trust bundle does not exist.", e);
		}
		
        Connection con = null;
        PreparedStatement pstmt = null;
        try 
        {
            con = DbConnectionManager.getConnection();

            
            pstmt = con.prepareStatement(INSERT_TRUST_CIRCLE_BUNDLE_ASSOC);
            pstmt.setString(1, circle.getId());
            pstmt.setString(2, bundle.getId());

            pstmt.execute();
       
        }
        catch (Exception e) 
        {
            throw new TrustCircleException("Failed to add bundle " + bundleName + " to circle " + circleName);
        }
        finally 
        {
            DbConnectionManager.closeConnection(pstmt, con);
        }
        
        return getTrustCircle(circleName, false, false);	
	}

	@Override
	public TrustCircle deleteTrustBundlesFromCircle(String circleName, Collection<String> bundleNames)
			throws TrustCircleException
	{
		final TrustCircle circle = getTrustCircle(circleName, false, false);
		final Collection<String> bundleIds = new ArrayList<>();
		
		final DefaultTrustBundleProvider provider = new DefaultTrustBundleProvider();
		TrustBundle bundle = null;
		for (String bundleName : bundleNames)
		{
			try
			{
				bundle = provider.getTrustBundle(bundleName);
				bundleIds.add(bundle.getId());
			}
			catch (Exception e)
			{
				/* no-op */
			}
		}
		
		for (String bundleId : bundleIds)
		{
	        Connection con = null;
	        PreparedStatement pstmt = null;
	        boolean abortTransaction = false;
	        try 
	        {	        	
	        	con = DbConnectionManager.getTransactionConnection();
	            pstmt = con.prepareStatement(DELETE_TRUST_CIRCLE_BUNDLE_ASSOC);
	            pstmt.setString(1, circle.getId());
	            pstmt.setString(2, bundleId);
	            pstmt.execute();
	        }
	        catch (Exception e) 
	        {
	            abortTransaction = true;
	            throw new TrustCircleException("Failed to remove bundle id " + bundleId + " from circle " + circleName);
	        }
	        finally 
	        {
	            DbConnectionManager.closeStatement(pstmt);
	            DbConnectionManager.closeTransactionConnection(pstmt, con, abortTransaction);
	        }
		}
		
		return getTrustCircle(circleName, false, false);	
	}

	@Override
	public void deleteCircle(String circleName) throws TrustCircleException
	{		
        Connection con = null;
        PreparedStatement pstmt = null;
        boolean abortTransaction = false;
        try 
        {	
        	con = DbConnectionManager.getTransactionConnection();
            pstmt = con.prepareStatement(DELETE_TRUST_CIRCLE);
            pstmt.setString(1, circleName);
            pstmt.execute();
        }
        catch (Exception e) 
        {
            abortTransaction = true;
            throw new TrustCircleException("Failed to delete circle from store.", e);
        }
        finally 
        {
            DbConnectionManager.closeStatement(pstmt);
            DbConnectionManager.closeTransactionConnection(pstmt, con, abortTransaction);
        }
	}

	@Override
	public Collection<TrustCircle> getCirclesByDomain(String domainName, boolean loadBundles, boolean loadAnchors)
			throws TrustCircleException
	{
		   final List<TrustCircle> trustCircles = new ArrayList<>();
		   Connection con = null;
		   PreparedStatement pstmt = null;
		   ResultSet rs = null;
		   try 
		   {
			   con = DbConnectionManager.getConnection();

	           pstmt = con.prepareStatement(LOAD_DOMAIN_CIRCLES);
	            // Set the fetch size. This will prevent some JDBC drivers from trying
	            // to load the entire result set into memory.
	           pstmt.setString(1, domainName.toUpperCase());
	          
	           
	           DbConnectionManager.setFetchSize(pstmt, 500);
	           rs = pstmt.executeQuery();
	           while (rs.next()) 
	           {
	            	final TrustCircle circle = this.getTrustCircleById(rs.getString(1), loadBundles, loadAnchors);
	            	trustCircles.add(circle);
	  
	           }
		   }
	       catch (SQLException e) 
		   {
	    	   throw new TrustCircleException("Failed to load trust circles.", e);
	       }
		   finally 
		   {
			   DbConnectionManager.closeConnection(rs, pstmt, con);
		   }
		   
		   return trustCircles;
	}

	@Override
	public void addCirclesToDomain(String domainName, Collection<String> circleNames) throws TrustCircleException
	{
		for (String circleName : circleNames)
		{
			final TrustCircle circle = this.getTrustCircle(circleName, false, false);
			
	        Connection con = null;
	        PreparedStatement pstmt = null;
	        boolean abortTransaction = false;
	        try 
	        {	
	        	con = DbConnectionManager.getTransactionConnection();
	            pstmt = con.prepareStatement(INSERT_TRUST_CIRCLE_DOMAIN_ASSOC);
	            pstmt.setString(1, circle.getId());
	            pstmt.setString(2, domainName);
	            pstmt.execute();
	        }
	        catch (Exception e) 
	        {
	            abortTransaction = true;
	            throw new TrustCircleException("Failed to add circle " + circleName + " to domain " + domainName, e);
	        }
	        finally 
	        {
	            DbConnectionManager.closeStatement(pstmt);
	            DbConnectionManager.closeTransactionConnection(pstmt, con, abortTransaction);
	        }
		}
	}

	@Override
	public void addDomainsToCircle(String circleName, Collection<String> domainNames) throws TrustCircleException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void deleteCirclesFromDomain(String domainName, Collection<String> circleNames) throws TrustCircleException
	{
		for (String circleName : circleNames)
		{
			final TrustCircle circle = this.getTrustCircle(circleName, false, false);
			
	        Connection con = null;
	        PreparedStatement pstmt = null;
	        boolean abortTransaction = false;
	        try 
	        {	
	        	con = DbConnectionManager.getTransactionConnection();
	            pstmt = con.prepareStatement(DELETE_TRUST_CIRCLE_DOMAIN_ASSOC);
	            pstmt.setString(1, circle.getId());
	            pstmt.setString(2, domainName);
	            pstmt.execute();
	        }
	        catch (Exception e) 
	        {
	            abortTransaction = true;
	            throw new TrustCircleException("Failed to delete circle " + circleName + " from domain " + domainName, e);
	        }
	        finally 
	        {
	            DbConnectionManager.closeStatement(pstmt);
	            DbConnectionManager.closeTransactionConnection(pstmt, con, abortTransaction);
	        }
		}
		
	}

	@Override
	public void deleteDomainsFromCircle(String circleName, Collection<String> domainNames) throws TrustCircleException
	{
		// TODO Auto-generated method stub
		
	}

	protected TrustCircle getTrustCircleById(String circleid, boolean loadBundles, boolean loadAnchors) throws TrustCircleException
	{
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try 
        {
            con = DbConnectionManager.getConnection();
            pstmt = con.prepareStatement(LOAD_CIRCLE_BY_ID);
            pstmt.setString(1, circleid);
            rs = pstmt.executeQuery();
            if (!rs.next()) 
            {
                throw new TrustCircleException();
            }

        	final TrustCircle circle = trustCircleFromResultSet(rs);
        	
        	if (loadAnchors)
        	{
        		circle.setAnchors(this.getTrustCircleAnchors(circle.getId()));
        	}
            
        	if (loadBundles)
        	{
        		circle.setTrustBundles(this.getTrustCircleBundles(circle.getId()));
        	}	
        	
            return circle;
        }
        catch (Exception e) 
        {
            throw new TrustCircleNotFoundException(e);
        }
        finally 
        {
            DbConnectionManager.closeConnection(rs, pstmt, con);
        }
	}	
	
	protected Collection<TrustAnchor> getTrustCircleAnchors(String circleId) throws TrustCircleException
	{
		   Connection con = null;
		   PreparedStatement pstmt = null;
		   ResultSet rs = null;
		   try 
		   {
			   con = DbConnectionManager.getConnection();

	           pstmt = con.prepareStatement(LOAD_CIRCLE_ANCHORS);
	           pstmt.setString(1, circleId);
	            // Set the fetch size. This will prevent some JDBC drivers from trying
	            // to load the entire result set into memory.
	            DbConnectionManager.setFetchSize(pstmt, 500);
	            rs = pstmt.executeQuery();
	            final Collection<String> anchorIds = new ArrayList<>();
	            while (rs.next()) 
	            {
	            	anchorIds.add(rs.getString(1));
	            }
	            
	            if (anchorIds.isEmpty())
	            	return Collections.emptyList();
	            else
	            {
	            	final DefaultTrustAnchorProvider prov = new DefaultTrustAnchorProvider();
	            	return prov.getAnchorsByIds(anchorIds);
	            }
		   }
	       catch (Exception e) 
		   {
	    	   throw new TrustCircleException("Failed to load anchors for trust circle", e);
	       }
		   finally 
		   {
			   DbConnectionManager.closeConnection(rs, pstmt, con);
		   }
		   
	}
	
	protected Collection<TrustBundle> getTrustCircleBundles(String circleId) throws TrustCircleException
	{
		   Connection con = null;
		   PreparedStatement pstmt = null;
		   ResultSet rs = null;
		   try 
		   {
			   con = DbConnectionManager.getConnection();

	           pstmt = con.prepareStatement(LOAD_CIRCLE_BUNDLES);
	           pstmt.setString(1, circleId);
	            // Set the fetch size. This will prevent some JDBC drivers from trying
	            // to load the entire result set into memory.
	            DbConnectionManager.setFetchSize(pstmt, 500);
	            rs = pstmt.executeQuery();
	            final Collection<String> bundleIds = new ArrayList<>();
	            while (rs.next()) 
	            {
	            	bundleIds.add(rs.getString(1));
	            }
	            
	            if (bundleIds.isEmpty())
	            	return Collections.emptyList();
	            else
	            {
	            	final DefaultTrustBundleProvider prov = new DefaultTrustBundleProvider();
	            	return prov.getTrustBundlesById(bundleIds, true);
	            }
		   }
	       catch (Exception e) 
		   {
	    	   throw new TrustCircleException("Failed to load anchors for trust circle", e);
	       }
		   finally 
		   {
			   DbConnectionManager.closeConnection(rs, pstmt, con);
		   }
		   
	}
	
	protected boolean isExistingCircle(String circleName)
	{
		try
		{
			return this.getTrustCircle(circleName, false, false) != null;
		}
		catch(TrustCircleException e)
		{
			return false;
		}
	}
	
	protected TrustCircle trustCircleFromResultSet(final ResultSet rs) throws SQLException
	{
		final TrustCircle circle = new TrustCircle();

		circle.setId(rs.getString(1));
		circle.setName(rs.getString(2));		
		circle.setCreationDate(Instant.ofEpochMilli(rs.getLong(3)));

		return circle;		
	}
}
