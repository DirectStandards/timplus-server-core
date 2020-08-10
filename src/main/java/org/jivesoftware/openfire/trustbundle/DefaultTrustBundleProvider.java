package org.jivesoftware.openfire.trustbundle;

import java.security.cert.X509Certificate;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.util.cert.CertUtils;

public class DefaultTrustBundleProvider implements TrustBundleProvider
{
	private static final String LOAD_BUNDLES = "SELECT * FROM ofTrustBundle";	
	
	private static final String LOAD_BUNDLE = "SELECT * FROM ofTrustBundle where UPPER(bundleName) = ?";	
	
	private static final String LOAD_BUNDLES_BY_IDS = "SELECT * FROM ofTrustBundle where id IN ";	
	
	private static final String LOAD_ANCHORS_BY_BUNDLE_ID = "SELECT * FROM ofTrustBundleAnchor where trustBundleId = ?";
	
    private static final String INSERT_TRUST_BUNDLE =
            "INSERT INTO ofTrustBundle (id,bundleName,bundleURL,checkSum,lastRefreshAttempt,lastSuccessfulRefresh,lastRefreshError,refreshInterval,signingCertificateData,createTime) " +
            "VALUES (?,?,?,?,?,?,?,?,?,?)"; 
	
    private static final String INSERT_TRUST_BUNDLE_ANCHOR =
            "INSERT INTO ofTrustBundleAnchor (id,distinguishedName,serialNumber,thumbprint,validStartDate,validEndDate,anchorData,trustBundleId) " +
            "VALUES (?,?,?,?,?,?,?,?)"; 
    
    private static final String UPDATE_TRUST_BUNDLE =
            "UPDATE ofTrustBundle set bundleName = ?, bundleURL = ?, checkSum = ?, lastRefreshAttempt = ?, lastSuccessfulRefresh = ?,lastRefreshError = ?,"
            + "refreshInterval = ?, signingCertificateData = ? " +
            " where UPPER(bundleName) = ?"; 
    
    private static final String UPDATE_SIGNING_CERTIFICATE =
            "UPDATE ofTrustBundle set signingCertificateData = ? where UPPER(bundleName) = ?"; 
    
    private static final String DELETE_BUNDLE = "DELETE from ofTrustBundle WHERE UPPER(bundleName) = ?";
    
    private static final String DELETE_ANCHORS_BY_BUNLDE_ID = "DELETE from ofTrustBundleAnchor  WHERE trustBundleId = ?";
    
	public DefaultTrustBundleProvider()
	{
		
	}

	@Override
	public Collection<TrustBundle> getTrustBundles(boolean loadAnchors) throws TrustBundleException
	{
	   final List<TrustBundle> trustBundles = new ArrayList<>();
	   Connection con = null;
	   PreparedStatement pstmt = null;
	   ResultSet rs = null;
	   try 
	   {
		   con = DbConnectionManager.getConnection();

           pstmt = con.prepareStatement(LOAD_BUNDLES);
            // Set the fetch size. This will prevent some JDBC drivers from trying
            // to load the entire result set into memory.
            DbConnectionManager.setFetchSize(pstmt, 500);
            rs = pstmt.executeQuery();
            while (rs.next()) 
            {
            	final TrustBundle bundle = trustBundleFromResultSet(rs);
            	trustBundles.add(bundle);
            	
            	if (loadAnchors)
            	{
            		bundle.setTrustBundleAnchors(loadAnchorsByBundleId(bundle.getId()));
            	}
            }
	   }
       catch (SQLException e) 
	   {
    	   throw new TrustBundleException("Failed to load anchors.", e);
       }
	   finally 
	   {
		   DbConnectionManager.closeConnection(rs, pstmt, con);
	   }
	   
	   return trustBundles;
	}

