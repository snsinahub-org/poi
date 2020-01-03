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

package org.apache.poi.hssf.record.chart;


import static org.apache.poi.hssf.record.TestcaseRecordInputStream.confirmRecordEncoding;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.poi.hssf.record.TestcaseRecordInputStream;
import org.junit.Test;

/**
 * Tests the serialization and deserialization of the AxisOptionsRecord
 * class works correctly.  Test data taken directly from a real
 * Excel file.
 */
public final class TestAxisOptionsRecord {
    private static final byte[] data = {
        (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x01,
        (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x01,(byte)0x00,
        (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
        (byte)0x00,(byte)0xEF,(byte)0x00
    };

    @Test
    public void testLoad() {
        AxisOptionsRecord record = new AxisOptionsRecord(TestcaseRecordInputStream.create(0x1062, data));
        assertEquals( 0, record.getMinimumCategory());
        assertEquals( 0, record.getMaximumCategory());
        assertEquals( 1, record.getMajorUnitValue());
        assertEquals( 0, record.getMajorUnit());
        assertEquals( 1, record.getMinorUnitValue());
        assertEquals( 0, record.getMinorUnit());
        assertEquals( 0, record.getBaseUnit());
        assertEquals( 0, record.getCrossingPoint());
        assertEquals( 239, record.getOptions());
        assertTrue(record.isDefaultMinimum());
        assertTrue(record.isDefaultMaximum());
        assertTrue(record.isDefaultMajor());
        assertTrue(record.isDefaultMinorUnit());
        assertFalse(record.isIsDate());
        assertTrue(record.isDefaultBase());
        assertTrue(record.isDefaultCross());
        assertTrue(record.isDefaultDateSettings());

        assertEquals( 22, record.getRecordSize() );
    }

    @SuppressWarnings("squid:S2699")
    @Test
    public void testStore() {
        AxisOptionsRecord record = new AxisOptionsRecord();
        record.setMinimumCategory( (short)0 );
        record.setMaximumCategory( (short)0 );
        record.setMajorUnitValue( (short)1 );
        record.setMajorUnit( (short)0 );
        record.setMinorUnitValue( (short)1 );
        record.setMinorUnit( (short)0 );
        record.setBaseUnit( (short)0 );
        record.setCrossingPoint( (short)0 );
        record.setOptions( (short)239 );
        record.setDefaultMinimum( true );
        record.setDefaultMaximum( true );
        record.setDefaultMajor( true );
        record.setDefaultMinorUnit( true );
        record.setIsDate( false );
        record.setDefaultBase( true );
        record.setDefaultCross( true );
        record.setDefaultDateSettings( true );


        byte [] recordBytes = record.serialize();
        confirmRecordEncoding(AxisOptionsRecord.sid, data, recordBytes);
    }
}
