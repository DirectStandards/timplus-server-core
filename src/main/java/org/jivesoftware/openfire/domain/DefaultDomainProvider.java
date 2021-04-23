package org.jivesoftware.openfire.domain;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.openfire.trustcircle.DefaultTrustCircleProvider;
import org.jivesoftware.openfire.trustcircle.TrustCircle;
import org.jivesoftware.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultDomainProvider implements DomainProvider
{
	private static final Logger Log = LoggerFactory.getLogger(DefaultDomainProvider.class);

    private static final String DOMAIN_COUNT = "SELECT count(*) FROM ofDomain";	
	
    private static final String LOAD_DOMAIN ="SELECT * FROM ofDomain WHERE domainName=?";
	
    private static final String ALL_DOMAINS = "SELECT * FROM ofDomain ORDER BY domainName";
    
    private static final String ALL_ENABLED_DOMAINS = "SELECT * FROM ofDomain where enabled=1 ORDER BY domainName";    

    private static final String ALL_DOMAIN_NAMES = "SELECT domainName FROM ofDomain ORDER BY domainName";
    
    private static final String ALL_ENABLED_DOMAIN_NAMES = "SELECT domainName FROM ofDomain where enabled=1 ORDER BY domainName";
  
    private static final String SEARCH_DOMAINS = "SELECT * FROM ofDomain where domainName like(?) ORDER BY domainName";    
 
    private static final String SEARCH_ENABLED_DOMAINS = "SELECT * FROM ofDomain where domainName like(?) and enabled=1 ORDER BY domainName";      
    
    private static final String INSERT_DOMAIN =
            "INSERT INTO ofDomain (domainName,enabled,creationDate,modificationDate) " +
            "VALUES (?,?,?,?)"; 
    
    private static final String DELETE_DOMAIN = "DELETE FROM ofDomain WHERE domainName=?";
    
    private static final String ENABLE_DOMAIN = "UPDATE ofDomain SET enabled=? WHERE domainName=?";   
    
	private static final String LOAD_CIRCLE_DOMAINS = "SELECT domainName FROM ofTrustCircleDomainReltn where trustCircleId = ?";	
    
	@Override
	public Domain getDomain(String domainName) throws DomainNotFoundException
	{
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try 
        {
            con = DbConnectionManager.getConnection();
            pstmt = con.prepareStatement(LOAD_DOMAIN);
            pstmt.setString(1, domainName);
            rs = pstmt.executeQuery();
            if (!rs.next()) 
            {
                throw new DomainNotFoundException();
            }

            return domainFromResultSet(rs);
        }
        catch (Exception e) 
        {
            throw new DomainNotFoundException(e);
        }
        finally 
        {
            DbConnectionManager.closeConnection(rs, pstmt, con);
        }
	}

	@Override
	public Collection<Domain> getDomains(boolean enabledOnly)
	{
	   final List<Domain> domains = new ArrayList<>();
	   Connection con = null;
	   PreparedStatement pstmt = null;
	   ResultSet rs = null;
	   try 
	   {
		   con = DbConnectionManager.getConnection();

           pstmt = con.prepareStatement((enabledOnly) ? ALL_ENABLED_DOMAINS : ALL_DOMAINS);
            // Set the fetch size. This will prevent some JDBC drivers from trying
            // to load the entire result set into memory.
            DbConnectionManager.setFetchSize(pstmt, 500);
            rs = pstmt.executeQuery();
            while (rs.next()) 
            	domains.add(domainFromResultSet(rs));
	   }
       catch (SQLException e) 
	   {
    	   Log.error(e.getMessage(), e);
       }
	   finally 
	   {
		   DbConnectionManager.closeConnection(rs, pstmt, con);
	   }
	   
	   return domains;
	}

	@Override
	public Collection<String> getDomainNames(boolean enabledOnly)
	{
	   final List<String> domains = new ArrayList<>();
	   Connection con = null;
	   PreparedStatement pstmt = null;
	   ResultSet rs = null;
	   try 
	   {
		   con = DbConnectionManager.getConnection();

           pstmt = con.prepareStatement((enabledOnly) ? ALL_ENABLED_DOMAIN_NAMES : ALL_DOMAIN_NAMES);
            // Set the fetch size. This will prevent some JDBC drivers from trying
            // to load the entire result set into memory.
            DbConnectionManager.setFetchSize(pstmt, 500);
            rs = pstmt.executeQuery();
            while (rs.next()) 
            	domains.add(rs.getString(1));
	   }
       catch (SQLException e) 
	   {
    	   Log.error(e.getMessage(), e);
       }
	   finally 
	   {
		   DbConnectionManager.closeConnection(rs, pstmt, con);
	   }
	   
	   return domains;
	}

	public int getDomainCount()
	{
        int count = 0;
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try 
        {
            con = DbConnectionManager.getConnection();
            pstmt = con.prepareStatement(DOMAIN_COUNT);
            rs = pstmt.executeQuery();
            if (rs.next()) 
                count = rs.getInt(1);
        }
        catch (SQLException e) 
        {
            Log.error(e.getMessage(), e);
        }
        finally 
        {
            DbConnectionManager.closeConnection(rs, pstmt, con);
        }
        return count;		
	}
	
	@Override
	public Collection<Domain> findDomains(String searchStr, boolean enabledOnly)
	{
		   final List<Domain> domains = new ArrayList<>();
		   Connection con = null;
		   PreparedStatement pstmt = null;
		   ResultSet rs = null;
		   try 
		   {
			   final StringBuilder search = new StringBuilder("%").append(searchStr).append("%");
			   
			   con = DbConnectionManager.getConnection();

	           pstmt = con.prepareStatement((enabledOnly) ? SEARCH_ENABLED_DOMAINS : SEARCH_DOMAINS );
	           pstmt.setString(1, search.toString());
	            // Set the fetch size. This will prevent some JDBC drivers from trying
	            // to load the entire result set into memory.
	            DbConnectionManager.setFetchSize(pstmt, 500);
	            rs = pstmt.executeQuery();
	            while (rs.next()) 
	            	domains.add(domainFromResultSet(rs));
		   }
	       catch (SQLException e) 
		   {
	    	   Log.error(e.getMessage(), e);
	       }
		   finally 
		   {
			   DbConnectionManager.closeConnection(rs, pstmt, con);
		   }
		   
		   return domains;
	}

	@Override
	public boolean isRegisteredDomain(String domainName)
	{
		try
		{
			getDomain(domainName);
		}
		catch (DomainNotFoundException ex)
		{
			return false;
		}
		
		return true;
	}

	@Override
	public Domain createDomain(String domainName, boolean enabled) throws DomainAlreadyExistsException
	{
		if (isRegisteredDomain(domainName))
			throw new DomainAlreadyExistsException("Domain " + domainName + " already exists");

        // The user doesn't already exist so we can create a new user
        final Date now = new Date();
        Connection con = null;
        PreparedStatement pstmt = null;
        try 
        {
            con = DbConnectionManager.getConnection();
            pstmt = con.prepareStatement(INSERT_DOMAIN);
            pstmt.setString(1, domainName);
            pstmt.setBoolean(2, enabled);
            pstmt.setString(3, StringUtils.dateToMillis(now));
            pstmt.setString(4, StringUtils.dateToMillis(now));
            pstmt.execute();
        }
        catch (SQLException e) 
        {
            throw new RuntimeException(e);
        }
        finally 
        {
            DbConnectionManager.closeConnection(pstmt, con);
        }
        
        return new Domain(domainName, enabled, now, now);
	}

	@Override
	public void deleteDomain(String domainName)
	{
        Connection con = null;
        PreparedStatement pstmt = null;
        boolean abortTransaction = false;
        try 
        {
        	con = DbConnectionManager.getTransactionConnection();
            pstmt = con.prepareStatement(DELETE_DOMAIN);
            pstmt.setString(1, domainName);
            pstmt.execute();
        }
        catch (Exception e) 
        {
            Log.error(e.getMessage(), e);
            abortTransaction = true;
        }
        finally 
        {
            DbConnectionManager.closeStatement(pstmt);
            DbConnectionManager.closeTransactionConnection(pstmt, con, abortTransaction);
        }
	}

	@Override
	public void enableDomain(String domainName, boolean enable) throws DomainNotFoundException
	{
		final Domain domain = this.getDomain(domainName);
		
		if (domain.isEnabled() == enable)
			return;
		
        Connection con = null;
        PreparedStatement pstmt = null;
        try 
        {
            con = DbConnectionManager.getConnection();
            pstmt = con.prepareStatement(ENABLE_DOMAIN);

            pstmt.setBoolean(1, enable);
            pstmt.setString(2, domainName);
            pstmt.executeUpdate();
        }
        catch (SQLException sqle) 
        {
            throw new RuntimeException(sqle);
        }
        finally 
        {
            DbConnectionManager.closeConnection(pstmt, con);
        }
	}
	
	public Collection<Domain> getDomainsByTrustCircle(String circleName)
	{

	   // Get the trust circle first
	   TrustCircle circle = null;
	   final DefaultTrustCircleProvider prov = new DefaultTrustCircleProvider();
	   try
	   {
		   circle = prov.getTrustCircle(circleName, false, false);
	   }
	   catch (Exception e)
	   {
		   Log.error("Failed to get trust circle information.", e);
		   return Collections.emptyList();
	   }
	   
	   Connection con = null;
	   PreparedStatement pstmt = null;
	   ResultSet rs = null;
	   try 
	   {
		   con = DbConnectionManager.getConnection();

           pstmt = con.prepareStatement(LOAD_CIRCLE_DOMAINS);
           pstmt.setString(1, circle.getId());
            // Set the fetch size. This will prevent some JDBC drivers from trying
            // to load the entire result set into memory.
            DbConnectionManager.setFetchSize(pstmt, 500);
            rs = pstmt.executeQuery();
            final Collection<Domain> domains = new ArrayList<>();
            while (rs.next()) 
            {
            	domains.add(this.getDomain(rs.getString(1)));
            }
            
            return domains;
	   }
       catch (Exception e) 
	   {
		   Log.error("Failed to get domains for trust circle " + circleName, e);
		   return Collections.emptyList();
       }
	   finally 
	   {
		   DbConnectionManager.closeConnection(rs, pstmt, con);
	   }
	}
	
	protected Domain domainFromResultSet(final ResultSet rs) throws SQLException
	{
        final String domain = rs.getString(1);
        final boolean enabled = rs.getBoolean(2);
        final Date creationDate = new Date(Long.parseLong(rs.getString(3).trim()));
        final Date modificationDate = new Date(Long.parseLong(rs.getString(4).trim()));

        return new Domain(domain, enabled, creationDate, modificationDate);
	}
}