	public Collection<TrustBundle> getTrustBundlesById(Collection<String> ids, boolean loadAnchors) throws TrustBundleException
	{
        if (ids == null || ids.size() == 0)
        	return Collections.emptyList();
	
        final StringBuilder idsStr = new StringBuilder("(");
        for (String tp : ids) 
        {
            if (idsStr.length() > 1) 
            {
            	idsStr.append(", ");
            }
            idsStr.append("'").append(tp).append("'");
        }
        idsStr.append(")");
	
	   final List<TrustBundle> trustBundles = new ArrayList<>();
	   Connection con = null;
	   PreparedStatement pstmt = null;
	   ResultSet rs = null;
	   try 
	   {
		   final String query = LOAD_BUNDLES_BY_IDS + idsStr.toString();
		   con = DbConnectionManager.getConnection();

           pstmt = con.prepareStatement(query);
            // Set the fetch size. This will prevent some JDBC drivers from trying
            // to load the entire result set into memory.
            DbConnectionManager.setFetchSize(pstmt, 500);
            rs = pstmt.executeQuery();
            while (rs.next()) 
            {
            	final TrustBundle bundle = trustBundleFromResultSet(rs);
            	trustBundles.add(bundle);
            	
            	if (loadAnchors)
            	{
            		bundle.setTrustBundleAnchors(loadAnchorsByBundleId(bundle.getId()));
            	}
            }
	   }
       catch (SQLException e) 
	   {
    	   throw new TrustBundleException("Failed to load anchors.", e);
       }
	   finally 
	   {
		   DbConnectionManager.closeConnection(rs, pstmt, con);
	   }
	   
	   return trustBundles;
	}
	
	@Override
	public TrustBundle getTrustBundle(String bundleName) throws TrustBundleException
	{
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try 
        {
            con = DbConnectionManager.getConnection();
            pstmt = con.prepareStatement(LOAD_BUNDLE);
            pstmt.setString(1, bundleName.toUpperCase());
            rs = pstmt.executeQuery();
            if (!rs.next()) 
            {
                throw new TrustBundleException();
            }

            final TrustBundle bundle = trustBundleFromResultSet(rs);
            bundle.setTrustBundleAnchors(loadAnchorsByBundleId(bundle.getId()));
            
            return bundle;
        }
        catch (Exception e) 
        {
            throw new TrustBundleNotFoundException(e);
        }
        finally 
        {
            DbConnectionManager.closeConnection(rs, pstmt, con);
        }
	}

	@Override
	public TrustBundle addTrustBundle(TrustBundle bundle) throws TrustBundleException
	{
		if (this.isExistingBundle(bundle.getBundleName()))
			throw new TrustBundleAlreadyExistsException("Bundle with name " + bundle.getBundleName() + "  already exists.");
		
		bundle.setId(UUID.randomUUID().toString());
		bundle.setCreateTime(Instant.now());
		
        Connection con = null;
        PreparedStatement pstmt = null;
        try 
        {
            con = DbConnectionManager.getConnection();

            pstmt = con.prepareStatement(INSERT_TRUST_BUNDLE);
            pstmt.setString(1, bundle.getId());
            pstmt.setString(2, bundle.getBundleName());
            pstmt.setString(3, bundle.getBundleURL());
            pstmt.setString(4, bundle.getCheckSum());
            if (bundle.getLastRefreshAttempt() != null)
            	pstmt.setLong(5, bundle.getLastRefreshAttempt().toEpochMilli());
            else
            	pstmt.setNull(5, Types.BIGINT);
            if (bundle.getLastSuccessfulRefresh() != null)
            	pstmt.setLong(6, bundle.getLastSuccessfulRefresh().toEpochMilli());
            else
            	pstmt.setNull(6, Types.BIGINT);
            if (bundle.getLastRefreshError() != null)
            	pstmt.setString(7, bundle.getLastRefreshError().name());
            else
            	pstmt.setNull(7, Types.VARCHAR);            
            pstmt.setInt(8, bundle.getRefreshInterval());
            pstmt.setBytes(9, bundle.getSigningCertificateData());
            pstmt.setLong(10, Instant.now().toEpochMilli());
            
            pstmt.execute();
        }
        catch (Exception e) 
        {
            throw new TrustBundleException("Failed to insert trust bundle.");
        }
        finally 
        {
            DbConnectionManager.closeConnection(pstmt, con);
        }
        
        return bundle;	
	}

