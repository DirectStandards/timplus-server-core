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
                 org.jivesoftware.openfire.trustbundle.TrustBundle,
                 org.jivesoftware.openfire.trustbundle.TrustBundleManager,
                 org.jivesoftware.openfire.domain.DomainManager,
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
        <title><fmt:message key="trustbundle.summary.title"/></title>
        <meta name="pageID" content="trust-bundle-summary"/>
        <meta name="helpPage" content="about_trust_bundles.html"/>
    </head>
    <body>

<%  // Get parameters
    int start = ParamUtils.getIntParameter(request,"start",0);
    int range = ParamUtils.getIntParameter(request,"range",webManager.getRowsPerPage("trust-bundle-summary", DEFAULT_RANGE));

    if (request.getParameter("range") != null) {
        webManager.setRowsPerPage("trust-bundle-summary", range);
    }

    int trustBundleCount = TrustBundleManager.getInstance().getTrustBundles(false).size();

    // paginator vars
    int numPages = (int)Math.ceil((double)trustBundleCount/(double)range);
    int curPage = (start/range) + 1;
%>

<%  if (request.getParameter("deletesuccess") != null) { %>

    <div class="jive-success">
    <table cellpadding="0" cellspacing="0" border="0">
    <tbody>
        <tr><td class="jive-icon"><img src="images/success-16x16.gif" width="16" height="16" border="0" alt=""></td>
        <td class="jive-icon-label">
        <fmt:message key="trustbundle.summary.deleted" />
        </td></tr>
    </tbody>
    </table>
    </div><br>

<%  } %>

<p>
<fmt:message key="trustbundle.summary.total_bundles" />:
<b><%= LocaleUtils.getLocalizedNumber(trustBundleCount) %></b> --

<%  if (numPages > 1) { %>

    <fmt:message key="global.showing" />
    <%= LocaleUtils.getLocalizedNumber(start+1) %>-<%= LocaleUtils.getLocalizedNumber(start+range > trustBundleCount ? trustBundleCount:start+range) %>,

<%  } %>
<fmt:message key="trustbundle.summary.sorted" />

-- <fmt:message key="trustbundle.summary.bundles_per_page" />:
<select size="1" onchange="location.href='trust-bundle-summary.jsp?start=0&range=' + this.options[this.selectedIndex].value;">

    <% for (int aRANGE_PRESETS : RANGE_PRESETS) { %>

    <option value="<%  if (aRANGE_PRESETS > 0) { %><%= aRANGE_PRESETS %><%  }else{ %><%= trustBundleCount %><%}%>"
            <%= (aRANGE_PRESETS == range ? "selected" : "") %>><%  if (aRANGE_PRESETS > 0) { %><%= aRANGE_PRESETS %><%  }else{ %><%= trustBundleCount %><%}%>
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
        <a href="trust-bundle-summary.jsp?start=0&range=<%= range %>">1</a> ...

    <%
        }
        int i;
        for (i=s; i<numPages && i<num; i++) {
            String sep = ((i+1)<numPages) ? " " : "";
            boolean isCurrent = (i+1) == curPage;
    %>
        <a href="trust-bundle-summary.jsp?start=<%= (i*range) %>&range=<%= range %>"
         class="<%= ((isCurrent) ? "jive-current" : "") %>"
         ><%= (i+1) %></a><%= sep %>

    <%  } %>

    <%  if (i < numPages) { %>

        ... <a href="trust-bundle-summary.jsp?start=<%= ((numPages-1)*range) %>&range=<%= range %>"><%= numPages %></a>

    <%  } %>

    ]

    </p>

<%  } %>

<div class="jive-table">
<table cellpadding="0" cellspacing="0" border="0" width="100%">
<thead>
    <tr>
        <th>&nbsp;</th>
        <th nowrap><fmt:message key="trustbundle.summary.bundle" /></th>
        <th nowrap><fmt:message key="trustbundle.summary.url" /></th>        
        <th nowrap><fmt:message key="trustbundle.summary.checksum" /></th>
        <th nowrap><fmt:message key="trustbundle.summary.lastRefreshAttempt" /></th>        
        <th nowrap><fmt:message key="trustbundle.summary.lastSuccessRefresh" /></th>
        <th nowrap><fmt:message key="trustbundle.summary.numAnchors" /></th>        
        <th nowrap><fmt:message key="trustbundle.summary.edit" /></th>
        <th nowrap><fmt:message key="global.delete" /></th>
    </tr>
</thead>
<tbody>

<%  // Print the list of trust bundles
    Collection<TrustBundle> bundles = TrustBundleManager.getInstance().getTrustBundles(true);
    if (bundles.isEmpty()) {
%>
    <tr>
        <td align="center" colspan="7">
            <fmt:message key="trustbundle.summary.no_bundles" />
        </td>
    </tr>

<%
    }
    int i = start;
    for (TrustBundle bundle : bundles) {
        i++;

%>
    <tr class="jive-<%= (((i%2)==0) ? "even" : "odd") %>">
        <td width="1%">
            <%= i %>
        </td>
        <td width="23%">
            <a href="trust-bundle-properties.jsp?bundleName=<%= URLEncoder.encode(bundle.getBundleName(), "UTF-8") %>">  
            <%= bundle.getBundleName() %>
            </a>
        </td>    
        <td width="23%">
            <a href="<%= URLEncoder.encode(bundle.getBundleURL(), "UTF-8") %>">  
            <%= bundle.getBundleURL() %>
            </a>
        </td>             
        <td width="12%">
            <%= bundle.getCheckSum() != null ? bundle.getCheckSum() :  "&nbsp;" %>
        </td> 
        <td width="12%">
            <%= bundle.getLastRefreshAttempt() != null ? JiveGlobals.formatDate(Date.from(bundle.getLastRefreshAttempt())) : "&nbsp;"  %>
        </td>      
        <td width="12%">
            <%= bundle.getLastSuccessfulRefresh() != null ? JiveGlobals.formatDate(Date.from(bundle.getLastSuccessfulRefresh())) : "&nbsp;"  %>
        </td>              
        <td width="12%">
            <%= bundle.getTrustBundleAnchors().size() %>
        </td>            
        <td width="1%" align="center">
            <a href="trust-bundle-edit-form.jsp?bundleName=<%= URLEncoder.encode(bundle.getBundleName(), "UTF-8") %>"
             title="<fmt:message key="global.click_edit" />"
             ><img src="images/edit-16x16.gif" width="16" height="16" border="0" alt="<fmt:message key="global.click_edit" />"></a>
        </td>
        <td width="1%" align="center" style="border-right:1px #ccc solid;">
            <a href="trust-bundle-delete.jsp?bundleName=<%= URLEncoder.encode(bundle.getBundleName(), "UTF-8") %>"
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
        <a href="trust-bundle-summary.jsp?start=0&range=<%= range %>">1</a> ...

    <%
        }
        for (i=s; i<numPages && i<num; i++) {
            String sep = ((i+1)<numPages) ? " " : "";
            boolean isCurrent = (i+1) == curPage;
    %>
        <a href="trust-bundle-summary.jsp?start=<%= (i*range) %>&range=<%= range %>"
         class="<%= ((isCurrent) ? "jive-current" : "") %>"
         ><%= (i+1) %></a><%= sep %>

    <%  } %>

    <%  if (i < numPages) { %>

        ... <a href="trust-bundle-summary.jsp?start=<%= ((numPages-1)*range) %>&range=<%= range %>"><%= numPages %></a>

    <%  } %>

    ]

    </p>

<%  } %>

    </body>
</html>
