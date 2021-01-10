/* ====================================================================
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
==================================================================== */

package org.apache.poi.poifs.filesystem;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.poi.util.IOUtils;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;
import org.junit.jupiter.api.Test;

final class TestEmptyDocument {
    private static final POILogger LOG = POILogFactory.getLogger(TestEmptyDocument.class);

    @Test
	void testSingleEmptyDocument() throws IOException {
		POIFSFileSystem fs = new POIFSFileSystem();
		DirectoryEntry dir = fs.getRoot();
		dir.createDocument("Foo", new ByteArrayInputStream(new byte[] {}));

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		fs.writeFilesystem(out);
		new POIFSFileSystem(new ByteArrayInputStream(out.toByteArray())).close();
		fs.close();
	}

    @Test
	void testSingleEmptyDocumentEvent() throws IOException {
		POIFSFileSystem fs = new POIFSFileSystem();
		DirectoryEntry dir = fs.getRoot();
		dir.createDocument("Foo", 0, event -> LOG.log(POILogger.WARN, "written"));

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		fs.writeFilesystem(out);
		new POIFSFileSystem(new ByteArrayInputStream(out.toByteArray())).close();
		fs.close();
	}

    @Test
	void testEmptyDocumentWithFriend() throws IOException {
		POIFSFileSystem fs = new POIFSFileSystem();
		DirectoryEntry dir = fs.getRoot();
		dir.createDocument("Bar", new ByteArrayInputStream(new byte[] { 0 }));
		dir.createDocument("Foo", new ByteArrayInputStream(new byte[] {}));

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		fs.writeFilesystem(out);
		new POIFSFileSystem(new ByteArrayInputStream(out.toByteArray())).close();
		fs.close();
	}

    @Test
	void testEmptyDocumentEventWithFriend() throws IOException {
		POIFSFileSystem fs = new POIFSFileSystem();
		DirectoryEntry dir = fs.getRoot();
		dir.createDocument("Bar", 1, event -> {
			try {
				event.getStream().write(0);
			} catch (IOException exception) {
				throw new RuntimeException("exception on write: " + exception);
			}
		});
		dir.createDocument("Foo", 0, event -> {});

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		fs.writeFilesystem(out);
		new POIFSFileSystem(new ByteArrayInputStream(out.toByteArray())).close();
		fs.close();
	}

    @Test
	void testEmptyDocumentBug11744() throws Exception {
		byte[] testData = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };

		POIFSFileSystem fs = new POIFSFileSystem();
		fs.createDocument(new ByteArrayInputStream(new byte[0]), "Empty");
		fs.createDocument(new ByteArrayInputStream(testData), "NotEmpty");
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		fs.writeFilesystem(out);
		out.toByteArray();
		fs.close();

		// This line caused the error.
		fs = new POIFSFileSystem(new ByteArrayInputStream(out.toByteArray()));

		DocumentEntry entry = (DocumentEntry) fs.getRoot().getEntry("Empty");
		assertEquals(0, entry.getSize(), "Expected zero size");
		byte[] actualReadbackData;
		actualReadbackData = IOUtils.toByteArray(new DocumentInputStream(entry));
		assertEquals(0, actualReadbackData.length, "Expected zero read from stream");

		entry = (DocumentEntry) fs.getRoot().getEntry("NotEmpty");
		actualReadbackData = IOUtils.toByteArray(new DocumentInputStream(entry));
		assertEquals(testData.length, entry.getSize(), "Expected size was wrong");
		assertArrayEquals(testData, actualReadbackData, "Expected same data read from stream");
		fs.close();
	}
}