	@Override
	public TrustBundle updateTrustBundleAttributes(String bundleName, TrustBundle updatedBundle)
			throws TrustBundleException
	{	
		if (!this.isExistingBundle(bundleName))
			throw new TrustBundleNotFoundException("Bundle with name " + bundleName + " does not exist.");
		
        Connection con = null;
        PreparedStatement pstmt = null;
        try 
        {
            con = DbConnectionManager.getConnection();

            pstmt = con.prepareStatement(UPDATE_TRUST_BUNDLE);
            pstmt.setString(1, updatedBundle.getBundleName());
            pstmt.setString(2, updatedBundle.getBundleURL());
            pstmt.setString(3, updatedBundle.getCheckSum());
            pstmt.setLong(4, updatedBundle.getLastRefreshAttempt().toEpochMilli());
            pstmt.setLong(5, updatedBundle.getLastSuccessfulRefresh().toEpochMilli());
            pstmt.setString(6, updatedBundle.getLastRefreshError().name());
            pstmt.setInt(7, updatedBundle.getRefreshInterval());
            pstmt.setBytes(8, updatedBundle.getSigningCertificateData());
            
            pstmt.setString(9, bundleName.toUpperCase());
            
            pstmt.execute();
        }
        catch (Exception e) 
        {
            throw new TrustBundleException("Failed to update trust bundle.");
        }
        finally 
        {
            DbConnectionManager.closeConnection(pstmt, con);
        }
        
        return updatedBundle;	
	}

	@Override
	public TrustBundle updateSigningCertificate(String bundleName, byte[] signingCert) throws TrustBundleException
	{
		if (!this.isExistingBundle(bundleName))
			throw new TrustBundleNotFoundException("Bundle with name " + bundleName + " does not exist.");
		
        Connection con = null;
        PreparedStatement pstmt = null;
        try 
        {
            con = DbConnectionManager.getConnection();

            pstmt = con.prepareStatement(UPDATE_SIGNING_CERTIFICATE);

            pstmt.setBytes(1, signingCert);
            
            pstmt.setString(2, bundleName.toUpperCase());
            
            pstmt.execute();
        }
        catch (Exception e) 
        {
            throw new TrustBundleException("Failed to update trust bundle.");
        }
        finally 
        {
            DbConnectionManager.closeConnection(pstmt, con);
        }
        
        return this.getTrustBundle(bundleName);	
	}

	@Override
	public void deleteTrustBundle(String bundleName) throws TrustBundleException
	{
		final TrustBundle trustBundle = getTrustBundle(bundleName);
		
        Connection con = null;
        PreparedStatement pstmt = null;
        boolean abortTransaction = false;
        try 
        {
        	this.deleteAnchorsByBundleId(trustBundle.getId());
        	
        	con = DbConnectionManager.getTransactionConnection();
            pstmt = con.prepareStatement(DELETE_BUNDLE);
            pstmt.setString(1, bundleName.toUpperCase());
            pstmt.execute();
        }
        catch (Exception e) 
        {
            abortTransaction = true;
            throw new TrustBundleException("Failed to delete bundle from store.", e);
        }
        finally 
        {
            DbConnectionManager.closeStatement(pstmt);
            DbConnectionManager.closeTransactionConnection(pstmt, con, abortTransaction);
        }
	}
	
	@Override
	public TrustBundleAnchor addTrustBundleAnchor(X509Certificate anchor, String trustBundleId) throws TrustBundleException
	{
		final TrustBundleAnchor trustAnchor = new TrustBundleAnchor();
		trustAnchor.setId(UUID.randomUUID().toString());
		trustAnchor.setAnchorData(CertUtils.x509CertificateToBytes(anchor));
		trustAnchor.setTrustBundleId(trustBundleId);
		
        Connection con = null;
        PreparedStatement pstmt = null;
        try 
        {
            con = DbConnectionManager.getConnection();

            
            pstmt = con.prepareStatement(INSERT_TRUST_BUNDLE_ANCHOR);
            pstmt.setString(1, trustAnchor.getId());
            pstmt.setString(2, trustAnchor.getDistinguishedName());
            pstmt.setString(3, trustAnchor.getSerial());
            pstmt.setString(4, trustAnchor.getThumbprint());
            pstmt.setLong(5, trustAnchor.getValidStartDate().toEpochMilli());
            pstmt.setLong(6, trustAnchor.getValidEndDate().toEpochMilli());
            pstmt.setBytes(7, trustAnchor.getAnchorData());
            pstmt.setString(8, trustAnchor.getTrustBundleId());
            
            pstmt.execute();
        }
        catch (Exception e) 
        {
            throw new TrustBundleException("Failed to insert anchor.");
        }
        finally 
        {
            DbConnectionManager.closeConnection(pstmt, con);
        }
        
        return trustAnchor;	
		
	}
	
	
	@Override
	public void deleteAnchorsByBundleId(String bundleId) throws TrustBundleException
	{
        Connection con = null;
        PreparedStatement pstmt = null;
        boolean abortTransaction = false;
        try 
        {
        	con = DbConnectionManager.getTransactionConnection();
            pstmt = con.prepareStatement(DELETE_ANCHORS_BY_BUNLDE_ID);
            pstmt.setString(1, bundleId);
            pstmt.execute();
        }
        catch (Exception e) 
        {
            abortTransaction = true;
            throw new TrustBundleException("Failed to delete bundle anchors from store.", e);
        }
        finally 
        {
            DbConnectionManager.closeStatement(pstmt);
            DbConnectionManager.closeTransactionConnection(pstmt, con, abortTransaction);
        }
	}
	
