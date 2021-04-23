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
                 org.jivesoftware.openfire.certificate.Certificate,
                 org.jivesoftware.openfire.certificate.CertificateManager,
                 org.jivesoftware.openfire.group.Group,
                 org.jivesoftware.util.JiveGlobals,
                 org.jivesoftware.util.LocaleUtils,
                 org.jivesoftware.util.ParamUtils"
%><%@ page import="org.jivesoftware.util.StringUtils"%>
<%@ page import="org.xmpp.packet.JID" %>
<%@ page import="org.xmpp.packet.Presence" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.util.Date" %>
<%@ page import="java.util.Collection" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%!
    final int DEFAULT_RANGE = 100;
    final int[] RANGE_PRESETS = {25, 50, 75, 100, 500, 1000, -1};
%>

<jsp:useBean id="webManager" class="org.jivesoftware.util.WebManager"  />
<% webManager.init(request, response, session, application, out ); %>

<html>
    <head>
        <title><fmt:message key="certificate.summary.title"/></title>
        <meta name="pageID" content="certificate-summary"/>
        <meta name="helpPage" content="about_certificates.html"/>
    </head>
    <body>

<%  // Get parameters
    int start = ParamUtils.getIntParameter(request,"start",0);
    int range = ParamUtils.getIntParameter(request,"range",webManager.getRowsPerPage("certificate-summary", DEFAULT_RANGE));

    if (request.getParameter("range") != null) {
        webManager.setRowsPerPage("certificate-summary", range);
    }

    Collection<Certificate> certs = CertificateManager.getInstance().getCertificates();
    int certCount = certs.size();

    for (Certificate cert : certs)
    {
    	String[] names = cert.getDomain().split(",");
    	StringBuilder builder = new StringBuilder();
    	for (int i = 0; i < names.length; ++i)
    	{
    		builder.append(names[i]);
    		if (i < (names.length - 1))
    			builder.append("\r\n");
    	}
    	cert.setDomain(builder.toString());
    }
    
    // paginator vars
    int numPages = (int)Math.ceil((double)certCount/(double)range);
    int curPage = (start/range) + 1;
%>

<%  if (request.getParameter("deletesuccess") != null) { %>

    <div class="jive-success">
    <table cellpadding="0" cellspacing="0" border="0">
    <tbody>
        <tr><td class="jive-icon"><img src="images/success-16x16.gif" width="16" height="16" border="0" alt=""></td>
        <td class="jive-icon-label">
        <fmt:message key="certificate.summary.deleted" />
        </td></tr>
    </tbody>
    </table>
    </div><br>

<%  } 
    else if (request.getParameter("success") != null) { %>

    <div class="jive-success">
    <table cellpadding="0" cellspacing="0" border="0">
    <tbody>
        <tr><td class="jive-icon"><img src="images/success-16x16.gif" width="16" height="16" border="0" alt=""></td>
        <td class="jive-icon-label">
        <fmt:message key="certificate.summary.created" />
        </td></tr>
    </tbody>
    </table>
    </div><br>


<%  } %>

<p>
<fmt:message key="certificate.summary.total_certs" />:
<b><%= LocaleUtils.getLocalizedNumber(certCount) %></b> --

<%  if (numPages > 1) { %>

    <fmt:message key="global.showing" />
    <%= LocaleUtils.getLocalizedNumber(start+1) %>-<%= LocaleUtils.getLocalizedNumber(start+range > certCount ? certCount:start+range) %>,

<%  } %>
<fmt:message key="certificate.summary.sorted" />

-- <fmt:message key="certificate.summary.certs_per_page" />:
<select size="1" onchange="location.href='certificate-summary.jsp?start=0&range=' + this.options[this.selectedIndex].value;">

    <% for (int aRANGE_PRESETS : RANGE_PRESETS) { %>

    <option value="<%  if (aRANGE_PRESETS > 0) { %><%= aRANGE_PRESETS %><%  }else{ %><%= certCount %><%}%>"
            <%= (aRANGE_PRESETS == range ? "selected" : "") %>><%  if (aRANGE_PRESETS > 0) { %><%= aRANGE_PRESETS %><%  }else{ %><%= certCount %><%}%>
    </option>

    <% } %>

</select>
</p>

