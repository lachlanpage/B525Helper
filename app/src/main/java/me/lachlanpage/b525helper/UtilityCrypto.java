package me.lachlanpage.b525helper;

import org.spongycastle.crypto.digests.SHA256Digest;
import org.spongycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.spongycastle.crypto.params.KeyParameter;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.Security;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

// Holds utility methods that could be used during client proof
public class UtilityCrypto {

    UtilityCrypto() {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }

    public String toHex(byte[] bytes) {
        BigInteger bi = new BigInteger(1, bytes);
        return String.format("%0" + (bytes.length << 1) + "x", bi);
    }

    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    public String generateClientNonce() {
        return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
    }

    public byte[] getClientProof(String clientNone, String serverNonce, String password, String salt, int iterations)
    {
        String msg = clientNone + "," + serverNonce + "," + serverNonce;

        try {
            // API level 26+
            //SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            //PBEKeySpec spec0 = new PBEKeySpec(password.toCharArray(), hexStringToByteArray(salt), iterations, 64*4);
            //SecretKey spec1 = factory.generateSecret(spec0);

            // spongy castle PBKDF2 HmacSHA256 equivalent
            PKCS5S2ParametersGenerator gen = new PKCS5S2ParametersGenerator(new SHA256Digest());
            gen.init(password.getBytes("UTF-8"), hexStringToByteArray(salt), iterations);
            byte[] spec1 = ((KeyParameter) gen.generateDerivedParameters(256)).getKey();

            Mac hasher = Mac.getInstance("HmacSHA256");
            hasher.init(new SecretKeySpec("Client Key".getBytes("utf-8"), "HmacSHA256"));

            byte[] hash = hasher.doFinal(spec1);

            // stored key now
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(hash);

            // signature
            Mac signature = Mac.getInstance("HmacSHA256");
            signature.init(new SecretKeySpec(msg.getBytes("utf-8"), "HmacSHA256"));
            byte[] signatureBytes = signature.doFinal(encodedHash);

            byte[] clientProof =  new byte[hash.length];
            int i = 0;
            while ( i < hash.length) {
                int val = hash[i] ^ signatureBytes[i];
                clientProof[i] = (byte)val;
                i = i + 1;
            }
            return clientProof;
        }

        catch(Exception e) {
            System.out.println(e);
        }

        return null;
    }
}
