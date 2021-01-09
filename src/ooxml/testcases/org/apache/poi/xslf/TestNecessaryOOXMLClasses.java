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

package org.apache.poi.xslf;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import org.apache.xmlbeans.XmlObject;
import org.junit.jupiter.api.Test;

// aim is to get these classes loaded and included in poi-ooxml-lite.jar
public class TestNecessaryOOXMLClasses {

    @Test
    void testProblemClasses() {
        List<Supplier<XmlObject>> sup = Arrays.asList(
            org.openxmlformats.schemas.presentationml.x2006.main.STPlaceholderSize.Factory::newInstance,
            org.openxmlformats.schemas.presentationml.x2006.main.CTHeaderFooter.Factory::newInstance
        );

        for (Supplier<XmlObject> xo : sup) {
            assertNotNull(xo.get());
        }
    }
}
