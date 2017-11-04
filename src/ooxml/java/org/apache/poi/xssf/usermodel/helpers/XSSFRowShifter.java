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

package org.apache.poi.xssf.usermodel.helpers;

import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.formula.FormulaParseException;
import org.apache.poi.ss.formula.FormulaParser;
import org.apache.poi.ss.formula.FormulaRenderer;
import org.apache.poi.ss.formula.FormulaShifter;
import org.apache.poi.ss.formula.FormulaType;
import org.apache.poi.ss.formula.ptg.Ptg;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.helpers.RowShifter;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.util.Internal;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFEvaluationWorkbook;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCell;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCellFormula;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCfRule;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTConditionalFormatting;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTWorksheet;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.STCellFormulaType;

/**
 * Helper for shifting rows up or down
 */
// non-Javadoc: When possible, code should be implemented in the RowShifter abstract class to avoid duplication with
// {@link org.apache.poi.hssf.usermodel.helpers.HSSFRowShifter}
public final class XSSFRowShifter extends RowShifter {
    private static final POILogger logger = POILogFactory.getLogger(XSSFRowShifter.class);

    public XSSFRowShifter(XSSFSheet sh) {
        super(sh);
    }

    /**
     * Updated named ranges
     */
    @Override
    public void updateNamedRanges(FormulaShifter formulaShifter) {
        XSSFRowColShifter.updateNamedRanges(sheet, formulaShifter);
    }

    /**
     * Update formulas.
     */
    @Override
    public void updateFormulas(FormulaShifter formulaShifter) {
        //update formulas on the parent sheet
        updateSheetFormulas(sheet, formulaShifter);

        //update formulas on other sheets
        Workbook wb = sheet.getWorkbook();
        for (Sheet sh : wb) {
            if (sheet == sh) continue;
            updateSheetFormulas(sh, formulaShifter);
        }
    }

    private void updateSheetFormulas(Sheet sh, FormulaShifter formulashifter) {
        for (Row r : sh) {
            XSSFRow row = (XSSFRow) r;
            updateRowFormulas(row, formulashifter);
        }
    }

    /**
     * Update the formulas in specified row using the formula shifting policy specified by shifter
     *
     * @param row the row to update the formulas on
     * @param formulaShifter the formula shifting policy
     */
    @Internal
    @Override
    public void updateRowFormulas(Row row, FormulaShifter formulaShifter) {
        XSSFSheet sheet = (XSSFSheet) row.getSheet();
        for (Cell c : row) {
            XSSFCell cell = (XSSFCell) c;

            CTCell ctCell = cell.getCTCell();
            if (ctCell.isSetF()) {
                CTCellFormula f = ctCell.getF();
                String formula = f.getStringValue();
                if (formula.length() > 0) {
                    String shiftedFormula = shiftFormula(row, formula, formulaShifter);
                    if (shiftedFormula != null) {
                        f.setStringValue(shiftedFormula);
                        if(f.getT() == STCellFormulaType.SHARED){
                            int si = (int)f.getSi();
                            CTCellFormula sf = sheet.getSharedFormula(si);
                            sf.setStringValue(shiftedFormula);
                            updateRefInCTCellFormula(row, formulaShifter, sf);
                        }
                    }

                }

                //Range of cells which the formula applies to.
                updateRefInCTCellFormula(row, formulaShifter, f);
            }

        }
    }

    private void updateRefInCTCellFormula(Row row, FormulaShifter formulaShifter, CTCellFormula f) {
        if (f.isSetRef()) { //Range of cells which the formula applies to.
            String ref = f.getRef();
            String shiftedRef = shiftFormula(row, ref, formulaShifter);
            if (shiftedRef != null) f.setRef(shiftedRef);
        }
    }

    /**
     * Shift a formula using the supplied FormulaShifter
     *
     * @param row     the row of the cell this formula belongs to. Used to get a reference to the parent workbook.
     * @param formula the formula to shift
     * @param formulaShifter the FormulaShifter object that operates on the parsed formula tokens
     * @return the shifted formula if the formula was changed,
     *         <code>null</code> if the formula wasn't modified
     */
    private static String shiftFormula(Row row, String formula, FormulaShifter formulaShifter) {
        Sheet sheet = row.getSheet();
        Workbook wb = sheet.getWorkbook();
        int sheetIndex = wb.getSheetIndex(sheet);
        final int rowIndex = row.getRowNum();
        XSSFEvaluationWorkbook fpb = XSSFEvaluationWorkbook.create((XSSFWorkbook) wb);
        
        try {
            Ptg[] ptgs = FormulaParser.parse(formula, fpb, FormulaType.CELL, sheetIndex, rowIndex);
            String shiftedFmla = null;
            if (formulaShifter.adjustFormula(ptgs, sheetIndex)) {
                shiftedFmla = FormulaRenderer.toFormulaString(fpb, ptgs);
            }
            return shiftedFmla;
        } catch (FormulaParseException fpe) {
            // Log, but don't change, rather than breaking
            logger.log(POILogger.WARN, "Error shifting formula on row ", row.getRowNum(), fpe);
            return formula;
        }
    }

    @Override
    public void updateConditionalFormatting(FormulaShifter formulaShifter) {
        XSSFRowColShifter.updateConditionalFormatting(sheet, formulaShifter);
    }
    
    /**
     * Shift the Hyperlink anchors (not the hyperlink text, even if the hyperlink
     * is of type LINK_DOCUMENT and refers to a cell that was shifted). Hyperlinks
     * do not track the content they point to.
     *
     * @param formulaShifter
     */
    @Override
    public void updateHyperlinks(FormulaShifter formulaShifter) {
        XSSFRowColShifter.updateHyperlinks(sheet, formulaShifter);
    }


}
