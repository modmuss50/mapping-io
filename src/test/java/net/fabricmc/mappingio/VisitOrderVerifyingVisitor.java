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

package net.fabricmc.mappingio;

import java.io.IOException;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.mappingio.adapter.ForwardingMappingVisitor;

/**
 * Visitor which verifies on each visit call that the invoked visits were in accordance
 * with the expected order of visitation, as defined in {@link MappingVisitor}'s Javadocs.
 */
public class VisitOrderVerifyingVisitor extends ForwardingMappingVisitor {
	public VisitOrderVerifyingVisitor(MappingVisitor next) {
		super(next);
		init();
	}

	private void init() {
		visitedHeader = false;
		visitedNamespaces = false;
		visitedMetadata = false;
		visitedContent = false;
		visitedClass = false;
		visitedField = false;
		visitedMethod = false;
		visitedMethodArg = false;
		visitedMethodVar = false;
		resetVisitedElementContentUpTo(0);
		visitedEnd = false;
		lastVisitedElement = null;
	}

	private void resetVisitedElementContentUpTo(int inclusiveLevel) {
		for (int i = visitedElementContent.length - 1; i >= inclusiveLevel; i--) {
			visitedElementContent[i] = false;
		}
	}

	@Override
	public boolean visitHeader() throws IOException {
		assertHeaderNotVisited();

		visitedEnd = false;
		visitedHeader = true;
		return super.visitHeader();
	}

	@Override
	public void visitNamespaces(String srcNamespace, List<String> dstNamespaces) throws IOException {
		assertHeaderVisited();
		assertNamespacesNotVisited();

		visitedNamespaces = true;
		super.visitNamespaces(srcNamespace, dstNamespaces);
	}

	@Override
	public void visitMetadata(String key, @Nullable String value) throws IOException {
		assertNamespacesVisited();

		visitedMetadata = true;
		visitedClass = false;
		visitedField = false;
		visitedMethod = false;
		visitedMethodArg = false;
		visitedMethodVar = false;
		super.visitMetadata(key, value);
	}

	@Override
	public boolean visitContent() throws IOException {
		assertNamespacesVisited();
		assertContentNotVisited();

		visitedContent = true;
		return super.visitContent();
	}

	@Override
	public boolean visitClass(String srcName) throws IOException {
		MappedElementKind elementKind = MappedElementKind.CLASS;

		assertContentVisited();

		visitedClass = true;
		visitedField = false;
		visitedMethod = false;
		visitedMethodArg = false;
		visitedMethodVar = false;
		lastVisitedElement = elementKind;
		resetVisitedElementContentUpTo(elementKind.level);

		return super.visitClass(srcName);
	}

	@Override
	public boolean visitField(String srcName, @Nullable String srcDesc) throws IOException {
		MappedElementKind elementKind = MappedElementKind.FIELD;

		assertClassVisited();
		assertElementContentVisited(elementKind.level - 1);

		visitedField = true;
		visitedMethod = false;
		visitedMethodArg = false;
		visitedMethodVar = false;
		lastVisitedElement = elementKind;
		resetVisitedElementContentUpTo(elementKind.level);

		return super.visitField(srcName, srcDesc);
	}

	@Override
	public boolean visitMethod(String srcName, @Nullable String srcDesc) throws IOException {
		MappedElementKind elementKind = MappedElementKind.METHOD;

		assertClassVisited();
		assertElementContentVisited(elementKind.level - 1);

		if (!visitedMethod) {
			assertMethodArgNotVisited();
			assertMethodVarNotVisited();
		}

		visitedField = false;
		visitedMethod = true;
		visitedMethodArg = false;
		visitedMethodVar = false;
		lastVisitedElement = elementKind;
		resetVisitedElementContentUpTo(elementKind.level);

		return super.visitMethod(srcName, srcDesc);
	}

	@Override
	public boolean visitMethodArg(int argPosition, int lvIndex, @Nullable String srcName) throws IOException {
		MappedElementKind elementKind = MappedElementKind.METHOD_ARG;

		assertFieldNotVisited();
		assertMethodVisited();
		assertElementContentVisited(elementKind.level - 1);

		visitedMethodArg = true;
		visitedMethodVar = false;
		lastVisitedElement = elementKind;
		resetVisitedElementContentUpTo(elementKind.level);

		return super.visitMethodArg(argPosition, lvIndex, srcName);
	}

	@Override
	public boolean visitMethodVar(int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, @Nullable String srcName) throws IOException {
		MappedElementKind elementKind = MappedElementKind.METHOD_VAR;

		assertFieldNotVisited();
		assertMethodVisited();
		assertElementContentVisited(elementKind.level - 1);

		visitedMethodArg = false;
		visitedMethodVar = true;
		lastVisitedElement = elementKind;
		resetVisitedElementContentUpTo(elementKind.level);

		return super.visitMethodVar(lvtRowIndex, lvIndex, startOpIdx, endOpIdx, srcName);
	}

	@Override
	public void visitDstName(MappedElementKind targetKind, int namespace, String name) throws IOException {
		assertElementVisited(targetKind);
		assertElementContentNotVisitedUpTo(targetKind.level + 1); // prevent visitation after visitElementContent of same level

		super.visitDstName(targetKind, namespace, name);
	}

