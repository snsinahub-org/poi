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

package org.apache.poi.hemf.record.emf;

import static org.apache.poi.hemf.record.emf.HemfDraw.readPointL;
import static org.apache.poi.hemf.record.emf.HemfFill.readBitmap;
import static org.apache.poi.hemf.record.emf.HemfFill.readXForm;
import static org.apache.poi.hemf.record.emf.HemfRecordIterator.HEADER_SIZE;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hemf.draw.HemfGraphics;
import org.apache.poi.hwmf.record.HwmfBinaryRasterOp;
import org.apache.poi.hwmf.record.HwmfBitmapDib;
import org.apache.poi.hwmf.record.HwmfBrushStyle;
import org.apache.poi.hwmf.record.HwmfColorRef;
import org.apache.poi.hwmf.record.HwmfHatchStyle;
import org.apache.poi.hwmf.record.HwmfMapMode;
import org.apache.poi.hwmf.record.HwmfMisc;
import org.apache.poi.hwmf.record.HwmfMisc.WmfSetBkMode;
import org.apache.poi.hwmf.record.HwmfPalette.PaletteEntry;
import org.apache.poi.hwmf.record.HwmfPenStyle;
import org.apache.poi.hwmf.record.HwmfPenStyle.HwmfLineDash;
import org.apache.poi.util.LittleEndianConsts;
import org.apache.poi.util.LittleEndianInputStream;

public class HemfMisc {
    private static final int MAX_RECORD_LENGTH = 10_000_000;

    public static class EmfEof implements HemfRecord {
        protected final List<PaletteEntry> palette = new ArrayList<>();

        @Override
        public HemfRecordType getEmfRecordType() {
            return HemfRecordType.eof;
        }

        @Override
        public long init(LittleEndianInputStream leis, long recordSize, long recordId) throws IOException {
            final int startIdx = leis.getReadIndex();

            // A 32-bit unsigned integer that specifies the number of palette entries.
            final int nPalEntries = (int) leis.readUInt();
            // A 32-bit unsigned integer that specifies the offset to the palette entries from the start of this record.
            final int offPalEntries = (int) leis.readUInt();

            int size = 2 * LittleEndianConsts.INT_SIZE;

            if (offPalEntries > 0) {
                int undefinedSpace1 = (int) (offPalEntries - size - HEADER_SIZE);
                assert (undefinedSpace1 >= 0);
                leis.skipFully(undefinedSpace1);
                size += undefinedSpace1;

                for (int i = 0; i < nPalEntries; i++) {
                    PaletteEntry pe = new PaletteEntry();
                    size += pe.init(leis);
                }

                int undefinedSpace2 = (int) (recordSize - size - LittleEndianConsts.INT_SIZE);
                assert (undefinedSpace2 >= 0);
                leis.skipFully(undefinedSpace2);
                size += undefinedSpace2;
            }

            // A 32-bit unsigned integer that MUST be the same as Size and MUST be the
            // last field of the record and hence the metafile.
            // LogPaletteEntry objects, if they exist, MUST precede this field.
            long sizeLast = leis.readUInt();
            size += LittleEndianConsts.INT_SIZE;
            // some files store the whole file size in sizeLast, other just the last record size
            // assert (sizeLast == size+HEADER_SIZE);
            assert (recordSize == size);

            return size;
        }
    }

    /**
     * The EMF_SAVEDC record saves the playback device context for later retrieval.
     */
    public static class EmfSaveDc extends HwmfMisc.WmfSaveDc implements HemfRecord {
        @Override
        public HemfRecordType getEmfRecordType() {
            return HemfRecordType.saveDc;
        }

        @Override
        public long init(LittleEndianInputStream leis, long recordSize, long recordId) throws IOException {
            return 0;
        }
    }

    /**
     * The EMF_RESTOREDC record restores the playback device context from a previously saved device
     * context.
     */
    public static class EmfRestoreDc extends HwmfMisc.WmfRestoreDc implements HemfRecord {

        @Override
        public HemfRecordType getEmfRecordType() {
            return HemfRecordType.restoreDc;
        }

        @Override
        public long init(LittleEndianInputStream leis, long recordSize, long recordId) throws IOException {
            // A 32-bit signed integer that specifies the saved state to restore relative to
            // the current state. This value MUST be negative; –1 represents the state that was most
            // recently saved on the stack, –2 the one before that, etc.
            nSavedDC = leis.readInt();
            return LittleEndianConsts.INT_SIZE;
        }
    }

    /**
     * The META_SETBKCOLOR record sets the background color in the playback device context to a
     * specified color, or to the nearest physical color if the device cannot represent the specified color.
     */
    public static class EmfSetBkColor extends HwmfMisc.WmfSetBkColor implements HemfRecord {

