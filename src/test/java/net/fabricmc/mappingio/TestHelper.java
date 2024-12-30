/*
 * Copyright (c) 2023 FabricMC
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

package net.fabricmc.mappingio;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.neoforged.srgutils.IMappingFile;
import org.cadixdev.lorenz.io.MappingFormats;
import org.jetbrains.annotations.Nullable;

import net.fabricmc.mappingio.adapter.ForwardingMappingVisitor;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.format.intellij.MigrationMapConstants;
import net.fabricmc.mappingio.lib.jool.Unchecked;
import net.fabricmc.mappingio.tree.MappingTreeView;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

public final class TestHelper {
	public static Path getResource(String slashPrefixedResourcePath) {
		try {
			return Paths.get(TestHelper.class.getResource(slashPrefixedResourcePath).toURI());
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	@Nullable
	public static String getFileName(MappingFormat format) {
		switch (format) {
		case ENIGMA_FILE:
			return "enigma.mappings";
		case ENIGMA_DIR:
			return "enigma-dir";
		case TINY_FILE:
			return "tiny.tiny";
		case TINY_2_FILE:
			return "tinyV2.tiny";
		case SRG_FILE:
			return "srg.srg";
		case XSRG_FILE:
			return "xsrg.xsrg";
		case JAM_FILE:
			return "jam.jam";
		case CSRG_FILE:
			return "csrg.csrg";
		case TSRG_FILE:
			return "tsrg.tsrg";
		case TSRG_2_FILE:
			return "tsrgV2.tsrg";
		case PROGUARD_FILE:
			return "proguard.txt";
		case INTELLIJ_MIGRATION_MAP_FILE:
			return "migration-map.xml";
		case RECAF_SIMPLE_FILE:
			return "recaf-simple.txt";
		case JOBF_FILE:
			return "jobf.jobf";
		default:
			return null;
		}
	}

	@Nullable
	public static org.cadixdev.lorenz.io.MappingFormat toLorenzFormat(MappingFormat format) {
		switch (format) {
		case SRG_FILE:
			return MappingFormats.SRG;
		case XSRG_FILE:
			return MappingFormats.XSRG;
		case CSRG_FILE:
			return MappingFormats.CSRG;
		case TSRG_FILE:
			return MappingFormats.TSRG;
		case ENIGMA_FILE:
			return MappingFormats.byId("enigma");
		case JAM_FILE:
			return MappingFormats.byId("jam");
		case TINY_FILE:
		case TINY_2_FILE:
		case ENIGMA_DIR:
		case TSRG_2_FILE:
		case PROGUARD_FILE:
		case INTELLIJ_MIGRATION_MAP_FILE:
		case RECAF_SIMPLE_FILE:
		case JOBF_FILE:
			return null;
		default:
			throw new IllegalArgumentException("Unknown format: " + format);
		}
	}

	@Nullable
	public static IMappingFile.Format toSrgUtilsFormat(MappingFormat format) {
		switch (format) {
		case TINY_FILE:
			return IMappingFile.Format.TINY1;
		case TINY_2_FILE:
			return IMappingFile.Format.TINY;
		case SRG_FILE:
			return IMappingFile.Format.SRG;
		case XSRG_FILE:
			return IMappingFile.Format.XSRG;
		case CSRG_FILE:
			return IMappingFile.Format.CSRG;
		case TSRG_FILE:
			return IMappingFile.Format.TSRG;
		case TSRG_2_FILE:
			return IMappingFile.Format.TSRG2;
		case PROGUARD_FILE:
			return IMappingFile.Format.PG;
		case ENIGMA_FILE:
		case ENIGMA_DIR:
		case JAM_FILE:
		case INTELLIJ_MIGRATION_MAP_FILE:
		case RECAF_SIMPLE_FILE:
		case JOBF_FILE:
			return null;
		default:
			throw new IllegalArgumentException("Unknown format: " + format);
		}
	}

	public static Path writeToDir(MappingTreeView tree, Path dir, MappingFormat format) throws IOException {
		Path path = dir.resolve(getFileName(format));
		tree.accept(MappingWriter.create(path, format));
		return path;
	}

	// After any changes, run "./gradlew updateTestMappings" to update the mapping files in the resources folder accordingly
	public static <T extends MappingVisitor> T acceptTestMappings(T target) throws IOException {
		MappingVisitor delegate = target instanceof VisitOrderVerifyingVisitor ? target : new VisitOrderVerifyingVisitor(target);

		if (delegate.visitHeader()) {
			delegate.visitNamespaces(MappingUtil.NS_SOURCE_FALLBACK, Arrays.asList(MappingUtil.NS_TARGET_FALLBACK, MappingUtil.NS_TARGET_FALLBACK + "2"));
			delegate.visitMetadata("name", "valid");
			delegate.visitMetadata(MigrationMapConstants.ORDER_KEY, "0");
		}

		if (delegate.visitContent()) {
			int[] dstNs = new int[] { 0, 1 };
			NameGen nameGen = new NameGen();

			if (nameGen.visitClass(delegate, dstNs)) {
				nameGen.visitField(delegate, dstNs);

				if (nameGen.visitMethod(delegate, dstNs)) {
					nameGen.visitMethodArg(delegate, dstNs);
					nameGen.visitMethodVar(delegate, dstNs);
				}
			}

			if (nameGen.visitInnerClass(delegate, 1, dstNs)) {
				nameGen.visitComment(delegate);
				nameGen.visitField(delegate, dstNs);
			}

			nameGen.visitClass(delegate, dstNs);
		}

		if (!delegate.visitEnd()) {
			acceptTestMappings(delegate);
		}

		return target;
	}

	// After any changes, run "./gradlew updateTestMappings" to update the mapping files in the resources folder accordingly.
	// Make sure to keep the few manual changes in the files.
	public static <T extends MappingVisitor> T acceptTestMappingsWithRepeats(T target, boolean repeatComments, boolean repeatClasses) throws IOException {
		acceptTestMappings(new ForwardingMappingVisitor(new VisitOrderVerifyingVisitor(target, true)) {
			private List<Runnable> replayQueue = new ArrayList<>();

			@Override
			public void visitMetadata(String key, @Nullable String value) throws IOException {
				super.visitMetadata(key, key.equals("name") ? "repeated-elements" : value);
			}

			@Override
			public boolean visitClass(String srcName) throws IOException {
				replayQueue.clear();

				if (repeatClasses) {
					replayQueue.add(Unchecked.runnable(() -> super.visitClass(srcName)));
				}

				return super.visitClass(srcName);
			}

			@Override
			public boolean visitField(String srcName, @Nullable String srcDesc) throws IOException {
				replayQueue.clear();
				replayQueue.add(Unchecked.runnable(() -> super.visitField(srcName, srcDesc)));
				return super.visitField(srcName, srcDesc);
			}

			@Override
			public boolean visitMethod(String srcName, @Nullable String srcDesc) throws IOException {
				replayQueue.clear();
				replayQueue.add(Unchecked.runnable(() -> super.visitMethod(srcName, srcDesc)));
				return super.visitMethod(srcName, srcDesc);
			}

			@Override
			public boolean visitMethodArg(int lvIndex, int argIndex, String srcName) throws IOException {
				replayQueue.clear();
				replayQueue.add(Unchecked.runnable(() -> super.visitMethodArg(lvIndex, argIndex, srcName)));
				return super.visitMethodArg(lvIndex, argIndex, srcName);
			}

			@Override
			public boolean visitMethodVar(int lvIndex, int varIndex, int startOpIdx, int endOpIdx, String srcName) throws IOException {
				replayQueue.clear();
				replayQueue.add(Unchecked.runnable(() -> super.visitMethodVar(lvIndex, varIndex, startOpIdx, endOpIdx, srcName)));
				return super.visitMethodVar(lvIndex, varIndex, startOpIdx, endOpIdx, srcName);
			}

			@Override
			public void visitDstName(MappedElementKind targetKind, int namespace, String name) throws IOException {
				if (targetKind == MappedElementKind.CLASS && !repeatClasses) {
					super.visitDstName(targetKind, namespace, name);
				} else {
					replayQueue.add(Unchecked.runnable(() -> super.visitDstName(targetKind, namespace, name)));
					super.visitDstName(targetKind, namespace, name + "0");
				}
			}

			@Override
			public void visitDstDesc(MappedElementKind targetKind, int namespace, String desc) throws IOException {
				replayQueue.add(Unchecked.runnable(() -> super.visitDstDesc(targetKind, namespace, desc)));
				super.visitDstDesc(targetKind, namespace, desc);
			}

			@Override
			public boolean visitElementContent(MappedElementKind targetKind) throws IOException {
				boolean ret = super.visitElementContent(targetKind);

				if (!replayQueue.isEmpty()) {
					replayQueue.forEach(Runnable::run);

					ret = super.visitElementContent(targetKind);
				}

				return ret;
			}

			@Override
			public void visitComment(MappedElementKind targetKind, String comment) throws IOException {
				if (repeatComments) {
					super.visitComment(targetKind, comment + ".");
				}

				super.visitComment(targetKind, comment);
			}
		});

		return target;
	}

	// After any changes, run "./gradlew updateTestMappings" to update the mapping files in the resources folder accordingly
	public static <T extends MappingVisitor> T acceptTestMappingsWithHoles(T target) throws IOException {
		MappingVisitor delegate = target instanceof VisitOrderVerifyingVisitor ? target : new VisitOrderVerifyingVisitor(target);

		if (delegate.visitHeader()) {
			delegate.visitNamespaces(MappingUtil.NS_SOURCE_FALLBACK, Arrays.asList(MappingUtil.NS_TARGET_FALLBACK, MappingUtil.NS_TARGET_FALLBACK + "2"));
		}

		if (delegate.visitContent()) {
			NameGen nameGen = new NameGen();

			// (Inner) Classes
			for (int nestLevel = 0; nestLevel <= 2; nestLevel++) {
				nameGen.visitClass(delegate);
				nameGen.visitClass(delegate);
				nameGen.visitInnerClass(delegate, nestLevel, 0);
				nameGen.visitInnerClass(delegate, nestLevel, 1);

				if (nameGen.visitInnerClass(delegate, nestLevel)) {
					nameGen.visitComment(delegate);
				}

				if (nameGen.visitInnerClass(delegate, nestLevel, 0)) {
					nameGen.visitComment(delegate);
				}

				if (nameGen.visitInnerClass(delegate, nestLevel, 1)) {
					nameGen.visitComment(delegate);
				}
			}

			if (nameGen.visitClass(delegate)) {
				// Fields
				nameGen.visitField(delegate);
				nameGen.visitField(delegate, 0);
				nameGen.visitField(delegate, 1);

				if (nameGen.visitField(delegate)) {
					nameGen.visitComment(delegate);
				}

				if (nameGen.visitField(delegate, 0)) {
					nameGen.visitComment(delegate);
				}

				if (nameGen.visitField(delegate, 1)) {
					nameGen.visitComment(delegate);
				}

				// Methods
				nameGen.visitMethod(delegate);
				nameGen.visitMethod(delegate, 0);
				nameGen.visitMethod(delegate, 1);

				if (nameGen.visitMethod(delegate)) {
					nameGen.visitComment(delegate);
				}

				if (nameGen.visitMethod(delegate, 0)) {
					nameGen.visitComment(delegate);
				}

				if (nameGen.visitMethod(delegate, 1)) {
					nameGen.visitComment(delegate);
				}

				// Method args
				if (nameGen.visitMethod(delegate)) {
					nameGen.visitMethodArg(delegate);
					nameGen.visitMethodArg(delegate, 1);
					nameGen.visitMethodArg(delegate, 0);

					if (nameGen.visitMethodArg(delegate)) {
						nameGen.visitComment(delegate);
					}

					if (nameGen.visitMethodArg(delegate, 0)) {
						nameGen.visitComment(delegate);
					}

					if (nameGen.visitMethodArg(delegate, 1)) {
						nameGen.visitComment(delegate);
					}
				}

				// Method vars
				if (nameGen.visitMethod(delegate)) {
					nameGen.visitMethodVar(delegate);
					nameGen.visitMethodVar(delegate, 1);
					nameGen.visitMethodVar(delegate, 0);

					if (nameGen.visitMethodVar(delegate)) {
						nameGen.visitComment(delegate);
					}

					if (nameGen.visitMethodVar(delegate, 0)) {
						nameGen.visitComment(delegate);
					}

					if (nameGen.visitMethodVar(delegate, 1)) {
						nameGen.visitComment(delegate);
					}
				}
			}
		}

		if (!delegate.visitEnd()) {
			acceptTestMappingsWithHoles(delegate);
		}

		return target;
	}

	public static class MappingDirs {
		@Nullable
		public static MemoryMappingTree getCorrespondingTree(Path dir) throws IOException {
			if (dir.equals(VALID)) return acceptTestMappings(new MemoryMappingTree());
			if (dir.equals(REPEATED_ELEMENTS)) return acceptTestMappingsWithRepeats(new MemoryMappingTree(), true, true);
			if (dir.equals(VALID_WITH_HOLES)) return acceptTestMappingsWithHoles(new MemoryMappingTree());
			return null;
		}

		public static final Path DETECTION = getResource("/detection/");
		public static final Path VALID = getResource("/read/valid/");
		public static final Path REPEATED_ELEMENTS = getResource("/read/repeated-elements/");
		public static final Path VALID_WITH_HOLES = getResource("/read/valid-with-holes/");
		public static final Path MERGING = getResource("/merging/");
	}
}
