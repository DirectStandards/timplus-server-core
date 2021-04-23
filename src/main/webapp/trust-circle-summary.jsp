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
                 org.jivesoftware.openfire.trustcircle.TrustCircle,
                 org.jivesoftware.openfire.trustcircle.TrustCircleManager,
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
        <title><fmt:message key="trustcircle.summary.title"/></title>
        <meta name="pageID" content="trust-circle-summary"/>
        <meta name="helpPage" content="about_trust_circles.html"/>
    </head>
    <body>

<%  // Get parameters
    int start = ParamUtils.getIntParameter(request,"start",0);
    int range = ParamUtils.getIntParameter(request,"range",webManager.getRowsPerPage("trust-circle-summary", DEFAULT_RANGE));

    if (request.getParameter("range") != null) {
        webManager.setRowsPerPage("trust-circle-summary", range);
    }

    int trustCircleCount = webManager.getTrustCircleManager().getTrustCircleCount();

    // paginator vars
    int numPages = (int)Math.ceil((double)trustCircleCount/(double)range);
    int curPage = (start/range) + 1;
%>

<%  if (request.getParameter("deletesuccess") != null) { %>

    <div class="jive-success">
    <table cellpadding="0" cellspacing="0" border="0">
    <tbody>
        <tr><td class="jive-icon"><img src="images/success-16x16.gif" width="16" height="16" border="0" alt=""></td>
        <td class="jive-icon-label">
        <fmt:message key="trustcircle.summary.deleted" />
        </td></tr>
    </tbody>
    </table>
    </div><br>

<%  } %>

<p>
<fmt:message key="trustcircle.summary.total_circles" />:
<b><%= LocaleUtils.getLocalizedNumber(trustCircleCount) %></b> --

<%  if (numPages > 1) { %>

    <fmt:message key="global.showing" />
    <%= LocaleUtils.getLocalizedNumber(start+1) %>-<%= LocaleUtils.getLocalizedNumber(start+range > trustCircleCount ? trustCircleCount:start+range) %>,

<%  } %>
<fmt:message key="trustcircle.summary.sorted" />

-- <fmt:message key="trustcircle.summary.circles_per_page" />:
<select size="1" onchange="location.href='trust-circle-summary.jsp?start=0&range=' + this.options[this.selectedIndex].value;">

    <% for (int aRANGE_PRESETS : RANGE_PRESETS) { %>

    <option value="<%  if (aRANGE_PRESETS > 0) { %><%= aRANGE_PRESETS %><%  }else{ %><%= trustCircleCount %><%}%>"
            <%= (aRANGE_PRESETS == range ? "selected" : "") %>><%  if (aRANGE_PRESETS > 0) { %><%= aRANGE_PRESETS %><%  }else{ %><%= trustCircleCount %><%}%>
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
        <a href="trust-circle-summary.jsp?start=0&range=<%= range %>">1</a> ...

    <%
        }
        int i;
        for (i=s; i<numPages && i<num; i++) {
            String sep = ((i+1)<numPages) ? " " : "";
            boolean isCurrent = (i+1) == curPage;
    %>
        <a href="trust-circle-summary.jsp?start=<%= (i*range) %>&range=<%= range %>"
         class="<%= ((isCurrent) ? "jive-current" : "") %>"
         ><%= (i+1) %></a><%= sep %>

    <%  } %>

    <%  if (i < numPages) { %>

        ... <a href="trust-circle-summary.jsp?start=<%= ((numPages-1)*range) %>&range=<%= range %>"><%= numPages %></a>

    <%  } %>

    ]

    </p>

<%  } %>

<div class="jive-table">
<table cellpadding="0" cellspacing="0" border="0" width="100%">
<thead>
    <tr>
        <th>&nbsp;</th>
        <th nowrap><fmt:message key="trustcircle.summary.circle" /></th>
        <th nowrap><fmt:message key="trustcircle.summary.numdomains" /></th>        
        <th nowrap><fmt:message key="trustcircle.summary.numbundles" /></th>
        <th nowrap><fmt:message key="trustcircle.summary.numanchors" /></th>
        <th nowrap><fmt:message key="trustcircle.summary.edit" /></th>
        <th nowrap><fmt:message key="global.delete" /></th>
    </tr>
</thead>
<tbody>

<%  // Print the list of trust circles
    Collection<TrustCircle> circles = webManager.getTrustCircleManager().getTrustCircles(true, true);
    if (circles.isEmpty()) {
%>
    <tr>
        <td align="center" colspan="7">
            <fmt:message key="trustcircle.summary.not_circle" />
        </td>
    </tr>

<%
    }
    int i = start;
    for (TrustCircle circle : circles) {
        i++;

%>
    <tr class="jive-<%= (((i%2)==0) ? "even" : "odd") %>">
        <td width="1%">
            <%= i %>
        </td>
        <td width="23%">
            <a href="trust-circle-properties.jsp?circleName=<%= URLEncoder.encode(circle.getName(), "UTF-8") %>">  
            <%= circle.getName() %>
            </a>
        </td>    
        <td width="12%">
            <%= DomainManager.getInstance().getDomainCountInTrustCircle(circle.getName()) %>
        </td>             
        <td width="12%">
            <%= circle.getTrustBundles().size() %>
        </td> 
        <td width="12%">
            <%= circle.getAnchors().size() %>
        </td>            
        <td width="1%" align="center">
            <a href="trust-circle-properties.jsp?circleName=<%= URLEncoder.encode(circle.getName(), "UTF-8") %>"
             title="<fmt:message key="global.click_edit" />"
             ><img src="images/edit-16x16.gif" width="16" height="16" border="0" alt="<fmt:message key="global.click_edit" />"></a>
        </td>
        <td width="1%" align="center" style="border-right:1px #ccc solid;">
            <a href="trust-circle-delete.jsp?circleName=<%= URLEncoder.encode(circle.getName(), "UTF-8") %>"
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
        <a href="trust-circle-summary.jsp?start=0&range=<%= range %>">1</a> ...

    <%
        }
        for (i=s; i<numPages && i<num; i++) {
            String sep = ((i+1)<numPages) ? " " : "";
            boolean isCurrent = (i+1) == curPage;
    %>
        <a href="trust-circle-summary.jsp?start=<%= (i*range) %>&range=<%= range %>"
         class="<%= ((isCurrent) ? "jive-current" : "") %>"
         ><%= (i+1) %></a><%= sep %>

    <%  } %>

    <%  if (i < numPages) { %>

        ... <a href="trust-circle-summary.jsp?start=<%= ((numPages-1)*range) %>&range=<%= range %>"><%= numPages %></a>

    <%  } %>

    ]

    </p>

<%  } %>

    </body>
</html>
