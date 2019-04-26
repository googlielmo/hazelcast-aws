package com.hazelcast.aws.security;

import com.hazelcast.aws.AwsConfig;
import com.hazelcast.aws.impl.Constants;
import uk.co.lucasweb.aws.v4.signer.Header;
import uk.co.lucasweb.aws.v4.signer.HttpRequest;
import uk.co.lucasweb.aws.v4.signer.Signer;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.hazelcast.aws.utility.StringUtil.isNotEmpty;

/**
 *
 */
public class Aws4RequestSignerReference implements Aws4RequestSigner {

    private static final int LAST_INDEX = 8;
    private final AwsConfig awsConfig;
    private final AwsCredentials awsCredentials;
    private final String timestamp;
    private final String service;
    private final String endpoint;

    private String signedHeaders;
    private String authorization;

    public Aws4RequestSignerReference(AwsConfig awsConfig, AwsCredentials awsCredentials, String timeStamp, String service, String endpoint) {
        this.awsConfig = awsConfig;
        this.awsCredentials = awsCredentials;
        this.timestamp = timeStamp;
        this.service = service;
        this.endpoint = endpoint;
    }

    @Override
    public String sign(Map<String, String> attributes, Map<String, String> headers) {
        return this.sign(attributes, headers, "", Constants.GET);
    }

    @Override
    public String sign(Map<String, String> attributes, Map<String, String> headers, String body, String httpMethod) {
        String query = Aws4RequestSignerImpl.getCanonicalizedQueryString(attributes);
        String spec = "/" + (isNotEmpty(query) ? "?" + query : "");
        URL url = null;
        HttpRequest request = null;
        try {
            url = new URL(new URL("https", endpoint, -1, "/"), spec);
            request = new HttpRequest(httpMethod, url.toURI());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        List<Header> headerList = new ArrayList<Header>();
        Set<Map.Entry<String, String>> entries = headers.entrySet();
        for (Map.Entry<String, String> e : entries) {
            headerList.add(new Header(e.getKey(), e.getValue()));
        }

        Signer signer = Signer.builder()
                .awsCredentials(new uk.co.lucasweb.aws.v4.signer.credentials.AwsCredentials(awsCredentials.getAccessKey(), awsCredentials.getSecretKey()))
                .date(timestamp)
                .region(awsConfig.getRegion())
                .headers(headerList.toArray(new Header[0]))
                .build(request, service, Aws4RequestSignerImpl.sha256Hashhex(body));
        String xAmzSignature = signer.getXAmzSignature();
        this.signedHeaders = signer.getXAmzSignedHeaders();
        this.authorization = signer.getSignature();
        return xAmzSignature;
    }

    @Override
    public String getSignedHeaders() {
        return this.signedHeaders;
    }

    @Override
    public String getAuthorizationHeader() {
        return this.authorization;
    }

    @Override
    public String createFormattedCredential() {
        return awsCredentials.getAccessKey() + '/' + timestamp.substring(0, LAST_INDEX) + '/' + awsConfig.getRegion() + '/'
                + service + "/aws4_request";
    }

}