<%  if (numPages > 1) { %>

    <p>
    <fmt:message key="global.pages" />:
    [
    <%  int num = 15 + curPage;
        int s = curPage-1;
        if (s > 5) {
            s -= 5;
        }
        if (s < 5) {
            s = 0;
        }
        if (s > 2) {
    %>
        <a href="certificate-summary.jsp?start=0&range=<%= range %>">1</a> ...

    <%
        }
        int i;
        for (i=s; i<numPages && i<num; i++) {
            String sep = ((i+1)<numPages) ? " " : "";
            boolean isCurrent = (i+1) == curPage;
    %>
        <a href="certificate-summary.jsp?start=<%= (i*range) %>&range=<%= range %>"
         class="<%= ((isCurrent) ? "jive-current" : "") %>"
         ><%= (i+1) %></a><%= sep %>

    <%  } %>

    <%  if (i < numPages) { %>

        ... <a href="certificate-summary.jsp?start=<%= ((numPages-1)*range) %>&range=<%= range %>"><%= numPages %></a>

    <%  } %>

    ]

    </p>

<%  } %>

<div class="jive-table">
<table cellpadding="0" cellspacing="0" border="0" width="100%">
<thead>
    <tr>
        <th>&nbsp;</th>
        <th nowrap><fmt:message key="certificate.summary.domain" /></th>  
        <th nowrap><fmt:message key="certificate.summary.certdn" /></th>        
        <th nowrap><fmt:message key="certificate.summary.certValidFrom" /></th>
        <th nowrap><fmt:message key="certificate.summary.certValidTo" /></th>
        <th nowrap><fmt:message key="certificate.summary.certThumbPrint" /></th>
        <th nowrap><fmt:message key="global.delete" /></th>
    </tr>
</thead>
<tbody>

<%  // Print the list of certificates
    if (certs.isEmpty()) {
%>
    <tr>
        <td align="center" colspan="7">
            <fmt:message key="certificate.summary.not_cert" />
        </td>
    </tr>

<%
    }
    int i = start;
    for (Certificate cert : certs) {
        i++;

%>
    <tr class="jive-<%= (((i%2)==0) ? "even" : "odd") %>">
        <td width="1%">
            <%= i %>
        </td>
        <td width="19">
            <%= cert.getDomain() %>
        </td>    
        <td width="20">
            <%= cert.getDistinguishedName() %>
        </td>           
        <td width="15%">
            <%= JiveGlobals.formatDate(Date.from(cert.getValidStartDate())) %>
        </td>             
        <td width="15">
            <%= JiveGlobals.formatDate(Date.from(cert.getValidEndDate())) %>
        </td> 
        <td width="20%">
            <%= cert.getThumbprint() %>
        </td>            
        <td width="1%" align="center" style="border-right:1px #ccc solid;">
            <a href="certificate-delete.jsp?thumbprint=<%= URLEncoder.encode(cert.getThumbprint(), "UTF-8") %>"
             title="<fmt:message key="global.click_delete" />"
             ><img src="images/delete-16x16.gif" width="16" height="16" border="0" alt="<fmt:message key="global.click_delete" />"></a>
        </td>
    </tr>

<%
    }
%>
</tbody>
</table>
</div>

<%  if (numPages > 1) { %>

    <p>
    <fmt:message key="global.pages" />:
    [
    <%  int num = 15 + curPage;
        int s = curPage-1;
        if (s > 5) {
            s -= 5;
        }
        if (s < 5) {
            s = 0;
        }
        if (s > 2) {
    %>
        <a href="certificate-summary.jsp?start=0&range=<%= range %>">1</a> ...

    <%
        }
        for (i=s; i<numPages && i<num; i++) {
            String sep = ((i+1)<numPages) ? " " : "";
            boolean isCurrent = (i+1) == curPage;
    %>
        <a href="certificate-summary.jsp?start=<%= (i*range) %>&range=<%= range %>"
         class="<%= ((isCurrent) ? "jive-current" : "") %>"
         ><%= (i+1) %></a><%= sep %>

    <%  } %>

    <%  if (i < numPages) { %>

        ... <a href="certificate-summary.jsp?start=<%= ((numPages-1)*range) %>&range=<%= range %>"><%= numPages %></a>

    <%  } %>

    ]

    </p>

<%  } %>

    </body>
</html>
