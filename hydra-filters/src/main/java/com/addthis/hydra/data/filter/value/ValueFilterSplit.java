/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.addthis.hydra.data.filter.value;

import java.util.ArrayList;
import java.util.List;

import com.addthis.basis.util.Strings;

import com.addthis.bundle.util.ValueUtil;
import com.addthis.bundle.value.ValueArray;
import com.addthis.bundle.value.ValueFactory;
import com.addthis.bundle.value.ValueMap;
import com.addthis.bundle.value.ValueObject;
import com.addthis.codec.Codec;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;

/**
 * This {@link ValueFilter ValueFilter} <span class="hydra-summary">splits the input into an array or a map</span>.
 * <p/>
 * <p>The input must be a string. The {@link #split split} field or the
 * {@link #fixedLength fixedLength} field is used to divide the input into a sequence
 * of elements. If the {@link #keySplit keySplit} field
 * is used then the sequence is returned as a map. Otherwise
 * the sequence is returned as an array.</p>
 * <p>Example:</p>
 * <pre>
 *     // The use of the keySplit field specifies that a map is returned
 *     {op:"field", from:"PATH_PARAMS", filter:{op:"split", split:"&", keySplit:"="}},
 * </pre>
 *
 * @user-reference
 * @hydra-name split
 * @exclude-fields once
 */
public class ValueFilterSplit extends ValueFilter {

    /**
     * Use this field as a delimiter in between
     * elements in the input string.
     * Default is ",".
     */
    @Codec.Set(codable = true)
    private String split = ",";

    /**
     * If this field is non-null,
     * then split the input into a map using
     * this delimiter between keys and values.
     * Default is null.
     */
    @Codec.Set(codable = true)
    private String keySplit;

    /**
     * An optional filter on elements of the output sequence. Default is null.
     */
    @Codec.Set(codable = true)
    private ValueFilter filter;

    /**
     * If keySplit is used, then this is an optional
     * filter on keys of the output map. Default is null.
     */
    @Codec.Set(codable = true)
    private ValueFilter keyFilter;

    /**
     * If this field is a positive integer,
     * then ignore the 'split' field and
     * divide the input string
     * into elements of equal length.
     * Default is -1.
     */
    @Codec.Set(codable = true)
    private int fixedLength = -1;

    public ValueFilterSplit setSplit(String split) {
        this.split = split;
        return this;
    }

    public ValueFilterSplit setKeySplit(String keySplit) {
        this.keySplit = keySplit;
        return this;
    }

    public ValueFilterSplit setFilter(ValueFilter filter) {
        this.filter = filter;
        return this;
    }

    public ValueFilterSplit setKeyFilter(ValueFilter keyFilter) {
        this.keyFilter = keyFilter;
        return this;
    }

    @Override
    public ValueObject filterValue(ValueObject value) {
        return filter != null ? filter.filter(value) : value;
    }

    private String filterKey(String value) {
        return keyFilter != null ? ValueUtil.asNativeString(keyFilter.filter(ValueFactory.create(value))) : value;
    }

    @Override
    public ValueObject filter(ValueObject value) {
        String string = ValueUtil.asNativeString(value);
        if (string == null || string.isEmpty()) {
            return null;
        }
        String token[];
        if (fixedLength > 0) {
            token = splitFixedLength(string, fixedLength);
        } else {
            token = Strings.splitArray(string, split);
        }
        if (keySplit != null) {
            ValueMap map = ValueFactory.createMap();
            for (String v : token) {
                int pos;
                if ((pos = v.indexOf(keySplit)) >= 0) {
                    String k = filterKey(v.substring(0, pos));
                    if (k == null) {
                        continue;
                    }
                    v = v.substring(pos + keySplit.length());
                    map.put(k, filterValue(ValueFactory.create(v)));
                } else {
                    v = filterKey(v);
                    if (v == null) {
                        continue;
                    }
                    map.put(v, filterValue(ValueFactory.create(v)));
                }
            }
            return map;
        } else {
            ValueArray arr = ValueFactory.createArray(token.length);
            for (String v : token) {
                arr.append(filterValue(ValueFactory.create(v)));
            }
            return arr;
        }
    }

    protected String[] splitFixedLength(String line, int length) {
        Iterable<String> splitIter = Splitter.fixedLength(length).split(line);
        List<String> tok = new ArrayList<>();
        Iterables.addAll(tok, splitIter);
        return Iterables.toArray(tok, String.class);
    }
}
