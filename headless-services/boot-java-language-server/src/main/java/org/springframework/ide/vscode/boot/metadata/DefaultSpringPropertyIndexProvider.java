/*******************************************************************************
 * Copyright (c) 2016, 2017 Pivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal, Inc. - initial API and implementation
 *******************************************************************************/

package org.springframework.ide.vscode.boot.metadata;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.ide.vscode.commons.java.IJavaProject;
import org.springframework.ide.vscode.commons.languageserver.ProgressService;
import org.springframework.ide.vscode.commons.languageserver.java.JavaProjectManager;
import org.springframework.ide.vscode.commons.util.FuzzyMap;
import org.springframework.ide.vscode.commons.util.text.IDocument;

public class DefaultSpringPropertyIndexProvider implements SpringPropertyIndexProvider {

	private static final FuzzyMap<ConfigurationMetadataProperty> EMPTY_INDEX = new SpringPropertyIndex(null);

	private JavaProjectManager javaProjectManager;
	private SpringPropertiesIndexManager indexManager = new SpringPropertiesIndexManager();

	private ProgressService progressService = (id, msg) -> {
		/* ignore */ };

	public DefaultSpringPropertyIndexProvider(JavaProjectManager javaProjectManager) {
		this.javaProjectManager = javaProjectManager;
	}

	@Override
	public FuzzyMap<ConfigurationMetadataProperty> getIndex(IDocument doc) {
		IJavaProject jp = javaProjectManager.find(doc);
		if (jp != null) {
			return indexManager.get(jp, progressService);
		}
		return EMPTY_INDEX;
	}

	public void setProgressService(ProgressService progressService) {
		this.progressService = progressService;
	}

}
