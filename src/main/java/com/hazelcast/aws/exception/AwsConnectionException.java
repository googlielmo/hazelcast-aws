/*
 * Copyright (c) 2008-2018, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.aws.exception;

/**
 * Thrown to indicate an error while connecting to AWS.
 * <p>
 * A list of error codes can be found at: {@see http://docs.aws.amazon.com/AWSEC2/latest/APIReference/errors-overview.html}.
 */
public class AwsConnectionException extends RuntimeException {

    private final int httpResponseCode;
    private final String errorMessage;

    public AwsConnectionException(int httpResponseCode, String errorMessage) {
        super(messageFrom(httpResponseCode, errorMessage));
        this.httpResponseCode = httpResponseCode;
        this.errorMessage = errorMessage;
    }

    public int getHttpResponseCode() {
        return httpResponseCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    private static String messageFrom(int httpResponseCode, String errorMessage) {
        return String.format("Connection to AWS failed (HTTP Response Code: %s, Message: \"%s\")",
                httpResponseCode, errorMessage);
    }
}
