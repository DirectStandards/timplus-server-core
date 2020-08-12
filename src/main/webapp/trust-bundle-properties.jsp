<%@ page contentType="text/html; charset=UTF-8" %>
<%--
  -
  - Copyright (C) 2004-2008 Jive Software. All rights reserved.
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -     http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
--%>

<%@ page import="org.jivesoftware.openfire.PresenceManager,
                 org.jivesoftware.openfire.admin.AdminManager,
                 org.jivesoftware.openfire.group.Group,
                 org.jivesoftware.openfire.trustbundle.TrustBundle,
                 org.jivesoftware.openfire.trustbundle.TrustBundleAnchor,
                 org.jivesoftware.openfire.trustbundle.TrustBundleManager,
                 org.jivesoftware.openfire.trustbundle.TrustBundleNotFoundException,
                 org.jivesoftware.util.DomainResolver,
                 org.jivesoftware.openfire.domain.DomainNotFoundException"
    errorPage="error.jsp"
%>
<%@ page import="org.jivesoftware.util.JiveGlobals"%>
<%@ page import="org.jivesoftware.util.LocaleUtils"%>
<%@ page import="org.jivesoftware.util.ParamUtils"%>
<%@ page import="org.jivesoftware.util.StringUtils"%>
<%@ page import="org.xmpp.packet.JID"%><%@ page import="org.xmpp.packet.Presence"%>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.util.Collection" %>
<%@ page import="java.util.Date" %>
<%@ page import="java.util.Map" %>
<%@ page import="org.jivesoftware.util.StringUtils" %>

<%@ taglib uri="admin" prefix="admin" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<jsp:useBean id="webManager" class="org.jivesoftware.util.WebManager" />

<%  // Get parameters //
    boolean cancel = request.getParameter("cancel") != null;
    boolean delete = request.getParameter("delete") != null;
    boolean refresh = request.getParameter("refresh") != null;
    String bundleName = ParamUtils.getParameter(request,"bundleName");


    
    // Handle a cancel
    if (cancel) {
        response.sendRedirect("trust-bundle-summary.jsp");
        return;
    }

    // Handle a delete
    if (delete) {
        response.sendRedirect("trust-bundle-delete.jsp?bundleName=" + URLEncoder.encode(bundleName, "UTF-8"));
        return;
    }


    // Load the trust bundle object
    TrustBundle bundle = null;
    try {
    	bundle = TrustBundleManager.getInstance().getTrustBundle(bundleName);
    }
    catch (TrustBundleNotFoundException unfe) {
    }

    if (refresh)
    {
    	TrustBundleManager.getInstance().refreshBundle(bundle);
    	
    	response.sendRedirect("trust-bundle-summary.jsp");
    }
    
%>

<html>
    <head>
        <title><fmt:message key="trustbundle.properties.title"/></title>
        <meta name="subPageID" content="trust-bundle-properties"/>
        <meta name="extraParams" content="<%= "bundleName="+URLEncoder.encode(bundleName, "UTF-8") %>"/>
        <meta name="helpPage" content="edit_trust_bundle_properties.html"/>
    </head>
    <body>

<p>
<fmt:message key="trustbundle.properties.info" />
</p>

<%  if (request.getParameter("success") != null) { %>

    <div class="jive-success">
    <table cellpadding="0" cellspacing="0" border="0">
    <tbody>
        <tr><td class="jive-icon"><img src="images/success-16x16.gif" width="16" height="16" border="0" alt=""></td>
        <td class="jive-icon-label">
        <fmt:message key="trustbundle.properties.created" />
        </td></tr>
    </tbody>
    </table>
    </div><br>


<%  } else if (request.getParameter("editsuccess") != null) { %>

    <div class="jive-success">
    <table cellpadding="0" cellspacing="0" border="0">
    <tbody>
        <tr><td class="jive-icon"><img src="images/success-16x16.gif" width="16" height="16" border="0" alt=""></td>
        <td class="jive-icon-label">
        <fmt:message key="trustbundle.properties.update" />
        </td></tr>
    </tbody>
    </table>
    </div><br>

<% } else if (bundle == null) { %>
    <div class="warning">
    <table cellpadding="0" cellspacing="0" border="0">
    <tbody>
        <tr>
        <td class="jive-icon-label">
            <fmt:message key="error.specific_trustbundle_not_found">
                <fmt:param value="<%= StringUtils.escapeHTMLTags(bundleName)%>" />
            </fmt:message>
        </td></tr>
    </tbody>
    </table>
    </div><br>
<%  } %>