	@Override
	public void visitDstDesc(MappedElementKind targetKind, int namespace, String desc) throws IOException {
		assertElementVisited(targetKind);
		assertElementContentNotVisitedUpTo(targetKind.level + 1);

		super.visitDstDesc(targetKind, namespace, desc);
	}

	@Override
	public boolean visitElementContent(MappedElementKind targetKind) throws IOException {
		assertElementVisited(targetKind);
		assertElementContentNotVisitedUpTo(targetKind.level); // no +1 to prevent repeated visitation

		if (targetKind.level > 0) {
			assertElementContentVisited(targetKind.level - 1);
		}

		visitedElementContent[targetKind.level] = true;

		if (targetKind.level < visitedElementContent.length - 1) {
			resetVisitedElementContentUpTo(targetKind.level + 1);
		}

		return super.visitElementContent(targetKind);
	}

	@Override
	public void visitComment(MappedElementKind targetKind, String comment) throws IOException {
		assertElementVisited(targetKind);
		assertElementContentVisited(targetKind.level);
		assertElementContentNotVisitedUpTo(targetKind.level + 1);

		super.visitComment(targetKind, comment);
	}

	@Override
	public boolean visitEnd() throws IOException {
		assertContentVisited();
		assertEndNotVisited();

		init();
		visitedEnd = true;
		return super.visitEnd();
	}

	private void assertHeaderNotVisited() {
		if (visitedHeader) {
			throw new IllegalStateException("Header already visited");
		}
	}

	private void assertHeaderVisited() {
		if (!visitedHeader) {
			throw new IllegalStateException("Header not visited");
		}
	}

	private void assertNamespacesNotVisited() {
		if (visitedNamespaces) {
			throw new IllegalStateException("Namespaces already visited");
		}
	}

	private void assertNamespacesVisited() {
		if (!visitedNamespaces) {
			throw new IllegalStateException("Namespaces not visited");
		}
	}

	private void assertMetadataNotVisited() {
		if (visitedMetadata) {
			throw new IllegalStateException("Metadata already visited");
		}
	}

	private void assertMetadataVisited() {
		if (!visitedMetadata) {
			throw new IllegalStateException("Metadata not visited");
		}
	}

	private void assertContentNotVisited() {
		if (visitedContent) {
			throw new IllegalStateException("Content already visited");
		}
	}

	private void assertContentVisited() {
		if (!visitedContent) {
			throw new IllegalStateException("Content not visited");
		}
	}

	private void assertElementVisited(MappedElementKind kind) {
		if (lastVisitedElement != kind) {
			throw new IllegalStateException("Element not visited");
		}
	}

	private void assertClassNotVisited() {
		if (visitedClass) {
			throw new IllegalStateException("Class already visited");
		}
	}

	private void assertClassVisited() {
		if (!visitedClass) {
			throw new IllegalStateException("Class not visited");
		}
	}

	private void assertFieldNotVisited() {
		if (visitedField) {
			throw new IllegalStateException("Field already visited");
		}
	}

	private void assertFieldVisited() {
		if (!visitedField) {
			throw new IllegalStateException("Field not visited");
		}
	}

	private void assertMethodNotVisited() {
		if (visitedMethod) {
			throw new IllegalStateException("Method already visited");
		}
	}

	private void assertMethodVisited() {
		if (!visitedMethod) {
			throw new IllegalStateException("Method not visited");
		}
	}

	private void assertMethodArgNotVisited() {
		if (visitedMethodArg) {
			throw new IllegalStateException("Method argument already visited");
		}
	}

	private void assertMethodArgVisited() {
		if (!visitedMethodArg) {
			throw new IllegalStateException("Method argument not visited");
		}
	}

	private void assertMethodVarNotVisited() {
		if (visitedMethodVar) {
			throw new IllegalStateException("Method variable already visited");
		}
	}

	private void assertMethodVarVisited() {
		if (!visitedMethodVar) {
			throw new IllegalStateException("Method variable not visited");
		}
	}

	private void assertElementContentNotVisitedUpTo(int inclusiveLevel) {
		for (int i = visitedElementContent.length - 1; i >= inclusiveLevel; i--) {
			if (visitedElementContent[i]) {
				throw new IllegalStateException("Element content already visited");
			}
		}
	}

	private void assertElementContentVisited(int depth) {
		if (!visitedElementContent[depth]) {
			throw new IllegalStateException("Element content not visited");
		}
	}

	private void assertEndNotVisited() {
		if (visitedEnd) {
			throw new IllegalStateException("End already visited");
		}
	}

	boolean visitedHeader;
	boolean visitedNamespaces;
	boolean visitedMetadata;
	boolean visitedContent;
	boolean visitedClass;
	boolean visitedField;
	boolean visitedMethod;
	boolean visitedMethodArg;
	boolean visitedMethodVar;
	boolean[] visitedElementContent = new boolean[3];
	boolean visitedEnd;
	MappedElementKind lastVisitedElement;
}
