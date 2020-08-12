package org.jivesoftware.openfire.trustanchor;

import java.security.cert.X509Certificate;
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

import org.directtruststandards.timplus.common.cert.CertUtils;
import org.directtruststandards.timplus.common.cert.Thumbprint;
import org.jivesoftware.database.DbConnectionManager;

public class DefaultTrustAnchorProvider implements TrustAnchorProvider
{	
	private static final String LOAD_ANCHORS = "SELECT * FROM ofTrustAnchor";	
	
	private static final String LOAD_ANCHOR_BY_THUMBPRINT = "SELECT * FROM ofTrustAnchor WHERE thumbprint=?";
	
	private static final String LOAD_ANCHORS_BY_THUMBPRINTS = "SELECT * from ofTrustAnchor WHERE thumbprint IN ";
	
	private static final String LOAD_ANCHORS_BY_IDS = "SELECT * from ofTrustAnchor WHERE id IN ";
	
	private static final String DELETE_ANCHORS_BY_THUMBPRINTS = "DELETE from ofTrustAnchor WHERE thumbprint IN ";
	
	private static final String DELETE_ANCHOR_BY_THUMBPRINT = "DELETE from ofTrustAnchor WHERE thumbprint=?";
	
    private static final String INSERT_TRUST_ANCHOR =
            "INSERT INTO ofTrustAnchor (id,distinguishedName,serialNumber,thumbprint,validStartDate,validEndDate,anchorData) " +
            "VALUES (?,?,?,?,?,?,?)"; 
    
	
	public DefaultTrustAnchorProvider()
	{
		
	}

	@Override
	public Collection<TrustAnchor> getAnchors() throws TrustAnchorException
	{
	   final List<TrustAnchor> trustAnchors = new ArrayList<>();
	   Connection con = null;
	   PreparedStatement pstmt = null;
	   ResultSet rs = null;
	   try 
	   {
		   con = DbConnectionManager.getConnection();

           pstmt = con.prepareStatement(LOAD_ANCHORS);
            // Set the fetch size. This will prevent some JDBC drivers from trying
            // to load the entire result set into memory.
            DbConnectionManager.setFetchSize(pstmt, 500);
            rs = pstmt.executeQuery();
            while (rs.next()) 
            	trustAnchors.add(trustAnchorFromResultSet(rs));
	   }
       catch (SQLException e) 
	   {
    	   throw new TrustAnchorException("Failed to load anchors.", e);
       }
	   finally 
	   {
		   DbConnectionManager.closeConnection(rs, pstmt, con);
	   }
	   
	   return trustAnchors;
	}

	@Override
	public TrustAnchor getAnchorByThumbprint(String thumbprint) throws TrustAnchorException
	{
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try 
        {
            con = DbConnectionManager.getConnection();
            pstmt = con.prepareStatement(LOAD_ANCHOR_BY_THUMBPRINT);
            pstmt.setString(1, thumbprint);
            rs = pstmt.executeQuery();
            if (!rs.next()) 
            {
                throw new TrustAnchorException();
            }

            return trustAnchorFromResultSet(rs);
        }
        catch (Exception e) 
        {
            throw new TrustAnchorNotFoundException(e);
        }
        finally 
        {
            DbConnectionManager.closeConnection(rs, pstmt, con);
        }
	}

	@Override
	public Collection<TrustAnchor> getAnchorsByThumbprints(Collection<String> thumbprints) throws TrustAnchorException
	{
        if (thumbprints == null || thumbprints.size() == 0)
        	return Collections.emptyList();
 
        final Collection<TrustAnchor> retVal = new ArrayList<>();
        
        final StringBuffer ids = new StringBuffer("(");
        for (String tp : thumbprints) 
        {
            if (ids.length() > 1) 
            {
            	ids.append(", ");
            }
            ids.append("'").append(tp).append("'");
        }
        ids.append(")");
        
 	   Connection con = null;
 	   PreparedStatement pstmt = null;
 	   ResultSet rs = null;
 	   try 
 	   {
 		   final String query = LOAD_ANCHORS_BY_THUMBPRINTS + ids.toString();
 		   con = DbConnectionManager.getConnection();

           pstmt = con.prepareStatement(query);
           // Set the fetch size. This will prevent some JDBC drivers from trying
           // to load the entire result set into memory.
           DbConnectionManager.setFetchSize(pstmt, 500);
           rs = pstmt.executeQuery();
           while (rs.next()) 
              retVal.add(trustAnchorFromResultSet(rs));
 	   }
       catch (Exception e) 
 	   {
     	   throw new TrustAnchorException("Failed to load anchors.", e);
       }
 	   finally 
 	   {
 		   DbConnectionManager.closeConnection(rs, pstmt, con);
 	   }
        
       return retVal;
	}

