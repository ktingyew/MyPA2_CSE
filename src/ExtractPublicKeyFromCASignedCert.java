import java.io.FileInputStream;
import java.io.InputStream;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class ExtractPublicKeyFromCASignedCert {

    public static PublicKey extract(String CA_pubkey_filepath, String CA_signed_Server_pubkey_filepath) throws Exception{

        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        // Obtaining public key of CA from CA's Public Key Certificate File. We can trust the certificate because it's from CA.
        InputStream fis_CA = new FileInputStream(CA_pubkey_filepath);
        X509Certificate CACert =(X509Certificate)cf.generateCertificate(fis_CA);
        PublicKey CA_PublicKey = CACert.getPublicKey();

        // TODO: If given String of ServerCert, instead of InputStream, how to do this? We have to have another argument for this String.
        // Check Validity of Server's certificate. Also verify Server's certificate with the CA"s public key we obtained earlier.
        InputStream fis_server = new FileInputStream(CA_signed_Server_pubkey_filepath);
        X509Certificate ServerCert =(X509Certificate)cf.generateCertificate(fis_server);
        ServerCert.checkValidity();
        ServerCert.verify(CA_PublicKey);

        // If we get to here, means that Server's cert is verified. We can now extract the Server's public key from it.
        PublicKey Server_PublicKey = ServerCert.getPublicKey();
        return Server_PublicKey;



    }

}




