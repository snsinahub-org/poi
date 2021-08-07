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

package org.apache.poi.ss.formula.atp;

import org.apache.poi.ss.formula.OperationEvaluationContext;
import org.apache.poi.ss.formula.eval.*;
import org.apache.poi.ss.formula.functions.FreeRefFunction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implementation of Excel function TEXTJOIN()
 *
 * <b>Syntax</b><br>
 * <b>TEXTJOIN</b>(<b>delimiter</b>, <b>ignore_empty</b>, <b>text1</b>, <b>[text2]<b>, …)<p>
 *
 * <b>delimiter</b> A text string, either empty, or one or more characters enclosed by double quotes, or a reference to a valid text string.
 * If a number is supplied, it will be treated as text.<br>
 * <b>ignore_empty</b> If TRUE, ignores empty cells.<br>
 * <b>text1</b> Text item to be joined. A text string, or array of strings, such as a range of cells.<br>
 * <b>text2 ...</b> Optional. Additional text items to be joined. There can be a maximum of 252 text arguments for the text items, including text1.
 * Each can be a text string, or array of strings, such as a range of cells.<br>
 *
 * @since POI 5.0.1
 */
final class TextJoinFunction implements FreeRefFunction {

    public static final FreeRefFunction instance = new TextJoinFunction(ArgumentsEvaluator.instance);

    private ArgumentsEvaluator evaluator;

    private TextJoinFunction(ArgumentsEvaluator anEvaluator) {
        // enforces singleton
        this.evaluator = anEvaluator;
    }

    public ValueEval evaluate(ValueEval[] args, OperationEvaluationContext ec) {
        /*
         * Must be at least three arguments:
         *  - delimiter    Delimiter for joining text arguments
         *  - ignoreEmpty  If true, empty strings will be ignored in the join
         *  - text1		   First value to be evaluated as text and joined
         *  - text2, etc.  Optional additional values to be evaluated and joined
         */

        // Make sure we have at least one text value, and at most 252 text values, as documented at:
        // https://support.microsoft.com/en-us/office/textjoin-function-357b449a-ec91-49d0-80c3-0e8fc845691c?ui=en-us&rs=en-us&ad=us
        if (args.length < 3 || args.length > 254) {
            return ErrorEval.VALUE_INVALID;
        }

        int srcRowIndex = ec.getRowIndex();
        int srcColumnIndex = ec.getColumnIndex();

        try {
            // Get the delimiter argument
            ValueEval delimiterArg = OperandResolver.getSingleValue(args[0], srcRowIndex, srcColumnIndex);
            String delimiter = OperandResolver.coerceValueToString(delimiterArg);

            // Get the boolean ignoreEmpty argument
            ValueEval ignoreEmptyArg = OperandResolver.getSingleValue(args[1], srcRowIndex, srcColumnIndex);
            boolean ignoreEmpty = OperandResolver.coerceValueToBoolean(ignoreEmptyArg, false);

            // Get a list of string values for each text argument
            ArrayList<String> textValues = new ArrayList<>();

            for (int i = 2; i < args.length; i++) {
                List<ValueEval> textArgs = getValues(args[i], srcRowIndex, srcColumnIndex);
                for (ValueEval textArg : textArgs) {
                    String textValue = OperandResolver.coerceValueToString(textArg);

                    // If we're not ignoring empty values or if our value is not empty, add it to the list
                    if (!ignoreEmpty || (textValue != null && textValue.length() > 0)) {
                        textValues.add(textValue);
                    }
                }
            }

            // Join the list of values with the specified delimiter and return
            return new StringEval(String.join(delimiter, textValues));
        } catch (EvaluationException e){
            return e.getErrorEval();
        }
    }

    private List<ValueEval> getValues(ValueEval eval, int srcRowIndex, int srcColumnIndex) throws EvaluationException {
        if (eval instanceof AreaEval) {
            AreaEval ae = (AreaEval)eval;
            List<ValueEval> list = new ArrayList<>();
            for (int r = 0; r < ae.getHeight(); r++) {
                for (int c = 0; c < ae.getWidth(); c++) {
                    list.add(ae.getRelativeValue(r, c));
                }
            }
            return list;
        } else {
            return Collections.singletonList(OperandResolver.getSingleValue(eval, srcRowIndex, srcColumnIndex));
        }
    }

}