	public Collection<TrustAnchor> getAnchorsByIds(Collection<String> ids) throws TrustAnchorException
	{
        if (ids == null || ids.size() == 0)
        	return Collections.emptyList();
 
        final Collection<TrustAnchor> retVal = new ArrayList<>();
        
        final StringBuffer idsStr = new StringBuffer("(");
        for (String tp : ids) 
        {
            if (idsStr.length() > 1) 
            {
            	idsStr.append(", ");
            }
            idsStr.append("'").append(tp).append("'");
        }
        idsStr.append(")");
        
 	   Connection con = null;
 	   PreparedStatement pstmt = null;
 	   ResultSet rs = null;
 	   try 
 	   {
 		   final String query = LOAD_ANCHORS_BY_IDS + idsStr.toString();
 		   con = DbConnectionManager.getConnection();

           pstmt = con.prepareStatement(query);
           // Set the fetch size. This will prevent some JDBC drivers from trying
           // to load the entire result set into memory.
           DbConnectionManager.setFetchSize(pstmt, 500);
           rs = pstmt.executeQuery();
           while (rs.next()) 
              retVal.add(trustAnchorFromResultSet(rs));
 	   }
       catch (Exception e) 
 	   {
     	   throw new TrustAnchorException("Failed to load anchors.", e);
       }
 	   finally 
 	   {
 		   DbConnectionManager.closeConnection(rs, pstmt, con);
 	   }
        
       return retVal;
	}
	
	@Override
	public TrustAnchor addTrustAnchor(X509Certificate anchor) throws TrustAnchorException
	{
		if (this.isExistingAnchor(Thumbprint.toThumbprint(anchor).toString()))
			throw new TrustAnchorAlreadyExistsException("Anchor already exists.");

		final TrustAnchor trustAnchor = new TrustAnchor();
		trustAnchor.setId(UUID.randomUUID().toString());
		trustAnchor.setAnchorData(CertUtils.x509CertificateToBytes(anchor));
		
        Connection con = null;
        PreparedStatement pstmt = null;
        try 
        {
            con = DbConnectionManager.getConnection();

            
            pstmt = con.prepareStatement(INSERT_TRUST_ANCHOR);
            pstmt.setString(1, trustAnchor.getId());
            pstmt.setString(2, trustAnchor.getDistinguishedName());
            pstmt.setString(3, trustAnchor.getSerial());
            pstmt.setString(4, trustAnchor.getThumbprint());
            pstmt.setLong(5, trustAnchor.getValidStartDate().toEpochMilli());
            pstmt.setLong(6, trustAnchor.getValidEndDate().toEpochMilli());
            pstmt.setBytes(7, trustAnchor.getAnchorData());
            pstmt.execute();
        }
        catch (Exception e) 
        {
            throw new TrustAnchorException("Failed to insert anchor.");
        }
        finally 
        {
            DbConnectionManager.closeConnection(pstmt, con);
        }
        
        return trustAnchor;	
		
	}

	@Override
	public void deleteTrustAnchors(Collection<String> thumbprints) throws TrustAnchorException
	{
        if (thumbprints == null || thumbprints.size() == 0)
        	return;
        
        final StringBuffer ids = new StringBuffer("(");
        for (String tp : thumbprints) 
        {
            if (ids.length() > 1) 
            {
            	ids.append(", ");
            }
            ids.append("'").append(tp).append("'");
        }
        ids.append(")");
        
        Connection con = null;
        PreparedStatement pstmt = null;
        boolean abortTransaction = false;
        try 
        {
        	final String query = DELETE_ANCHORS_BY_THUMBPRINTS + ids.toString();
        	con = DbConnectionManager.getTransactionConnection();
            pstmt = con.prepareStatement(query);
            pstmt.execute();
        }
        catch (Exception e) 
        {
            abortTransaction = true;
            throw new TrustAnchorException("Failed to delete anchors from store.", e);
        }
        finally 
        {
            DbConnectionManager.closeStatement(pstmt);
            DbConnectionManager.closeTransactionConnection(pstmt, con, abortTransaction);
        }		
	}

	@Override
	public void deleteTrustAnchor(String thumbprint) throws TrustAnchorException
	{
        Connection con = null;
        PreparedStatement pstmt = null;
        boolean abortTransaction = false;
        try 
        {
        	con = DbConnectionManager.getTransactionConnection();
            pstmt = con.prepareStatement(DELETE_ANCHOR_BY_THUMBPRINT);
            pstmt.setString(1, thumbprint);
            pstmt.execute();
        }
        catch (Exception e) 
        {
            abortTransaction = true;
            throw new TrustAnchorException("Failed to delete anchor from store.", e);
        }
        finally 
        {
            DbConnectionManager.closeStatement(pstmt);
            DbConnectionManager.closeTransactionConnection(pstmt, con, abortTransaction);
        }
		
	}
	
	public static TrustAnchor trustAnchorFromResultSet(final ResultSet rs) throws SQLException
	{
		final TrustAnchor anchor = new TrustAnchor();
		
		anchor.setId(rs.getString(1));
		anchor.setDistinguishedName(rs.getString(2));
		anchor.setSerial(rs.getString(3));
		anchor.setThumbprint(rs.getString(4));
		anchor.setValidStartDate(Instant.ofEpochMilli(rs.getLong(5)));
		anchor.setValidEndDate(Instant.ofEpochMilli(rs.getLong(6)));
		anchor.setAnchorData(rs.getBytes(7));
		
		return anchor;
	}
	
	protected boolean isExistingAnchor(String thumbprint)
	{
		try
		{
			return this.getAnchorByThumbprint(thumbprint) != null;
		}
		catch(TrustAnchorException e)
		{
			return false;
		}
	}
}
