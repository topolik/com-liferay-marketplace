/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.marketplace.store.web.oauth.util;

import com.liferay.marketplace.store.web.configuration.MarketplaceStoreWebConfigurationValues;
import com.liferay.marketplace.store.web.oauth.api.MarketplaceApi;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.module.framework.ModuleServiceLifecycle;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.portal.service.CompanyLocalService;
import com.liferay.portlet.expando.DuplicateColumnNameException;
import com.liferay.portlet.expando.DuplicateTableNameException;
import com.liferay.portlet.expando.model.ExpandoColumnConstants;
import com.liferay.portlet.expando.model.ExpandoTable;
import com.liferay.portlet.expando.model.ExpandoValue;
import com.liferay.portlet.expando.service.ExpandoColumnLocalService;
import com.liferay.portlet.expando.service.ExpandoTableLocalService;
import com.liferay.portlet.expando.service.ExpandoValueLocalService;

import java.util.List;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import org.scribe.builder.api.Api;
import org.scribe.model.OAuthConfig;
import org.scribe.model.SignatureType;
import org.scribe.model.Token;
import org.scribe.oauth.OAuthService;

/**
 * @author Ryan Park
 */
@Component(immediate = true, service = OAuthManager.class)
public class OAuthManager {

	public void deleteAccessToken(User user) throws PortalException {
		_expandoValueLocalService.deleteValue(
			user.getCompanyId(), User.class.getName(), "MP", "secret",
			user.getUserId());
		_expandoValueLocalService.deleteValue(
			user.getCompanyId(), User.class.getName(), "MP", "token",
			user.getUserId());
	}

	public Token getAccessToken(User user) throws PortalException {
		ExpandoValue secretExpandoValue = _expandoValueLocalService.getValue(
			user.getCompanyId(), User.class.getName(), "MP", "secret",
			user.getUserId());
		ExpandoValue tokenExpandoValue = _expandoValueLocalService.getValue(
			user.getCompanyId(), User.class.getName(), "MP", "token",
			user.getUserId());

		if ((secretExpandoValue == null) || (tokenExpandoValue == null)) {
			return null;
		}

		return new Token(
			tokenExpandoValue.getString(), secretExpandoValue.getString());
	}

	public OAuthService getOAuthService() {
		Api api = new MarketplaceApi();

		OAuthConfig oAuthConfig = new OAuthConfig(
			MarketplaceStoreWebConfigurationValues.MARKETPLACE_KEY,
			MarketplaceStoreWebConfigurationValues.MARKETPLACE_SECRET,
			MarketplaceStoreWebConfigurationValues.MARKETPLACE_URL,
			SignatureType.Header, null, null);

		return api.createService(oAuthConfig);
	}

	public void updateAccessToken(User user, Token token)
		throws PortalException {

		_updateAccessToken(user, token);
	}

	@Activate
	protected void activate() {
		List<Company> companys = _companyLocalService.getCompanies();

		for (Company company : companys) {
			try {
				_setupExpando(company.getCompanyId());
			}
			catch (Exception e) {
				if (_log.isWarnEnabled()) {
					_log.warn(
						"Unable to setup Marketplace for company " +
							company.getCompanyId() + ": " + e.getMessage());
				}
			}
		}
	}

	@Reference(unbind = "-")
	protected void setCompanyLocalService(
		CompanyLocalService companyLocalService) {

		_companyLocalService = companyLocalService;
	}

	@Reference(unbind = "-")
	protected void setExpandoColumnLocalService(
		ExpandoColumnLocalService expandoColumnLocalService) {

		_expandoColumnLocalService = expandoColumnLocalService;
	}

	@Reference(unbind = "-")
	protected void setExpandoTableLocalService(
		ExpandoTableLocalService expandoTableLocalService) {

		_expandoTableLocalService = expandoTableLocalService;
	}

	@Reference(unbind = "-")
	protected void setExpandoValueLocalService(
		ExpandoValueLocalService expandoValueLocalService) {

		_expandoValueLocalService = expandoValueLocalService;
	}

	@Reference(
		target = ModuleServiceLifecycle.PORTAL_INITIALIZED, unbind = "-"
	)
	protected void setModuleServiceLifecycle(
		ModuleServiceLifecycle moduleServiceLifecycle) {
	}

	private void _setupExpando(long companyId) throws Exception {
		ExpandoTable table = null;

		try {
			table = _expandoTableLocalService.addTable(
				companyId, User.class.getName(), "MP");
		}
		catch (DuplicateTableNameException dtne) {
			table = _expandoTableLocalService.getTable(
				companyId, User.class.getName(), "MP");
		}

		try {
			_expandoColumnLocalService.addColumn(
				table.getTableId(), "secret", ExpandoColumnConstants.STRING);
			_expandoColumnLocalService.addColumn(
				table.getTableId(), "token", ExpandoColumnConstants.STRING);
		}
		catch (DuplicateColumnNameException dcne) {
		}
	}

	private void _updateAccessToken(User user, Token token)
		throws PortalException {

		_expandoValueLocalService.addValue(
			user.getCompanyId(), User.class.getName(), "MP", "secret",
			user.getUserId(), token.getSecret());
		_expandoValueLocalService.addValue(
			user.getCompanyId(), User.class.getName(), "MP", "token",
			user.getUserId(), token.getToken());
	}

	private static final Log _log = LogFactoryUtil.getLog(OAuthManager.class);

	private CompanyLocalService _companyLocalService;
	private ExpandoColumnLocalService _expandoColumnLocalService;
	private ExpandoTableLocalService _expandoTableLocalService;
	private ExpandoValueLocalService _expandoValueLocalService;

}