        @Override
        public HemfRecordType getEmfRecordType() {
            return HemfRecordType.setBkColor;
        }

        @Override
        public long init(LittleEndianInputStream leis, long recordSize, long recordId) throws IOException {
            return colorRef.init(leis);
        }
    }


    /**
     * The EMR_SETBKMODE record specifies the background mix mode of the playback device context.
     * The background mix mode is used with text, hatched brushes, and pen styles that are not solid
     * lines.
     */
    public static class EmfSetBkMode extends WmfSetBkMode implements HemfRecord {
        public HemfRecordType getEmfRecordType() {
            return HemfRecordType.setBkMode;
        }

        @Override
        public long init(LittleEndianInputStream leis, long recordSize, long recordId) throws IOException {
            /*
             * A 32-bit unsigned integer that specifies the background mode
             * and MUST be in the BackgroundMode (section 2.1.4) enumeration
             */
            bkMode = HwmfBkMode.valueOf((int) leis.readUInt());
            return LittleEndianConsts.INT_SIZE;
        }
    }

    /**
     * The EMR_SETMAPPERFLAGS record specifies parameters of the process of matching logical fonts to
     * physical fonts, which is performed by the font mapper.
     */
    public static class EmfSetMapperFlags extends HwmfMisc.WmfSetMapperFlags implements HemfRecord {
        public HemfRecordType getEmfRecordType() {
            return HemfRecordType.setMapperFlags;
        }

        @Override
        public long init(LittleEndianInputStream leis, long recordSize, long recordId) throws IOException {
            return super.init(leis, recordSize, (int) recordId);
        }
    }

    /**
     * The EMR_SETMAPMODE record specifies the mapping mode of the playback device context. The
     * mapping mode specifies the unit of measure used to transform page space units into device space
     * units, and also specifies the orientation of the device's x-axis and y-axis.
     */
    public static class EmfSetMapMode extends HwmfMisc.WmfSetMapMode implements HemfRecord {
        public HemfRecordType getEmfRecordType() {
            return HemfRecordType.setMapMode;
        }

        @Override
        public long init(LittleEndianInputStream leis, long recordSize, long recordId) throws IOException {
            // A 32-bit unsigned integer whose definition MUST be in the MapMode enumeration
            mapMode = HwmfMapMode.valueOf((int) leis.readUInt());
            return LittleEndianConsts.INT_SIZE;
        }
    }

    /**
     * The EMR_SETROP2 record defines a binary raster operation mode.
     */
    public static class EmfSetRop2 extends HwmfMisc.WmfSetRop2 implements HemfRecord {
        @Override
        public HemfRecordType getEmfRecordType() {
            return HemfRecordType.setRop2;
        }

        @Override
        public long init(LittleEndianInputStream leis, long recordSize, long recordId) throws IOException {
            // A 32-bit unsigned integer that specifies the raster operation mode and
            // MUST be in the WMF Binary Raster Op enumeration
            drawMode = HwmfBinaryRasterOp.valueOf((int) leis.readUInt());
            return LittleEndianConsts.INT_SIZE;
        }
    }


    /**
     * The EMR_SETSTRETCHBLTMODE record specifies bitmap stretch mode.
     */
    public static class EmfSetStretchBltMode extends HwmfMisc.WmfSetStretchBltMode implements HemfRecord {

        @Override
        public HemfRecordType getEmfRecordType() {
            return HemfRecordType.setStretchBltMode;
        }

        @Override
        public long init(LittleEndianInputStream leis, long recordSize, long recordId) throws IOException {
            // A 32-bit unsigned integer that specifies the stretch mode and MAY be
            // in the StretchMode enumeration.
            stretchBltMode = StretchBltMode.valueOf((int) leis.readUInt());
            return LittleEndianConsts.INT_SIZE;
        }
    }

    /**
     * The EMR_CREATEBRUSHINDIRECT record defines a logical brush for graphics operations.
     */
    public static class EmfCreateBrushIndirect extends HwmfMisc.WmfCreateBrushIndirect implements HemfRecord {
        /**
         * A 32-bit unsigned integer that specifies the index of the logical brush object in the
         * EMF Object Table. This index MUST be saved so that this object can be reused or modified.
         */
        private int brushIdx;

        @Override
        public HemfRecordType getEmfRecordType() {
            return HemfRecordType.createBrushIndirect;
        }

