package com.genoutfit.api;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.ssl.SSLContexts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

@Configuration
public class R2Config {
    @Value("${R2_ACCESS_KEY_ID}")
    private String accessKeyId;
    @Value("${R2_SECRET_ACCESS_KEY}")
    private String secretAccessKey;
    @Value("${ACCOUNT_ID}")
    private String accountId;
    private String endpoint = "https://c2137757a033f71e5abddb52bba08b3f.r2.cloudflarestorage.com";
    @Value("${R2_BUCKET_NAME}")
    private String bucketName;

    @Bean
    public AmazonS3 r2Client() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        BasicAWSCredentials awsCredentials = new BasicAWSCredentials(accessKeyId, secretAccessKey);

        ClientConfiguration clientConfig = new ClientConfiguration();
        clientConfig.setSignerOverride("AWSS3V4SignerType");

        // Load the default Java trust store
        SSLContext sslContext = SSLContexts.custom()
                .loadTrustMaterial(null, (x509Certificates, s) -> true)
                .build();

        clientConfig.getApacheHttpClientConfig()
                .setSslSocketFactory(new SSLConnectionSocketFactory(sslContext, new NoopHostnameVerifier()));

        return AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(
                        endpoint,
                        "auto"
                ))
                .withPathStyleAccessEnabled(true)
                .withClientConfiguration(clientConfig)
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .build();
    }

    @Bean
    public String bucketName() {
        return bucketName;
    }
}
