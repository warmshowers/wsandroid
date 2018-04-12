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

package fi.bitrite.android.ws.util;

/**
 * Forked from org.apache.commons.
 *
 * <p>Operations on {@code Object}.</p>
 *
 * <p>This class tries to handle {@code null} input gracefully.
 * An exception will generally not be thrown for a {@code null} input.
 * Each method documents its behaviour in more detail.</p>
 *
 * <p>#ThreadSafe#</p>
 * @since 1.0
 */
//@Immutable
public class ObjectUtils {

    /**
     * <p>{@code ObjectUtils} instances should NOT be constructed in
     * standard programming. Instead, the static methods on the class should
     * be used, such as {@code ObjectUtils.defaultIfNull("a","b");}.</p>
     *
     * <p>This constructor is public to permit tools that require a JavaBean
     * instance to operate.</p>
     */
    public ObjectUtils() {
        super();
    }

    // Defaulting
    //-----------------------------------------------------------------------
    /**
     * <p>Returns a default value if the object passed is {@code null}.</p>
     *
     * <pre>
     * ObjectUtils.defaultIfNull(null, null)      = null
     * ObjectUtils.defaultIfNull(null, "")        = ""
     * ObjectUtils.defaultIfNull(null, "zz")      = "zz"
     * ObjectUtils.defaultIfNull("abc", *)        = "abc"
     * ObjectUtils.defaultIfNull(Boolean.TRUE, *) = Boolean.TRUE
     * </pre>
     *
     * @param <T> the type of the object
     * @param object  the {@code Object} to test, may be {@code null}
     * @param defaultValue  the default value to return, may be {@code null}
     * @return {@code object} if it is not {@code null}, defaultValue otherwise
     */
    public static <T> T defaultIfNull(final T object, final T defaultValue) {
        return object != null ? object : defaultValue;
    }

    /**
     * <p>Returns the first value in the array which is not {@code null}.
     * If all the values are {@code null} or the array is {@code null}
     * or empty then {@code null} is returned.</p>
     *
     * <pre>
     * ObjectUtils.firstNonNull(null, null)      = null
     * ObjectUtils.firstNonNull(null, "")        = ""
     * ObjectUtils.firstNonNull(null, null, "")  = ""
     * ObjectUtils.firstNonNull(null, "zz")      = "zz"
     * ObjectUtils.firstNonNull("abc", *)        = "abc"
     * ObjectUtils.firstNonNull(null, "xyz", *)  = "xyz"
     * ObjectUtils.firstNonNull(Boolean.TRUE, *) = Boolean.TRUE
     * ObjectUtils.firstNonNull()                = null
     * </pre>
     *
     * @param <T> the component type of the array
     * @param values  the values to test, may be {@code null} or empty
     * @return the first value from {@code values} which is not {@code null},
     *  or {@code null} if there are no non-null values
     * @since 3.0
     */
    @SafeVarargs
    public static <T> T firstNonNull(final T... values) {
        if (values != null) {
            for (final T val : values) {
                if (val != null) {
                    return val;
                }
            }
        }
        return null;
    }

    /**
     * Checks if any value in the given array is not {@code null}.
     *
     * <p>
     * If all the values are {@code null} or the array is {@code null}
     * or empty then {@code false} is returned. Otherwise {@code true} is returned.
     * </p>
     *
     * <pre>
     * ObjectUtils.anyNotNull(*)                = true
     * ObjectUtils.anyNotNull(*, null)          = true
     * ObjectUtils.anyNotNull(null, *)          = true
     * ObjectUtils.anyNotNull(null, null, *, *) = true
     * ObjectUtils.anyNotNull(null)             = false
     * ObjectUtils.anyNotNull(null, null)       = false
     * </pre>
     *
     * @param values  the values to test, may be {@code null} or empty
     * @return {@code true} if there is at least one non-null value in the array,
     * {@code false} if all values in the array are {@code null}s.
     * If the array is {@code null} or empty {@code false} is also returned.
     * @since 3.5
     */
    public static boolean anyNotNull(final Object... values) {
        return firstNonNull(values) != null;
    }

    /**
     * Checks if all values in the array are not {@code nulls}.
     *
     * <p>
     * If any value is {@code null} or the array is {@code null} then
     * {@code false} is returned. If all elements in array are not
     * {@code null} or the array is empty (contains no elements) {@code true}
     * is returned.
     * </p>
     *
     * <pre>
     * ObjectUtils.allNotNull(*)             = true
     * ObjectUtils.allNotNull(*, *)          = true
     * ObjectUtils.allNotNull(null)          = false
     * ObjectUtils.allNotNull(null, null)    = false
     * ObjectUtils.allNotNull(null, *)       = false
     * ObjectUtils.allNotNull(*, null)       = false
     * ObjectUtils.allNotNull(*, *, null, *) = false
     * </pre>
     *
     * @param values  the values to test, may be {@code null} or empty
     * @return {@code false} if there is at least one {@code null} value in the array or the array is {@code null},
     * {@code true} if all values in the array are not {@code null}s or array contains no elements.
     * @since 3.5
     */
    public static boolean allNotNull(final Object... values) {
        if (values == null) {
            return false;
        }

        for (final Object val : values) {
            if (val == null) {
                return false;
            }
        }

        return true;
    }

    /**
     * <p>Null safe comparison of Comparables.
     * {@code null} is assumed to be less than a non-{@code null} value.</p>
     *
     * @param <T> type of the values processed by this method
     * @param c1  the first comparable, may be null
     * @param c2  the second comparable, may be null
     * @return a negative value if c1 &lt; c2, zero if c1 = c2
     *  and a positive value if c1 &gt; c2
     */
    public static <T extends Comparable<? super T>> int compare(final T c1, final T c2) {
        return compare(c1, c2, false);
    }

    /**
     * <p>Null safe comparison of Comparables.</p>
     *
     * @param <T> type of the values processed by this method
     * @param c1  the first comparable, may be null
     * @param c2  the second comparable, may be null
     * @param nullGreater if true {@code null} is considered greater
     *  than a non-{@code null} value or if false {@code null} is
     *  considered less than a Non-{@code null} value
     * @return a negative value if c1 &lt; c2, zero if c1 = c2
     *  and a positive value if c1 &gt; c2
     * @see java.util.Comparator#compare(Object, Object)
     */
    public static <T extends Comparable<? super T>> int compare(final T c1, final T c2,
                                                                final boolean nullGreater) {
        if (c1 == c2) {
            return 0;
        } else if (c1 == null) {
            return nullGreater ? 1 : -1;
        } else if (c2 == null) {
            return nullGreater ? -1 : 1;
        }
        return c1.compareTo(c2);
    }
}
