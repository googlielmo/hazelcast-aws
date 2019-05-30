/*
 * Copyright (c) 2008-2019, Hazelcast, Inc. All Rights Reserved.
 *
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

package com.hazelcast.aws.utility;

public final class StringUtils {

    private StringUtils() {
    }

    /**
     * Checks if a string is empty or not after trim operation
     *
     * @param s the string to check.
     * @return true if the string is null or empty, false otherwise
     */
    public static boolean isEmpty(String s) {
        if (s == null) {
            return true;
        }
        return s.trim().isEmpty();
    }

    /**
     * Checks if a string is empty or not after trim operation
     *
     * @param s the string to check.
     * @return true if the string is not null or not empty, false otherwise
     */
    public static boolean isNotEmpty(String s) {
        return !isEmpty(s);
    }

}
