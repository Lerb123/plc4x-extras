/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.plc4x.merlot.api;

import java.util.List;

public interface TagSimplifier {

    /**
     * Group the tags into blocks.
     *
     * @param requests tags as written by the user, each one together with the
     * data type (PvType) of the record it belongs to.
     * @param maxGapBytes maximum allowed spacing between tags when joining
     * them; null (or 0) uses the driver's default value (15)
     * @return List of blocks containing the list of original addresses, the
     * simplified addresses, offsets, area number, starting byte, and number of
     * bytes
     */
    List<ItemBlock> simplify(List<TagRequest> requests, Integer maxGapBytes);

    /**
     * Same as {@link #simplify(List, Integer)} using the driver's default gap.
     *
     * @param requests tags as written by the user, with their data type.
     * @return List of blocks containing the list of original addresses, the
     * simplified addresses, offsets, area number, starting byte, and number of
     * bytes
     */
    default List<ItemBlock> simplify(List<TagRequest> requests) {
        return simplify(requests, null);
    }

    /**
     * The absolute offset of a tag, as written. Returns "null" if the tag is
     * invalid.
     *
     * @param tag Memory address from which the value at that position is
     * retrieved
     * @return Numeric position value in its direction
     */
    Offset offsetBase(String tag);

    /**
     * A tag as the user wrote it, plus the data type (PvType) of its record.
     * The type is needed because some types (for example the S7 compound
     * types) occupy more bytes than the address letter suggests.
     */
    record TagRequest(String tag, String dataType) {}

    /**
     * Representation of an offset: Byte.bit
     */
    record Offset(int byteOffset, int bit) {

        //Check whether this is actually a Boolean memory address
        public boolean isBit() {
            return bit >= 0;
        }

        @Override
        public String toString() {
            return isBit() ? byteOffset + "." + bit : String.valueOf(byteOffset);
        }
    }

    /**
     * The result of simplification: a range that can be read in a single pass.
     * Original tags, offsets and source indexes are parallel lists (same
     * position = same tag). The offsets are already calculated relative to the
     * start of the block. sourceIndexes holds the position each tag had in the
     * list given to simplify().
     */
    record ItemBlock(
            String area,
            int startByte,
            int totalBytes,
            String tagSimplificado,
            List<String> tagsOriginales,
            List<Offset> offsets,
            List<Integer> sourceIndexes) {

        /**
         *
         * @param positionInList Position inside this block's lists
         * @return The new offset, recalculated based on the start of the block
         */
        public Offset offsetRelativo(int positionInList) {
            return offsets.get(positionInList);
        }

        /**
         *
         * @param tag The original tag, to find it in the list of original tags
         * @return The new offset, recalculated based on the start of the block
         */
        public Offset offsetRelativo(String tag) {
            if (tag == null) {
                return null;
            }
            int i = tagsOriginales.indexOf(tag.trim());
            return i < 0 ? null : offsets.get(i);
        }
    }
}