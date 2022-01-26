<%--
  ~ Copyright Siemens AG, 2013-2016, 2019. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
  --%>

<%@include file="/html/init.jsp" %>
<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<div class="jumbotron">
    <core_rt:if test="${not empty welcomePageGuideLine}">
        ${welcomePageGuideLine}
    </core_rt:if>
    <core_rt:if test="${empty welcomePageGuideLine}">
	    <h1 class="display-4"><liferay-ui:message key="welcome.to.sw360" /></h1>
		<%-- Select Language --%>
		<a href="/en_US" title="English"> <img src="<%=request.getContextPath()%>/images/en.png" width="25px" height="25px"></a>
		<a href="/ja_JP" title="Japan"> <img src="<%=request.getContextPath()%>/images/jp.png" width="25px" height="25px"></a>
		<a href="/vi_VN" title="Vietnam"> <img src="<%=request.getContextPath()%>/images/vi.png" width="25px" height="25px"></a>
		<br/>
	    <p class="lead">
		<liferay-ui:message key="sw360.is.an.open.source.software.project.that.provides.both.a.web.application.and.a.repository.to.collect.organize.and.make.available.information.about.software.components.it.establishes.a.central.hub.for.software.components.in.an.organization" />
	    </p>
    </core_rt:if>
	<hr class="my-4">
	<core_rt:if test="${themeDisplay.signedIn}">
		<h3><liferay-ui:message key="you.are.signed.in.please.go.ahead.using.sw360" /></h3>
		<div class="buttons">
			<a class="btn btn-primary btn-lg" href="/group/guest/home" role="button"><liferay-ui:message key="start" /></a>
		</div>
	</core_rt:if>
	<core_rt:if test="${not themeDisplay.signedIn}">
		<h3><liferay-ui:message key="in.order.to.go.ahead.please.sign.in.or.create.a.new.account" /></h3>
		<div class="buttons">

			<portlet:actionURL name="signIn" var="signInURL">
				<portlet:param name="mvcRenderCommandName" value="/login/login" />
			</portlet:actionURL>

			<aui:form action="<%= signInURL %>" autocomplete='on' cssClass="sign-in-form" method="post" name="SignInForm">
				<div class="form-group">
					<aui:input name="saveLastPath" type="hidden" value="<%= false %>" />
					<aui:input name="redirect" type="hidden" value="" />

					<aui:input autoFocus="true" cssClass="clearable" label="email-address" name="login"
						showRequiredLabel="<%= false %>" type="text" value="">
						<aui:validator name="required" />
					</aui:input>

					<aui:input name="password" showRequiredLabel="<%= false %>" type="password">
						<aui:validator name="required" />
					</aui:input>

					<button class="btn btn-primary btn-lg" type="submit" value="sign-in" role="button"><liferay-ui:message key="sign.in" /></button>
					<a class="btn btn-outline-primary btn-lg" href="/web/guest/sign-up" role="button"><liferay-ui:message key="create.account" /></a>
				</div>
			</aui:form>

		</div>
	</core_rt:if>
</div>

<%@ include file="/html/utils/includes/requirejs.jspf" %>
<script>
	require(['jquery'], function($){
		let signedIn = "${themeDisplay.signedIn}";
		if (signedIn == 'false') {
			$('#p_p_id_com_liferay_product_navigation_user_personal_bar_web_portlet_ProductNavigationUserPersonalBarPortlet_').hide();
		}
	});
</script>