        @Override
        public long init(LittleEndianInputStream leis, long recordSize, long recordId) throws IOException {
            brushIdx = (int) leis.readUInt();

            brushStyle = HwmfBrushStyle.valueOf((int) leis.readUInt());
            colorRef = new HwmfColorRef();
            int size = colorRef.init(leis);
            brushHatch = HwmfHatchStyle.valueOf((int) leis.readUInt());
            return size + 3 * LittleEndianConsts.INT_SIZE;
        }

        @Override
        public void draw(HemfGraphics ctx) {
            ctx.addObjectTableEntry(this, brushIdx);
        }


        @Override
        public String toString() {
            return
                "{ brushIndex: "+brushIdx+
                ", brushStyle: '"+brushStyle+"'"+
                ", colorRef: "+colorRef+
                ", brushHatch: '"+brushHatch+"' }";
        }
    }

    /**
     * The EMR_DELETEOBJECT record deletes a graphics object, which is specified by its index
     * in the EMF Object Table
     */
    public static class EmfDeleteObject extends HwmfMisc.WmfDeleteObject implements HemfRecord {

        @Override
        public HemfRecordType getEmfRecordType() {
            return HemfRecordType.deleteobject;
        }

        @Override
        public long init(LittleEndianInputStream leis, long recordSize, long recordId) throws IOException {
            objectIndex = (int) leis.readUInt();
            return LittleEndianConsts.INT_SIZE;
        }
    }

    /**
     * The EMR_CREATEPEN record defines a logical pen for graphics operations.
     */
    public static class EmfCreatePen extends HwmfMisc.WmfCreatePenIndirect implements HemfRecord {
        /**
         * A 32-bit unsigned integer that specifies the index of the logical palette object
         * in the EMF Object Table. This index MUST be saved so that this object can be
         * reused or modified.
         */
        protected int penIndex;

        @Override
        public HemfRecordType getEmfRecordType() {
            return HemfRecordType.createPen;
        }

        @Override
        public long init(LittleEndianInputStream leis, long recordSize, long recordId) throws IOException {
            penIndex = (int) leis.readUInt();

            // A 32-bit unsigned integer that specifies the PenStyle.
            // The value MUST be defined from the PenStyle enumeration table
            penStyle = HwmfPenStyle.valueOf((int) leis.readUInt());

            int widthX = leis.readInt();
            int widthY = leis.readInt();
            dimension.setSize(widthX, widthY);

            int size = colorRef.init(leis);

            return size + 4 * LittleEndianConsts.INT_SIZE;
        }

        @Override
        public void draw(HemfGraphics ctx) {
            ctx.addObjectTableEntry(this, penIndex);
        }

        @Override
        public String toString() {
            return super.toString().replaceFirst("\\{", "{ penIndex: "+penIndex+", ");
        }
    }

    public static class EmfExtCreatePen extends EmfCreatePen {
        protected HwmfBrushStyle brushStyle;
        protected HwmfHatchStyle hatchStyle;

        protected int[] styleEntry;

        protected final HwmfBitmapDib bitmap = new HwmfBitmapDib();


        @Override
        public HemfRecordType getEmfRecordType() {
            return HemfRecordType.extCreatePen;
        }

