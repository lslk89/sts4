/*******************************************************************************
 * Copyright (c) 2016 Pivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.springframework.ide.vscode.commons.languageserver.java;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import org.springframework.ide.vscode.commons.languageserver.util.IDocument;
import org.springframework.ide.vscode.commons.maven.MavenCore;
import org.springframework.ide.vscode.commons.maven.java.classpathfile.JavaProjectWithClasspathFile;
import org.springframework.ide.vscode.commons.util.FileUtils;
import org.springframework.ide.vscode.commons.util.StringUtil;

public class JavaProjectWithClasspathFileFinderStrategy implements IJavaProjectFinderStrategy {

	@Override
	public JavaProjectWithClasspathFile find(IDocument d) {
		String uriStr = d.getUri();
		if (StringUtil.hasText(uriStr)) {
			try {
				URI uri = new URI(uriStr);
				//TODO: This only work with File uri. Should it work with others too?
				File file = new File(uri).getAbsoluteFile();
				File cpFile = FileUtils.findFile(file, MavenCore.CLASSPATH_TXT);
				if (cpFile!=null) {
					return new JavaProjectWithClasspathFile(cpFile);
				}
			} catch (URISyntaxException | IllegalArgumentException e) {
				//garbage data. Ignore it.
			}
		}
		return null;
	}

}