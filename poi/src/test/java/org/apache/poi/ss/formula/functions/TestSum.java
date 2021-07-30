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

package org.apache.poi.ss.formula.functions;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFFormulaEvaluator;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.FormulaError;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.apache.poi.ss.util.Utils.addRow;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for SUM() -- there are plenty of additional cases in other test classes
 */
final class TestSum {

    @Test
    void testSum() throws IOException {
        try (HSSFWorkbook wb = initWorkbook1()) {
            HSSFFormulaEvaluator fe = new HSSFFormulaEvaluator(wb);
            HSSFCell cell = wb.getSheetAt(0).getRow(0).createCell(100);
            confirmDouble(fe, cell, "SUM(B2:B5)", 70000);
        }
    }

    @Test
    void testSumWithNA() throws IOException {
        try (HSSFWorkbook wb = initWorkbook1()) {
            HSSFFormulaEvaluator fe = new HSSFFormulaEvaluator(wb);
            HSSFCell cell = wb.getSheetAt(0).getRow(0).createCell(100);
            confirmError(fe, cell, "SUM(B2:B6)", FormulaError.NA);
        }
    }

    private HSSFWorkbook initWorkbook1() {
        HSSFWorkbook wb = new HSSFWorkbook();
        HSSFSheet sheet = wb.createSheet();
        addRow(sheet, 0, "Property Value", "Commission", "Data");
        addRow(sheet, 1, 100000, 7000, 250000);
        addRow(sheet, 2, 200000, 14000);
        addRow(sheet, 3, 300000, 21000);
        addRow(sheet, 4, 400000, 28000);
        addRow(sheet, 5, 500000, FormulaError.NA);
        return wb;
    }

    private static void confirmDouble(HSSFFormulaEvaluator fe, HSSFCell cell, String formulaText, double expectedResult) {
        cell.setCellFormula(formulaText);
        fe.notifyUpdateCell(cell);
        CellValue result = fe.evaluate(cell);
        assertEquals(result.getCellType(), CellType.NUMERIC);
        assertEquals(expectedResult, result.getNumberValue());
    }

    private static void confirmError(HSSFFormulaEvaluator fe, HSSFCell cell, String formulaText, FormulaError expectedError) {
        cell.setCellFormula(formulaText);
        fe.notifyUpdateCell(cell);
        CellValue result = fe.evaluate(cell);
        assertEquals(result.getCellType(), CellType.ERROR);
        assertEquals(expectedError.getCode(), result.getErrorValue());
    }
}
