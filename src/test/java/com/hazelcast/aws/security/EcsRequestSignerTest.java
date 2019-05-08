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

package com.hazelcast.aws.security;

import com.hazelcast.aws.AwsConfig;
import com.hazelcast.aws.impl.DescribeInstances;
import com.hazelcast.test.HazelcastSerialClassRunner;
import com.hazelcast.test.annotation.QuickTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(HazelcastSerialClassRunner.class)
@Category(QuickTest.class)
public class EcsRequestSignerTest {

    private final static String TEST_REGION = "eu-central-1";
    private final static String TEST_HOST = "ecs.eu-central-1.amazonaws.com";
    private final static String TEST_SERVICE = "ecs";
    private final static String TEST_ACCESS_KEY = "AKIDEXAMPLE";
    private final static String TEST_SECRET_KEY = "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY";
    private final static String TEST_REQUEST_DATE = "20141106T111126Z";
    private final static String TEST_DERIVED_EXPECTED = "ac8d19964fcea9428c6cf191526249112adf5547331898b190239b834fbb7c9e";
    private final static String TEST_SIGNATURE_EXPECTED = "b0e93ee3108fdb85c7ad29eca28646d2b3c0b1218c527fec6cba3580ab806733";

    @Test
    public void deriveSigningKeyTest()
            throws Exception {
        // this is from http://docs.aws.amazon.com/general/latest/gr/signature-v4-examples.html
        AwsConfig awsConfig = AwsConfig.builder()
                .setRegion(TEST_REGION)
                .setHostHeader(TEST_HOST)
                .setAccessKey(TEST_ACCESS_KEY)
                .setSecretKey(TEST_SECRET_KEY)
                .build();
        AwsCredentials awsCredentials = new AwsCredentials(awsConfig);

        DescribeInstances di = new DescribeInstances(awsConfig, TEST_HOST);
        // Override the attributes map. We need to change values. Not pretty, but
        // no real alternative, and in this case : testing only

        Field field = di.getClass().getSuperclass().getSuperclass().getDeclaredField("attributes");
        field.setAccessible(true);
        Map<String, String> attributes = (Map<String, String>) field.get(di);
        attributes.put("X-Amz-Date", TEST_REQUEST_DATE);
        field.set(di, attributes);

        // Override private method
        Aws4RequestSignerImpl rs = new Aws4RequestSignerImpl(awsConfig, awsCredentials, TEST_REQUEST_DATE, TEST_SERVICE, TEST_HOST);

        Method method = rs.getClass().getDeclaredMethod("deriveSigningKey");
        method.setAccessible(true);
        byte[] derivedKey = (byte[]) method.invoke(rs);

        assertEquals(TEST_DERIVED_EXPECTED, bytesToHex(derivedKey));
    }

    @Test
    public void testSigning()
            throws NoSuchFieldException, IllegalAccessException, IOException {
        AwsConfig awsConfig = AwsConfig.builder().setRegion(TEST_REGION).
                setHostHeader(TEST_HOST).
                                               setAccessKey(TEST_ACCESS_KEY).
                                               setSecretKey(TEST_SECRET_KEY).build();
        AwsCredentials awsCredentials = new AwsCredentials(awsConfig);

        DescribeInstances di = new DescribeInstances(awsConfig, TEST_HOST);
        di.getRequestSigner();

        Field headersField = di.getClass().getSuperclass().getSuperclass().getDeclaredField("headers");
        headersField.setAccessible(true);
        Map<String, String> headers = (Map<String, String>) headersField.get(di);
        headers.put("X-Amz-Date", TEST_REQUEST_DATE);
        headers.put("Host", TEST_HOST);

        Aws4RequestSignerImpl actual = new Aws4RequestSignerImpl(awsConfig, awsCredentials, TEST_REQUEST_DATE, TEST_SERVICE, TEST_HOST);
        headers.put("X-Amz-Credential", actual.createFormattedCredential());
        String signature = actual.sign(headers, new HashMap<String, String>());

        assertEquals(TEST_SIGNATURE_EXPECTED, signature);
    }

    private String bytesToHex(byte[] in) {
        char[] hexArray = "0123456789abcdef".toCharArray();

        char[] hexChars = new char[in.length * 2];
        for (int j = 0; j < in.length; j++) {
            int v = in[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
