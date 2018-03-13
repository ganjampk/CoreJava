package com.fisglobal.scheduler;

import java.util.Hashtable;
import java.util.List;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import com.fis.pji.core.ErrorException;
import com.fis.pji.core.ProfileSession;
import com.fis.pji.generated.func.impl.PrfNotifications;
import com.fisglobal.base.Application;
import com.fisglobal.base.ApplicationResources;
import com.fisglobal.base.Logger;
import com.fisglobal.base.SanchezException;
import com.fisglobal.host.ProfileSQL;
import com.fisglobal.security.filter.UserSessionValidatorFilter;
import com.fisglobal.utils.Utility;

public class FetchCustomerStatusScheduler {
	private static final String channelId = "userSecurityAlert";

	@SuppressWarnings("unchecked")
	public static void fetchCustomerStatus() throws Exception {
		final CacheManager cacheManager = UserSessionValidatorFilter.getCacheManager();
		if (cacheManager != null) {
			final Cache cache = cacheManager.getCache(ApplicationResources.USER_AUTHENTICATION_CACHE);
			if (!cache.getKeys().isEmpty()) {
				final ProfileSession profileSession = Application.getProfileSession();
				final Hashtable<String, Object> returnParam = new Hashtable<String, Object>();
				try {
					final List<String> reauthenticatedUsersList = PrfNotifications.getAuthenticateAlerts(channelId, false, returnParam, profileSession.getContext());
					for (final String reauthenticatedUser : Utility.protectNull(reauthenticatedUsersList)) {
						final String reauthenticateHashedUser = Utility.hash(reauthenticatedUser, ApplicationResources.SECURE_HASH_ALGORITHM_256);
						for (final Object hashedUserNameInCache : cache.getKeys()) {
							if (reauthenticateHashedUser.equals(hashedUserNameInCache.toString())) {
								Logger.info("reauthenticatedUser from PJI :" + reauthenticatedUser);
								final Element element = cache.get(reauthenticateHashedUser);
								Utility.putUserInCache(cache, element, null, reauthenticateHashedUser, false);
							}
						}
					}

				} catch (final ErrorException e) {
					Logger.error("exception", new String[] {e.getClass().getName(), "FetchCustomerStatusScheduler", "fetchCustomerStatus", e.getMessage()});
					throw ProfileSQL.toSanchezException(e);
				} catch (final Exception e) {
					Logger.warn("exception", new String[] {e.getClass().getName(), "FetchCustomerStatusScheduler", "fetchCustomerStatus", e.getMessage()});
					throw new SanchezException(e.getMessage());
				} finally {
					Application.closeProfileSession(profileSession);
				}
			}
		}
	}
}
