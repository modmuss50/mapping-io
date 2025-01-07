/*
 * Copyright (c) 2024 FabricMC
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

package net.fabricmc.mappingio.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;

import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.MappingWriter;
import net.fabricmc.mappingio.adapter.MappingNsCompleter;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.test.TestMappings.MappingDir;
import net.fabricmc.mappingio.test.lib.jool.Unchecked;

public class TestFileUpdater {
	public static void main(String[] args) throws IOException {
		if (args.length == 1) {
			TestUtil.setResourceRoot(mkDir(Paths.get(args[0])));
		}

		for (MappingDir dir : TestMappings.values()) {
			if (!dir.supportsGeneration()) {
				continue;
			}

			rmDir(dir.path());
			mkDir(dir.path());

			for (MappingFormat format : MappingFormat.values()) {
				if (!format.hasWriter) {
					continue;
				}

				MappingVisitor target = MappingWriter.create(dir.pathFor(format), format);

				if (dir == TestMappings.READING.REPEATED_ELEMENTS) {
					boolean isEnigma = format == MappingFormat.ENIGMA_FILE || format == MappingFormat.ENIGMA_DIR;

					TestMappings.generateRepeatedElements(target, !isEnigma, !isEnigma);
					continue;
				}

				if (dir.isIn(TestMappings.PROPAGATION.BASE_DIR) && !format.features().hasNamespaces()) {
					target = new MappingNsCompleter(target);
				}

				dir.generate(target);
			}
		}
	}

	private static Path mkDir(Path path) throws IOException {
		if (!Files.exists(path)) {
			Files.createDirectories(path);
		}

		return path;
	}

	private static void rmDir(Path path) throws IOException {
		try (Stream<Path> paths = Files.walk(path)) {
			paths.sorted(Comparator.reverseOrder()).forEach(Unchecked.consumer(Files::deleteIfExists));
		}
	}
}
