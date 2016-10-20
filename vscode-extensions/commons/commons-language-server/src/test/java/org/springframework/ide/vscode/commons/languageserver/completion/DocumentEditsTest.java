/*******************************************************************************
 * Copyright (c) 2015 Pivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.springframework.ide.vscode.commons.languageserver.completion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.springframework.ide.vscode.commons.languageserver.completion.DocumentEdits;
import org.springframework.ide.vscode.commons.languageserver.util.IDocument;
import org.springframework.ide.vscode.commons.languageserver.util.IRegion;
import org.springframework.ide.vscode.commons.languageserver.util.TextDocument;
import org.springframework.ide.vscode.languageserver.testharness.Editor;
import org.springframework.ide.vscode.languageserver.testharness.LanguageServerHarness;
/**
 * @author Kris De Volder
 */
public class DocumentEditsTest {

	private LanguageServerHarness harness;

	@Before
	public void setup() throws Exception {
		harness = new LanguageServerHarness(null);
	}

	class TestSubject {
		private Editor editor;
		private DocumentEdits edits;
		private String orgText;

		public TestSubject(String contents) throws Exception {
			this.orgText = contents;
			reset();
		}

		public void reset() throws Exception {
			this.editor = harness.newEditor(orgText);
			this.edits = new DocumentEdits(getFreshDocument(editor));
		}

		private IDocument getFreshDocument(Editor editor) throws Exception {
			TextDocument doc = new TextDocument(null);
			doc.setText(editor.getRawText());
			return doc;
		}

		public void del(String snippet) {
			int start = orgText.indexOf(snippet);
			assertTrue(start>=0);
			int end = start + snippet.length();
			edits.delete(start, end);
		}

		public void expect(String expect) throws Exception {
			apply(editor, edits);
			assertEquals(expect, editor.getText());
		}

		private void apply(Editor editor, DocumentEdits edit) throws Exception {
			IDocument document = getFreshDocument(editor);
			edits.apply(document);
			editor.setRawText(document.get());
			IRegion sel = edit.getSelection();
			int selectionStart = sel.getOffset();
			int selectionEnd = selectionStart+sel.getLength();
			editor.setSelection(selectionStart, selectionEnd);
		}

		public void insBefore(String before, String insert) {
			int offset = orgText.indexOf(before);
			assertTrue(offset>=0);
			edits.insert(offset, insert);
		}

		public void delLineAt(String snippet) throws Exception {
			int offset = orgText.indexOf(snippet);
			assertTrue(offset>=0);
			edits.deleteLineBackwardAtOffset(offset);
		}

		public void delLine(int i) throws Exception {
			edits.deleteLineBackward(0);
		}

	}

	@Test public void testDeletes() throws Exception {
		TestSubject it;

		it = new TestSubject("0123456789<*>");
		it.del("123");
		it.del("567");
		it.expect("04<*>89");

		it = new TestSubject("0123456789<*>");
		it.del("567");
		it.del("123");
		it.expect("0<*>489");

		it = new TestSubject("0123456789<*>");
		it.del("012345");
		it.del("345");
		it.expect("<*>6789");

		it = new TestSubject("0123456789<*>");
		it.del("345");
		it.del("012345");
		it.expect("<*>6789");

		it = new TestSubject("0123456789<*>");
		it.del("2345");
		it.del("34567");
		it.expect("01<*>89");

		it = new TestSubject("0123456789<*>");
		it.del("123");
		it.del("234");
		it.del("7");
		it.expect("056<*>89");
	}

	@Test public void testInserts() throws Exception {
		TestSubject it;

		it = new TestSubject("The fox jumps over the dog!");
		it.insBefore("fox", "quick ");
		it.insBefore("fox", "brown ");
		it.insBefore("dog", "lazy ");
		it.expect("The quick brown fox jumps over the lazy <*>dog!");
	}

	@Test public void testInsertAndDelete() throws Exception {
		TestSubject it;

		it = new TestSubject("The fox jumps over the dog!");

		it.insBefore("fox", "quick ");  //"The quick fox jumps..."
		it.del("The fox");              //" jumps..."
		it.insBefore("fox", "A rabbit");//"A rabbit jumps ..."

		it.expect("A rabbit<*> jumps over the dog!");
	}

	@Test public void testDeleteLine() throws Exception {
		TestSubject it;

		it = new TestSubject(
				"Line 0\n" +
				"Line 1\n" +
				"Line 2"
		);

		it.delLineAt("0");
		it.expect(
				"<*>Line 1\n" +
				"Line 2"
		);

		it.reset();
		it.delLineAt("1");
		it.expect(
				"Line 0<*>\n" +
				"Line 2"
		);

		it.reset();
		it.delLineAt("2");
		it.expect(
				"Line 0\n" +
				"Line 1<*>"
		);

		it = new TestSubject("Line 0"); //special case: no newlines in document
		it.delLineAt("0");
		it.expect("<*>");

		it = new TestSubject("");
		it.delLine(0);
		it.expect("<*>");

	}

}
