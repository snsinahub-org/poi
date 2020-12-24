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

package org.apache.poi.ss.formula.ptg;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.apache.poi.hssf.usermodel.HSSFEvaluationWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.junit.jupiter.api.Test;

/**
 * Tests for Area3DPtg
 */
public final class TestArea3DPtg extends BaseTestPtg {

	/**
	 * confirms that sheet names get properly escaped
	 */
	@Test
	public void testToFormulaString() throws IOException {

		Area3DPtg target = new Area3DPtg("A1:B1", (short)0);

		String sheetName = "my sheet";
		try (HSSFWorkbook wb = createWorkbookWithSheet(sheetName)) {
			HSSFEvaluationWorkbook book = HSSFEvaluationWorkbook.create(wb);
			assertEquals("'my sheet'!A1:B1", target.toFormulaString(book));

			wb.setSheetName(0, "Sheet1");
			assertEquals("Sheet1!A1:B1", target.toFormulaString(book));

			wb.setSheetName(0, "C64");
			assertEquals("'C64'!A1:B1", target.toFormulaString(book));
		}
	}
}
