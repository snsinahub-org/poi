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

package org.apache.poi.ss.usermodel;

/**
 * Ordered list of table style elements, for both data tables and pivot tables.
 * Some elements only apply to pivot tables, but any style definition can omit any number,
 * so having them in one list should not be a problem.
 * <p/>
 * The order is the specification order of application, with later elements overriding previous
 * ones, if style properties conflict.
 * <p/>
 * Processing could iterate bottom-up if looking for specific properties, and stop when the
 * first style is found defining a value for that property.
 * <p/>
 * Enum names match the OOXML spec values exactly, so {@link #valueOf(String)} will work.
 * 
 * @since 3.17 beta 1
 */
public enum TableStyleType {

    wholeTable,
    headerRow,
    totalRow,
    firstColumn,
    lastColumn,
    firstRowStripe,
    secondRowStripe,
    firstColumnStripe,
    secondColumnStripe,
    firstHeaderCell,
    lastHeaderCell,
    firstTotalCell,
    lastTotalCell,
    firstSubtotalColumn,
    secondSubtotalColumn,
    thirdSubtotalColumn,
    firstSubtotalRow,
    secondSubtotalRow,
    thirdSubtotalRow,
    blankRow,
    firstColumnSubheading,
    secondColumnSubheading,
    thirdColumnSubheading,
    firstRowSubheading,
    secondRowSubheading,
    thirdRowSubheading,
    pageFieldLabels,
    pageFieldValues,
    ;
}
