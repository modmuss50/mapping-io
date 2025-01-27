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

import java.io.IOException;
import java.io.StringReader;

import org.teavm.jso.JSExport;

import net.fabricmc.mappingio.format.tiny.Tiny2FileReader;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

public class MappingIoWasm {
	@JSExport
	public static WasmMappingView readTinyV2(String mappings) throws IOException {
		MemoryMappingTree tree = new MemoryMappingTree();

		try (StringReader reader = new StringReader(mappings)) {
			Tiny2FileReader.read(reader, tree);
		}

		return new WasmMappingView(tree);
	}
}
