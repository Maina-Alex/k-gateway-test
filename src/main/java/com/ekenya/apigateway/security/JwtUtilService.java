package com.ekenya.apigateway.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.context.annotation.Configuration;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * @author Alex Maina
 * @created 13/03/2023
 **/
@RequiredArgsConstructor
@Configuration
public class JwtUtilService {
    private final KeysConfig keysConfig;

    @SneakyThrows
    public String generateJwt(String username, List<String> authorities, List<String> roles) {
        Calendar calendar = Calendar.getInstance ();
        Date now = calendar.getTime ();
        //INTERNAL Token expires after 7 days
        //Allows time to process any workflow| approval within 7 days
        Date expiryDate = new Date (now.getTime () + 604800);
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder ()
                .subject (username)
                .claim ("USERNAME", username)
                .claim ("jti", UUID.randomUUID ())
                .claim ("CLIENT_ID", "CHANNEL-AUTH_KEY")
                .claim ("authorities", authorities)
                .claim ("roles", roles)
                .claim ("SCOPE", "AUTH")
                .audience ("CHANNEL-MANAGER")
                .expirationTime (expiryDate)
                .notBeforeTime (now)
                .issueTime (now)
                .build ();

        JWSHeader jwsHeader = new JWSHeader.Builder (JWSAlgorithm.RS256)
                .type (JOSEObjectType.JWT)
                .build ();
        SignedJWT signedJWT = new SignedJWT (jwsHeader, claimsSet);
        RSASSASigner signer = new RSASSASigner (keysConfig.privateKey ());
        signedJWT.sign (signer);
        return signedJWT.toString ();
    }
}