	protected Collection<TrustBundleAnchor> loadAnchorsByBundleId(String bundleId) throws TrustBundleException
	{
		   final List<TrustBundleAnchor> trustBundleAnchors = new ArrayList<>();
		   Connection con = null;
		   PreparedStatement pstmt = null;
		   ResultSet rs = null;
		   try 
		   {
			   con = DbConnectionManager.getConnection();

	           pstmt = con.prepareStatement(LOAD_ANCHORS_BY_BUNDLE_ID);
	           pstmt.setString(1, bundleId);
	            // Set the fetch size. This will prevent some JDBC drivers from trying
	            // to load the entire result set into memory.
	            DbConnectionManager.setFetchSize(pstmt, 500);
	            rs = pstmt.executeQuery();
	            while (rs.next()) 
	            	trustBundleAnchors.add(trustBundleAnchorFromResultSet(rs));
		   }
	       catch (SQLException e) 
		   {
	    	   throw new TrustBundleException("Failed to load trust bundle anchors.", e);
	       }
		   finally 
		   {
			   DbConnectionManager.closeConnection(rs, pstmt, con);
		   }
		   
		   return trustBundleAnchors;
	}
	
	protected TrustBundle trustBundleFromResultSet(final ResultSet rs) throws SQLException
	{
		final TrustBundle bundle = new TrustBundle();

		bundle.setId(rs.getString(1));
		bundle.setBundleName(rs.getString(2));
		bundle.setBundleURL(rs.getString(3));
		bundle.setCheckSum(rs.getString(4));
		
		if (rs.getLong(5) != 0)
			bundle.setLastRefreshAttempt(Instant.ofEpochMilli(rs.getLong(5)));
		
		if (rs.getLong(6) != 0)
		bundle.setLastSuccessfulRefresh(Instant.ofEpochMilli(rs.getLong(6)));
		
		if (!StringUtils.isEmpty(rs.getString(7)))
				bundle.setLastRefreshError(BundleRefreshError.valueOf(rs.getString(7)));
		
		bundle.setRefreshInterval(rs.getInt(8));
		bundle.setSigningCertificateData(rs.getBytes(9));
		bundle.setCreateTime(Instant.ofEpochMilli(rs.getLong(10)));

		return bundle;		
	}
	
	protected TrustBundleAnchor trustBundleAnchorFromResultSet(final ResultSet rs) throws SQLException
	{
		final TrustBundleAnchor anchor = new TrustBundleAnchor();
		
		anchor.setId(rs.getString(1));
		anchor.setDistinguishedName(rs.getString(2));
		anchor.setSerial(rs.getString(3));
		anchor.setThumbprint(rs.getString(4));
		anchor.setValidStartDate(Instant.ofEpochMilli(rs.getLong(5)));
		anchor.setValidEndDate(Instant.ofEpochMilli(rs.getLong(6)));
		anchor.setAnchorData(rs.getBytes(7));
		anchor.setTrustBundleId(rs.getString(8));
		
		return anchor;
	}
	
	protected boolean isExistingBundle(String bundleName)
	{
		try
		{
			return this.getTrustBundle(bundleName) != null;
		}
		catch(TrustBundleException e)
		{
			return false;
		}
	}
	
}
