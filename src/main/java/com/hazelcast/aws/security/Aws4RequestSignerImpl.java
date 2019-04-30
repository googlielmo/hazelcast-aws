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
import com.hazelcast.aws.impl.Constants;
import com.hazelcast.aws.utility.Aws4RequestSignerUtils;
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;
import com.hazelcast.util.QuickMath;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

import static java.lang.String.format;

public class Aws4RequestSignerImpl implements Aws4RequestSigner {

    private static final ILogger LOGGER = Logger.getLogger(Aws4RequestSignerImpl.class);

    private static final String NEW_LINE = "\n";
    private static final String API_TERMINATOR = "aws4_request";
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String UTF_8 = "UTF-8";
    private static final int DATE_LENGTH = 8;
    private static final int LAST_INDEX = 8;

    private final AwsConfig config;
    private final String timestamp;
    private final AwsCredentials awsCredentials;

    private String service;
    private String endpoint;
    private Map<String, String> attributes;
    private Map<String, String> headers;
    private String body;
    private String signature;

    public Aws4RequestSignerImpl(AwsConfig config, AwsCredentials awsCredentials, String timeStamp, String service, String endpoint) {
        this.config = config;
        this.awsCredentials = awsCredentials;
        this.timestamp = timeStamp;
        this.service = service;
        this.endpoint = endpoint;
    }

    public String getCredentialScope() {
        // datestamp/region/service/API_TERMINATOR
        String dateStamp = timestamp.substring(0, DATE_LENGTH);
        return format("%s/%s/%s/%s", dateStamp, config.getRegion(), this.service, API_TERMINATOR);
    }

    @Override
    public String sign(Map<String, String> attributes, Map<String, String> headers) {
        return this.sign(attributes, headers, "", Constants.GET);
    }

    @Override
    public String sign(Map<String, String> attributes, Map<String, String> headers, String body, String httpMethod) {
        this.attributes = attributes;
        this.headers = headers;
        this.body = body;

        String canonicalRequest = getCanonicalizedRequest(httpMethod);
        String stringToSign = createStringToSign(canonicalRequest);
        byte[] signingKey = deriveSigningKey();

        this.signature = createSignature(stringToSign, signingKey);
        return signature;
    }
    /* Task 1 */
    String getCanonicalizedRequest(String httpMethod) {
        return httpMethod + NEW_LINE + '/' + NEW_LINE + Aws4RequestSignerUtils.getCanonicalizedQueryString(this.attributes) + NEW_LINE
                + getCanonicalHeaders() + NEW_LINE + getSignedHeaders() + NEW_LINE + Aws4RequestSignerUtils.sha256Hashhex(body);
    }

    /* Task 2 */
    String createStringToSign(String canonicalRequest) {
        return Constants.SIGNATURE_METHOD_V4 + NEW_LINE + timestamp + NEW_LINE + getCredentialScope() + NEW_LINE + Aws4RequestSignerUtils.sha256Hashhex(
                canonicalRequest);
    }

    /* Task 3 */
    byte[] deriveSigningKey() {
        String signKey = awsCredentials.getSecretKey();
        String dateStamp = timestamp.substring(0, DATE_LENGTH);
        // this is derived from
        // http://docs.aws.amazon.com/general/latest/gr/signature-v4-examples.html#signature-v4-examples-python

        try {
            String key = "AWS4" + signKey;
            Mac mDate = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec skDate = new SecretKeySpec(key.getBytes(UTF_8), HMAC_SHA256);
            mDate.init(skDate);
            byte[] kDate = mDate.doFinal(dateStamp.getBytes(UTF_8));

            Mac mRegion = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec skRegion = new SecretKeySpec(kDate, HMAC_SHA256);
            mRegion.init(skRegion);
            byte[] kRegion = mRegion.doFinal(config.getRegion().getBytes(UTF_8));

            Mac mService = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec skService = new SecretKeySpec(kRegion, HMAC_SHA256);
            mService.init(skService);
            byte[] kService = mService.doFinal(this.service.getBytes(UTF_8));

            Mac mSigning = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec skSigning = new SecretKeySpec(kService, HMAC_SHA256);
            mSigning.init(skSigning);

            return mSigning.doFinal("aws4_request".getBytes(UTF_8));
        } catch (NoSuchAlgorithmException e) {
            return null;
        } catch (InvalidKeyException e) {
            return null;
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    String createSignature(String stringToSign, byte[] signingKey) {
        byte[] signature;
        try {
            Mac signMac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec signKS = new SecretKeySpec(signingKey, HMAC_SHA256);
            signMac.init(signKS);
            signature = signMac.doFinal(stringToSign.getBytes(UTF_8));
        } catch (NoSuchAlgorithmException e) {
            return null;
        } catch (InvalidKeyException e) {
            return null;
        } catch (UnsupportedEncodingException e) {
            return null;
        }
        return QuickMath.bytesToHex(signature);
    }

    String getCanonicalHeaders() {
        StringBuffer canonical = new StringBuffer();
        Map<String, String> sortedHeaders = getSortedLowercaseHeaders();
        for (String k : sortedHeaders.keySet()) {
            canonical.append(format("%s:%s%s", k.toLowerCase(), sortedHeaders.get(k), NEW_LINE));
        }
        LOGGER.finest("Canonical Headers:\n"+canonical.toString());
        return canonical.toString();
    }

    @Override
    public String getSignedHeaders() {
        StringBuffer signed = new StringBuffer();
        Map<String, String> sortedHeaders = getSortedLowercaseHeaders();
        int n = 0;
        for (String k : sortedHeaders.keySet()) {
            if (n++ > 0) {
                signed.append(";");
            }
            signed.append(k);
        }
        LOGGER.finest("Signed Headers:\n"+signed.toString());
        return signed.toString();
    }

    @Override
    public String getAuthorizationHeader() {
        return Aws4RequestSignerUtils.buildAuthHeader(awsCredentials.getAccessKey(), getCredentialScope(), getSignedHeaders(), signature);
    }

    Map<String, String> getSortedLowercaseHeaders() {
        Map<String, String> sortedHeaders = new TreeMap<String, String>();
        for (Map.Entry<String, String> e : headers.entrySet()) {
            sortedHeaders.put(e.getKey().toLowerCase(), e.getValue());
        }
        sortedHeaders.put("host", endpoint);
        return sortedHeaders;
    }

    @Override
    public String createFormattedCredential() {
        return awsCredentials.getAccessKey() + '/' + timestamp.substring(0, LAST_INDEX) + '/' + config.getRegion() + '/'
                + service + "/aws4_request";
    }

}
