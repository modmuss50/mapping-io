/*
 * Copyright (c) 2025 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.fabricmc.mappingio.wasm;

import org.jetbrains.annotations.Nullable;
import org.teavm.jso.JSExport;

import net.fabricmc.mappingio.tree.MappingTreeView;

public class WasmMappingView {
	private final MappingTreeView view;

	WasmMappingView(MappingTreeView view) {
		this.view = view;
	}

	@JSExport
	public String getSrcNamespace() {
		return view.getSrcNamespace();
	}

	@JSExport
	public String[] getDstNamespaces() {
		return view.getDstNamespaces().toArray(String[]::new);
	}

	@JSExport
	public int getNamespaceId(String namespace) {
		return view.getNamespaceId(namespace);
	}

	@JSExport
	public WasmClassView[] getClasses() {
		return view.getClasses().stream().map(WasmClassView::new).toArray(WasmClassView[]::new);
	}

	@JSExport
	@Nullable
	public WasmClassView getClass(String srcName) {
		MappingTreeView.ClassMappingView clazz = view.getClass(srcName);
		if (clazz == null) return null;
		return new WasmClassView(clazz);
	}

	@JSExport
	@Nullable
	public WasmClassView getClass(String name, int namespace) {
		MappingTreeView.ClassMappingView clazz = view.getClass(name, namespace);
		if (clazz == null) return null;
		return new WasmClassView(clazz);
	}

	@JSExport
	@Nullable
	public WasmMethodView getMethod(String srcClsName, String srcName, @Nullable String srcDesc) {
		MappingTreeView.MethodMappingView method = view.getMethod(srcClsName, srcName, srcDesc);
		if (method == null) return null;
		return new WasmMethodView(method);
	}

	@JSExport
	@Nullable
	public WasmMethodView getMethod(String clsName, String name, @Nullable String desc, int namespace) {
		MappingTreeView.MethodMappingView method = view.getMethod(clsName, name, desc, namespace);
		if (method == null) return null;
		return new WasmMethodView(method);
	}

	@JSExport
	String mapClassName(String name, int namespace) {
		return view.mapClassName(name, namespace);
	}

	@JSExport
	String mapClassName(String name, int srcNamespace, int dstNamespace) {
		return view.mapClassName(name, srcNamespace, dstNamespace);
	}

	// TODO mapDesc

	public static class WasmClassView extends WasmElementView<MappingTreeView.ClassMappingView> {
		public WasmClassView(MappingTreeView.ClassMappingView view) {
			super(view);
		}
	}

	public static class WasmMethodView extends WasmElementView<MappingTreeView.MethodMappingView> {
		public WasmMethodView(MappingTreeView.MethodMappingView view) {
			super(view);
		}
	}

	public static class WasmElementView<T extends MappingTreeView.ElementMappingView> {
		protected final T view;

		public WasmElementView(T view) {
			this.view = view;
		}

		@JSExport
		public String getSrcName() {
			return view.getSrcName();
		}

		@JSExport
		public String getDstName(int namespaceIndex) {
			return view.getDstName(namespaceIndex);
		}

		@JSExport
		@Nullable
		String getComment() {
			return view.getComment();
		}
	}
}
