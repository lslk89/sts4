/*******************************************************************************
 * Copyright (c) 2017 Pivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.springframework.ide.vscode.commons.cloudfoundry.client.cftarget;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.ide.vscode.commons.cloudfoundry.client.ClientTimeouts;
import org.springframework.ide.vscode.commons.cloudfoundry.client.CloudFoundryClientFactory;
import org.springframework.ide.vscode.commons.util.Assert;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public class CFTargetCache {

	private final CfClientConfig cfClientConfig;
	private final CloudFoundryClientFactory clientFactory;
	private final ClientTimeouts timeouts;
	private LoadingCache<ClientParamsCacheKey, CFTarget> cache;
	private CFCallableContext cacheCallableContext;

	public static final Duration SERVICES_EXPIRATION = Duration.ofSeconds(10);
	public static final Duration TARGET_EXPIRATION = Duration.ofHours(1);
	public static final Duration ERROR_EXPIRATION = Duration.ofSeconds(10);

	public CFTargetCache(CfClientConfig cfClientConfig, CloudFoundryClientFactory clientFactory,
			ClientTimeouts timeouts) {
		Assert.isLegal(cfClientConfig != null,
				"A Cloud Foundry client parameters provider must be set when creating a target cache.");
		this.cfClientConfig = cfClientConfig;
		this.clientFactory = clientFactory;
		this.timeouts = timeouts;
		//TODO: I suspect that addClientParamsProviderChangedListener below is not necessary. 
		// I think it results in unnessary refreshes of the cache, any time the providers are
		// changed. The cached results doesn't really depend on the providers, only on the targets. So I think, 
		// it shouldn't need to refresh when the providers are changed.
		cfClientConfig.addClientParamsProviderChangedListener((newProvider, oldProvider) -> initCache());
		initCache();
	}
	
	private void initCache() {
		CacheLoader<ClientParamsCacheKey, CFTarget> loader = new CacheLoader<ClientParamsCacheKey, CFTarget>() {

			@Override
			public CFTarget load(ClientParamsCacheKey params) throws Exception {
				return create(params.fullParams);
			}

		};
		cache = CacheBuilder.newBuilder()./*maximumSize(1).*/expireAfterAccess(TARGET_EXPIRATION.toMillis(), TimeUnit.MILLISECONDS)
				.build(loader);
		this.cacheCallableContext = new CFCallableContext(cfClientConfig.getClientParamsProvider().getMessages());
	}

	/**
	 * @return non-null list of targets, or throws exception if no targets found
	 * @throws NoTargetsException
	 *             if no targets found
	 * @throws Exception
	 *             for any other error encountered
	 */
	public synchronized List<CFTarget> getOrCreate() throws NoTargetsException, Exception {
		return cacheCallableContext.checkConnection(() -> doGetOrCreate());
	}

	protected synchronized List<CFTarget> doGetOrCreate() throws NoTargetsException, Exception {

		Collection<CFClientParams> allParams = cfClientConfig.getClientParamsProvider().getParams();
		List<CFTarget> targets = new ArrayList<>();
		if (allParams != null) {
			for (CFClientParams params : allParams) {
				ClientParamsCacheKey key = ClientParamsCacheKey.from(params);
				CFTarget target = cache.get(key);
				if (target != null) {
					// If any CF errors occurred in the target, refresh once
					if (target.hasExpiredConnectionError()) {
						cache.refresh(key);
						target = cache.get(key);
					}
					targets.add(target);
				}
			}
		}
		
		return targets;
	}
	
	protected CFTarget create(CFClientParams params) throws Exception {
		/*
		 * Must pass a NEW callable context. Cannot be
		 * the same as the target cache callable context, as
		 * contexts may contain error state
		 */
		return new CFTarget(getTargetName(params), params, clientFactory.getClient(params, timeouts),
				new CFCallableContext(cfClientConfig.getClientParamsProvider().getMessages()));
	}

	protected static String getTargetName(CFClientParams params) {
		return labelFromCfApi(params.getApiUrl());
	}

	protected static String labelFromCfApi(String cfApiUrl) {
		if (cfApiUrl.startsWith("https://")) {
			return cfApiUrl.substring("https://".length());
		} else if (cfApiUrl.startsWith("http://")) {
			return cfApiUrl.substring("http://".length());
		} else {
			return cfApiUrl;
		}
	}

	public CfClientConfig getCfClientConfig() {
		return cfClientConfig;
	}
}
