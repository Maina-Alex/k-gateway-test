package com.ekenya.apigateway.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class KeysConfig {
    private final AppProperties appProperties;

    @Bean
    public RSAPublicKey publicKey() {
        try {
            Certificate certificate = keyStore().getCertificate(appProperties.getAlias ());
            PublicKey publicKey = certificate.getPublicKey();

            if (publicKey instanceof RSAPublicKey) {
                return (RSAPublicKey) publicKey;
            }
        } catch (KeyStoreException e) {
            log.error("Unable to load private key from keystore: {}", appProperties.getName (), e);
        }

        throw new IllegalArgumentException("Unable to load RSA public key");
    }

    @Bean
    public RSAPrivateKey privateKey() {
        try {
            ClassPathResource resource = new ClassPathResource(appProperties.getName ());
            keyStore().load(resource.getInputStream(), appProperties.getPassword ().toCharArray());

            return (RSAPrivateKey) keyStore().getKey(appProperties.getAlias (), appProperties.getPassword ().toCharArray());
        } catch (IOException | CertificateException | NoSuchAlgorithmException | KeyStoreException |
                 UnrecoverableKeyException e) {
            log.error("Unable to load keystore: {}", appProperties.getName (), e);
        }

        throw new IllegalArgumentException("Unable to load keystore");
    }

    public KeyStore keyStore() {
        try {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            ClassPathResource resource = new ClassPathResource(appProperties.getName ());
            keyStore.load(resource.getInputStream(), appProperties.getPassword ().toCharArray());
            return keyStore;
        } catch (IOException | CertificateException | NoSuchAlgorithmException | KeyStoreException e) {
            log.error("Unable to load keystore: {}", appProperties.getName (), e);
        }

        throw new IllegalArgumentException("Unable to load keystore");
    }
}
