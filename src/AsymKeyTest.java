import javax.crypto.Cipher;
import java.security.PrivateKey;
import java.security.PublicKey;

public class AsymKeyTest {
    public static void main(String[] args) throws Exception{
        String CA_Cert_filepath = "C:\\Users\\kting\\Documents\\GitHub\\MyPA2_CSE\\Keys and Certificates\\cacse.crt";

        PublicKey Server_PublicKey = ExtractPublicKeyFromCASignedCert.extract(CA_Cert_filepath);

        String filepath = "C:\\Users\\kting\\Documents\\GitHub\\MyPA2_CSE\\Keys and Certificates\\private_key.der";
        PrivateKey privateKey = PrivateKeyReader.get(filepath);


        // Prepare all the ciphering stuff.
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, Server_PublicKey);
        String plaintext = "Test String";

        byte[] plaintext_byte = plaintext.getBytes();
        byte[] enc_byte = cipher.doFinal(plaintext_byte);

        // Prepare all the deciphering stuff.
        Cipher decipher = Cipher.getInstance("RSA");
        decipher.init(Cipher.DECRYPT_MODE, privateKey);

        byte[] dec_byte = decipher.doFinal(enc_byte);

        System.out.println(new String(dec_byte));



    }
}
