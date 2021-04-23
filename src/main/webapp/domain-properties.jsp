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
                 org.jivesoftware.openfire.domain.Domain,
                 org.jivesoftware.openfire.domain.DomainManager,
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
<%@ page import="java.util.Map" %>
<%@ page import="org.jivesoftware.util.StringUtils" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<jsp:useBean id="webManager" class="org.jivesoftware.util.WebManager" />

<%  // Get parameters //
    boolean cancel = request.getParameter("cancel") != null;
    boolean delete = request.getParameter("delete") != null;
    String domainName = ParamUtils.getParameter(request,"domain");

    // Handle a cancel
    if (cancel) {
        response.sendRedirect("domain-summary.jsp");
        return;
    }

    // Handle a delete
    if (delete) {
        response.sendRedirect("domain-delete.jsp?domain=" + URLEncoder.encode(domainName, "UTF-8"));
        return;
    }


    // Load the domain object
    Domain domain = null;
    try {
    	domain = webManager.getDomainManager().getDomain(domainName);
    }
    catch (DomainNotFoundException unfe) {
    }

%>

<html>
    <head>
        <title><fmt:message key="domain.properties.title"/></title>
        <meta name="subPageID" content="domain-properties"/>
        <meta name="extraParams" content="<%= "domain="+URLEncoder.encode(domainName, "UTF-8") %>"/>
        <meta name="helpPage" content="edit_domain_properties.html"/>
    </head>
    <body>

<p>
<fmt:message key="domain.properties.info" />
</p>

<%  if (request.getParameter("success") != null) { %>

    <div class="jive-success">
    <table cellpadding="0" cellspacing="0" border="0">
    <tbody>
        <tr><td class="jive-icon"><img src="images/success-16x16.gif" width="16" height="16" border="0" alt=""></td>
        <td class="jive-icon-label">
        <fmt:message key="domain.properties.created" />
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
        <fmt:message key="domain.properties.update" />
        </td></tr>
    </tbody>
    </table>
    </div><br>

<% } else if (domain == null) { %>
    <div class="warning">
    <table cellpadding="0" cellspacing="0" border="0">
    <tbody>
        <tr>
        <td class="jive-icon-label">
            <fmt:message key="error.specific_domain_not_found">
                <fmt:param value="<%= StringUtils.escapeHTMLTags(domainName)%>" />
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
            <fmt:message key="domain.properties.title" />
        </th>
    </tr>
</thead>
<tbody>
    <% if (domain == null) { %>
    <tr>
        <td colspan="2" align="center">
            <fmt:message key="error.requested_domain_not_found" />
        </td>
    </tr>
    <% } else { %>
    <tr>
        <td class="c1">
            <fmt:message key="domain.create.domain" />:
        </td>
        <td>
            <%= StringUtils.escapeHTMLTags(JID.unescapeNode(domain.getDomainName())) %>
        </td>
    </tr>
    <tr>
        <td class="c1">
            <fmt:message key="domain.properties.isenabled" />:
        </td>
        <td>
            <%= domain.isEnabled() ? LocaleUtils.getLocalizedString("global.yes") : LocaleUtils.getLocalizedString("global.no") %>
        </td>
    </tr>
    <tr>
        <td class="c1">
            <fmt:message key="domain.properties.registered" />:
        </td>
        <td>
            <%= domain.getCreationDate() != null ? JiveGlobals.formatDate(domain.getCreationDate()) : "&nbsp;" %>
        </td>
    </tr>

    <% } %>
</tbody>
</table>
</div>

<% if (domain != null) { %>
    <br>

        <br><br>

        <form action="domain-edit-form.jsp">
        <input type="hidden" name="domain" value="<%= StringUtils.escapeForXML(domain.getDomainName()) %>">
        <input type="submit" value="<fmt:message key="global.edit_properties" />">
        </form>

<% } %>

</body>
</html>
