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
                 org.jivesoftware.openfire.domain.Domain,
                 org.jivesoftware.openfire.domain.DomainManager,
                 org.jivesoftware.openfire.group.Group,
                 org.jivesoftware.util.JiveGlobals,
                 org.jivesoftware.util.LocaleUtils,
                 org.jivesoftware.util.ParamUtils"
%><%@ page import="org.jivesoftware.util.StringUtils"%>
<%@ page import="org.xmpp.packet.JID" %>
<%@ page import="org.xmpp.packet.Presence" %>
<%@ page import="java.net.URLEncoder" %>
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
        <title><fmt:message key="domain.summary.title"/></title>
        <meta name="pageID" content="domain-summary"/>
        <meta name="helpPage" content="about_domains.html"/>
    </head>
    <body>

<%  // Get parameters
    int start = ParamUtils.getIntParameter(request,"start",0);
    int range = ParamUtils.getIntParameter(request,"range",webManager.getRowsPerPage("domain-summary", DEFAULT_RANGE));

    if (request.getParameter("range") != null) {
        webManager.setRowsPerPage("domain-summary", range);
    }

    // Get the domain manager
    int domainCount = webManager.getDomainManager().getDomainCount();

    // paginator vars
    int numPages = (int)Math.ceil((double)domainCount/(double)range);
    int curPage = (start/range) + 1;
%>

<%  if (request.getParameter("deletesuccess") != null) { %>

    <div class="jive-success">
    <table cellpadding="0" cellspacing="0" border="0">
    <tbody>
        <tr><td class="jive-icon"><img src="images/success-16x16.gif" width="16" height="16" border="0" alt=""></td>
        <td class="jive-icon-label">
        <fmt:message key="domain.summary.deleted" />
        </td></tr>
    </tbody>
    </table>
    </div><br>

<%  } %>

<p>
<fmt:message key="domain.summary.total_domains" />:
<b><%= LocaleUtils.getLocalizedNumber(domainCount) %></b> --

<%  if (numPages > 1) { %>

    <fmt:message key="global.showing" />
    <%= LocaleUtils.getLocalizedNumber(start+1) %>-<%= LocaleUtils.getLocalizedNumber(start+range > domainCount ? domainCount:start+range) %>,

<%  } %>
<fmt:message key="domain.summary.sorted" />

-- <fmt:message key="domain.summary.domains_per_page" />:
<select size="1" onchange="location.href='domain-summary.jsp?start=0&range=' + this.options[this.selectedIndex].value;">

    <% for (int aRANGE_PRESETS : RANGE_PRESETS) { %>

    <option value="<%  if (aRANGE_PRESETS > 0) { %><%= aRANGE_PRESETS %><%  }else{ %><%= domainCount %><%}%>"
            <%= (aRANGE_PRESETS == range ? "selected" : "") %>><%  if (aRANGE_PRESETS > 0) { %><%= aRANGE_PRESETS %><%  }else{ %><%= domainCount %><%}%>
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
        <a href="domain-summary.jsp?start=0&range=<%= range %>">1</a> ...

    <%
        }
        int i;
        for (i=s; i<numPages && i<num; i++) {
            String sep = ((i+1)<numPages) ? " " : "";
            boolean isCurrent = (i+1) == curPage;
    %>
        <a href="domain-summary.jsp?start=<%= (i*range) %>&range=<%= range %>"
         class="<%= ((isCurrent) ? "jive-current" : "") %>"
         ><%= (i+1) %></a><%= sep %>

    <%  } %>

    <%  if (i < numPages) { %>

        ... <a href="domain-summary.jsp?start=<%= ((numPages-1)*range) %>&range=<%= range %>"><%= numPages %></a>

    <%  } %>

    ]

    </p>

<%  } %>

<div class="jive-table">
<table cellpadding="0" cellspacing="0" border="0" width="100%">
<thead>
    <tr>
        <th>&nbsp;</th>
        <th nowrap><fmt:message key="domain.create.domain" /></th>
        <th nowrap><fmt:message key="domain.summary.enabled" /></th>        
        <th nowrap><fmt:message key="domain.summary.created" /></th>
        <th nowrap><fmt:message key="domain.summary.edit" /></th>
        <th nowrap><fmt:message key="global.delete" /></th>
    </tr>
</thead>
<tbody>

<%  // Print the list of domains
    Collection<Domain> domains = webManager.getDomainManager().getDomains(false);
    if (domains.isEmpty()) {
%>
    <tr>
        <td align="center" colspan="7">
            <fmt:message key="domain.summary.not_domain" />
        </td>
    </tr>

<%
    }
    int i = start;
    for (Domain domain : domains) {
        i++;
        Boolean disabled = domain.isEnabled();

%>
    <tr class="jive-<%= (((i%2)==0) ? "even" : "odd") %>">
        <td width="1%">
            <%= i %>
        </td>
        <td width="23%">
            <a href="domain-properties.jsp?domain=<%= URLEncoder.encode(domain.getDomainName(), "UTF-8") %>">  
            <%= domain.getDomainName() %>
            </a>
        </td>
        <td width="10%">
            <%= domain.isEnabled() ? LocaleUtils.getLocalizedString("global.yes") : LocaleUtils.getLocalizedString("global.no") %>
        </td>        
        <td width="12%">
            <%= domain.getCreationDate() != null ? JiveGlobals.formatDate(domain.getCreationDate()) : "&nbsp;" %>
        <td width="1%" align="center">
            <a href="domain-edit-form.jsp?domain=<%= URLEncoder.encode(domain.getDomainName(), "UTF-8") %>"
             title="<fmt:message key="global.click_edit" />"
             ><img src="images/edit-16x16.gif" width="16" height="16" border="0" alt="<fmt:message key="global.click_edit" />"></a>
        </td>
        <td width="1%" align="center" style="border-right:1px #ccc solid;">
            <a href="domain-delete.jsp?domain=<%= URLEncoder.encode(domain.getDomainName(), "UTF-8") %>"
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
        <a href="domain-summary.jsp?start=0&range=<%= range %>">1</a> ...

    <%
        }
        for (i=s; i<numPages && i<num; i++) {
            String sep = ((i+1)<numPages) ? " " : "";
            boolean isCurrent = (i+1) == curPage;
    %>
        <a href="domain-summary.jsp?start=<%= (i*range) %>&range=<%= range %>"
         class="<%= ((isCurrent) ? "jive-current" : "") %>"
         ><%= (i+1) %></a><%= sep %>

    <%  } %>

    <%  if (i < numPages) { %>

        ... <a href="domain-summary.jsp?start=<%= ((numPages-1)*range) %>&range=<%= range %>"><%= numPages %></a>

    <%  } %>

    ]

    </p>

<%  } %>

    </body>
</html>