<div class="jive-table">
<table cellpadding="0" cellspacing="0" border="0" width="100%">
<thead>
    <tr>
        <th colspan="2">
            <fmt:message key="trustbundle.properties.title" />
        </th>
    </tr>
</thead>
<tbody>
    <% if (bundle == null) { %>
    <tr>
        <td colspan="2" align="center">
            <fmt:message key="error.requested_trust_bundle_not_found" />
        </td>
    </tr>
    <% } else { %>
    <tr>
        <td class="c1">
            <fmt:message key="trustbundle.create.bundle" />:
        </td>
        <td>
            <%= StringUtils.escapeHTMLTags(bundle.getBundleName()) %>
        </td>
    </tr>
    <tr>
        <td class="c1">
            <fmt:message key="trustbundle.create.bundleURL" />:
        </td>
        <td>
            <%= bundle.getBundleURL() %>
        </td>
    </tr>
    <tr>
        <td class="c1">
            <fmt:message key="trustbundle.create.bundle_refresh_interval" />:
        </td>
        <td>
            <%= bundle.getRefreshInterval() %>
        </td>
    </tr>    
    <tr>
        <td class="c1">
            <fmt:message key="trustbundle.properties.update" />:
        </td>
        <td>
            <%= bundle.getCreateTime() != null ? JiveGlobals.formatDate(Date.from(bundle.getCreateTime())) : "&nbsp;" %>
        </td>
    </tr>

    <% } %>
</tbody>
</table>
</div>


        <p/><br/>
		<p>
		<fmt:message key="trustbundle.properties.anchors.info" />
		</p>

		<div class="jive-table">
		<table cellpadding="0" cellspacing="0" border="0" width="100%">
		<thead>
		    <tr>
		        <th>&nbsp;</th>
		        <th nowrap><fmt:message key="trustbundle.properties.anchordn" /></th>
		        <th nowrap><fmt:message key="trustbundle.properties.anchorValidFrom" /></th>        
		        <th nowrap><fmt:message key="trustbundle.properties.anchorValidTo" /></th>
		        <th nowrap><fmt:message key="trustbundle.properties.anchorThumbPrint" /></th>        
		    </tr>
		</thead>
		<tbody>

		<%  // Print the list of trust anchors
		    Collection<TrustBundleAnchor> anchors = bundle.getTrustBundleAnchors();
		    if (anchors.isEmpty()) {
		%>
		    <tr>
		        <td align="center" colspan="7">
		            <fmt:message key="trustbundle.summary.no_anchor" />
		        </td>
		    </tr>
		
		<%
		    }
		    int i = 0;
		    for (TrustBundleAnchor anchor : anchors) {
		        i++;
		
		%>
		    <tr class="jive-<%= (((i%2)==0) ? "even" : "odd") %>">
		        <td width="1%">
		            <%= i %>
		        </td>
		        <td width="23%">
		            <%= anchor.getDistinguishedName() %>
		        </td>    
		        <td width="15%">
		            <%= JiveGlobals.formatDate(Date.from(anchor.getValidStartDate())) %>
		        </td>             
		        <td width="15%">
		            <%= JiveGlobals.formatDate(Date.from(anchor.getValidEndDate())) %>
		        </td> 
		        <td width="23%">
		            <%= anchor.getThumbprint() %>
		        </td> 
		    </tr>
		
		<%
		    }
		%>	
		


		
		</tbody>
		</table>
		</div>		

<% if (bundle != null) { %>
    <br>

        <br><br>

        <form action="trust-bundle-edit-form.jsp">
        <input type="hidden" name="bundleName" value="<%= StringUtils.escapeForXML(bundle.getBundleName()) %>">
        <input type="submit" value="<fmt:message key="global.edit_properties" />">
        </form>
        <br><br>
        <form action="trust-bundle-properties.jsp">
        <input type="hidden" name="bundleName" value="<%= StringUtils.escapeForXML(bundle.getBundleName()) %>">
        <input type="submit" name="refresh" value="<fmt:message key="trustbundle.properties.refresh" />">
        </form>

<% } %>

</body>
</html>
