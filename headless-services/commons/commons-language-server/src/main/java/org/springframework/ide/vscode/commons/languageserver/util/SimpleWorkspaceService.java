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
package org.springframework.ide.vscode.commons.languageserver.util;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.WorkspaceSymbolParams;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.springframework.ide.vscode.commons.util.Assert;
import org.springframework.ide.vscode.commons.util.FileObserver;
import org.springframework.ide.vscode.commons.util.Log;

import com.google.common.collect.ImmutableList;

import reactor.core.publisher.Mono;

public class SimpleWorkspaceService implements WorkspaceService, FileObserver {

	private SimpleLanguageServer server;

	private ListenerList<Settings> configurationListeners = new ListenerList<>();
	private ExecuteCommandHandler executeCommandHandler;
	private WorkspaceSymbolHandler workspaceSymbolHandler;
	private org.springframework.ide.vscode.commons.util.ListenerList<FileListener> fileListeners = new org.springframework.ide.vscode.commons.util.ListenerList<>();

	public SimpleWorkspaceService(SimpleLanguageServer server) {
		this.server = server;
	}

	@Override
	public CompletableFuture<List<? extends SymbolInformation>> symbol(WorkspaceSymbolParams params) {
		WorkspaceSymbolHandler workspaceSymbolHandler = this.workspaceSymbolHandler;
		if (workspaceSymbolHandler==null) {
			return CompletableFuture.completedFuture(ImmutableList.of());
		}
		return Mono.fromCallable(() -> {
			server.waitForReconcile();
			return workspaceSymbolHandler.handle(params);
		})
		.toFuture()
		.thenApply(l -> (List<? extends SymbolInformation>)l);
	}

	@Override
	public void didChangeConfiguration(DidChangeConfigurationParams params) {
		configurationListeners.fire(new Settings(params.getSettings()));
	}

	@Override
	public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
		params.getChanges().forEach(event -> fileListeners.forEach(listener -> {
			try {
				String uri = event.getUri();
				if (uri != null) {
					if (listener.accept(event.getUri())) {
						switch (event.getType()) {
						case Created:
							listener.created(uri);
							break;
						case Changed:
							listener.changed(uri);
							break;
						case Deleted:
							listener.deleted(uri);
							break;
						default:
							Log.log("Uknown file change type '" + event.getType() + "' for file: " + uri);
							break;

						}
					}
				}
			} catch (Throwable t) {
				Log.log(t);
			}
		}));
	}

	@Override
	public CompletableFuture<Object> executeCommand(ExecuteCommandParams params) {
		if (this.executeCommandHandler!=null) {
			return this.executeCommandHandler.handle(params);
		}
		throw new UnsupportedOperationException();
	}

	public void onDidChangeConfiguraton(Consumer<Settings> l) {
		configurationListeners.add(l);
	}

	public void onExecuteCommand(ExecuteCommandHandler handler) {
		Assert.isNull("A executeCommandHandler is already set, multiple handlers not supported yet", this.executeCommandHandler);
		this.executeCommandHandler = handler;
	}

	public synchronized void onWorkspaceSymbol(WorkspaceSymbolHandler h) {
		Assert.isNull("A WorkspaceSymbolHandler is already set, multiple handlers not supported yet", workspaceSymbolHandler);
		this.workspaceSymbolHandler = h;
	}

	public boolean hasWorkspaceSymbolHandler() {
		return this.workspaceSymbolHandler != null;
	}

	@Override
	public void addListener(FileListener listener) {
		fileListeners.add(listener);
	}

	@Override
	public void removeListener(FileListener listener) {
		fileListeners.remove(listener);
	}

}