        @Override
        public long init(LittleEndianInputStream leis, long recordSize, long recordId) throws IOException {
            final int startIdx = leis.getReadIndex();

            penIndex = (int) leis.readUInt();

            // A 32-bit unsigned integer that specifies the offset from the start of this
            // record to the DIB header, if the record contains a DIB.
            int offBmi = (int) leis.readUInt();

            // A 32-bit unsigned integer that specifies the size of the DIB header, if the
            // record contains a DIB.
            int cbBmi = (int) leis.readUInt();

            // A 32-bit unsigned integer that specifies the offset from the start of this
            // record to the DIB bits, if the record contains a DIB.
            int offBits = (int) leis.readUInt();

            // A 32-bit unsigned integer that specifies the size of the DIB bits, if the record
            // contains a DIB.
            int cbBits = (int) leis.readUInt();

            // A 32-bit unsigned integer that specifies the PenStyle.
            // The value MUST be defined from the PenStyle enumeration table
            penStyle = HwmfPenStyle.valueOf((int) leis.readUInt());

            // A 32-bit unsigned integer that specifies the width of the line drawn by the pen.
            // If the pen type in the PenStyle field is PS_GEOMETRIC, this value is the width in logical
            // units; otherwise, the width is specified in device units. If the pen type in the PenStyle field is
            // PS_COSMETIC, this value MUST be 0x00000001.
            long width = leis.readUInt();
            dimension.setSize(width, 0);
            int size = 7 * LittleEndianConsts.INT_SIZE;

            // A 32-bit unsigned integer that specifies a brush style for the pen from the WMF BrushStyle enumeration
            //
            // If the pen type in the PenStyle field is PS_GEOMETRIC, this value MUST be either BS_SOLID or BS_HATCHED.
            // The value of this field can be BS_NULL, but only if the line style specified in PenStyle is PS_NULL.
            // The BS_NULL style SHOULD be used to specify a brush that has no effect
            brushStyle = HwmfBrushStyle.valueOf((int) leis.readUInt());

            size += LittleEndianConsts.INT_SIZE;

            size += colorRef.init(leis);

            hatchStyle = HwmfHatchStyle.valueOf(leis.readInt());
            size += LittleEndianConsts.INT_SIZE;

            // The number of elements in the array specified in the StyleEntry
            // field. This value SHOULD be zero if PenStyle does not specify PS_USERSTYLE.
            final int numStyleEntries = (int) leis.readUInt();
            size += LittleEndianConsts.INT_SIZE;

            assert (numStyleEntries == 0 || penStyle.getLineDash() == HwmfLineDash.USERSTYLE);

            // An optional array of 32-bit unsigned integers that defines the lengths of
            // dashes and gaps in the line drawn by this pen, when the value of PenStyle is
            // PS_USERSTYLE line style for the pen. The array contains a number of entries specified by
            // NumStyleEntries, but it is used as if it repeated indefinitely.
            // The first entry in the array specifies the length of the first dash. The second entry specifies
            // the length of the first gap. Thereafter, lengths of dashes and gaps alternate.
            // If the pen type in the PenStyle field is PS_GEOMETRIC, the lengths are specified in logical
            // units; otherwise, the lengths are specified in device units.

            styleEntry = new int[numStyleEntries];

            for (int i = 0; i < numStyleEntries; i++) {
                styleEntry[i] = (int) leis.readUInt();
            }

            size += numStyleEntries * LittleEndianConsts.INT_SIZE;

            size += readBitmap(leis, bitmap, startIdx, offBmi, cbBmi, offBits, cbBits);

            return size;
        }

        @Override
        public String toString() {
            // TODO: add style entries + bmp
            return super.toString().replaceFirst("\\{",
                "{ brushStyle: '"+brushStyle+"', hatchStyle: '"+hatchStyle+"', ");
        }
    }

    /**
     * The EMR_SETMITERLIMIT record specifies the limit for the length of miter joins for the playback
     * device context.
     */
    public static class EmfSetMiterLimit implements HemfRecord {
        protected int miterLimit;

        @Override
        public HemfRecordType getEmfRecordType() {
            return HemfRecordType.setMiterLimit;
        }

        @Override
        public long init(LittleEndianInputStream leis, long recordSize, long recordId) throws IOException {
            miterLimit = (int) leis.readUInt();
            return LittleEndianConsts.INT_SIZE;
        }

        @Override
        public void draw(HemfGraphics ctx) {
            ctx.getProperties().setPenMiterLimit(miterLimit);
        }
    }


    public static class EmfSetBrushOrgEx implements HemfRecord {
        protected final Point2D origin = new Point2D.Double();

        @Override
        public HemfRecordType getEmfRecordType() {
            return HemfRecordType.setBrushOrgEx;
        }

        @Override
        public long init(LittleEndianInputStream leis, long recordSize, long recordId) throws IOException {
            return readPointL(leis, origin);
        }
    }

    public static class EmfSetWorldTransform implements HemfRecord {
        protected final AffineTransform xForm = new AffineTransform();

        @Override
        public HemfRecordType getEmfRecordType() {
            return HemfRecordType.setWorldTransform;
        }

        @Override
        public long init(LittleEndianInputStream leis, long recordSize, long recordId) throws IOException {
            return readXForm(leis, xForm);
        }
    }

    public static class EmfModifyWorldTransform implements HemfRecord {
        protected final AffineTransform xForm = new AffineTransform();
        protected int modifyWorldTransformMode;

        @Override
        public HemfRecordType getEmfRecordType() {
            return HemfRecordType.modifyWorldTransform;
        }

        @Override
        public long init(LittleEndianInputStream leis, long recordSize, long recordId) throws IOException {
            // An XForm object that defines a two-dimensional linear transform in logical units.
            // This transform is used according to the ModifyWorldTransformMode to define a new value for
            // the world-space to page-space transform in the playback device context.
            int size = readXForm(leis, xForm);

            // A 32-bit unsigned integer that specifies how the transform specified in Xform is used.
            // This value MUST be in the ModifyWorldTransformMode enumeration
            modifyWorldTransformMode = (int)leis.readUInt();

            return size + LittleEndianConsts.INT_SIZE;
        }
    }
}
