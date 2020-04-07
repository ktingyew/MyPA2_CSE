import java.io.FileInputStream;
import java.io.InputStream;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class ClientSide {

    public static void main(String[] args) throws Exception{

        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        // Obtaining public key of CA from CA's certificate. We can trust the certificate.
        InputStream fis_CA = new FileInputStream("C:\\Users\\kting\\Documents\\GitHub\\MyPA2_CSE\\Keys and Certificates\\cacse.crt");
        X509Certificate CACert =(X509Certificate)cf.generateCertificate(fis_CA);
        PublicKey CA_PublicKey = CACert.getPublicKey();

        // Check Validity of Server's certificate. Also verify Server's certificate with the CA"s public key we obtained earlier.
        InputStream fis_server = new FileInputStream("C:\\Users\\kting\\Documents\\GitHub\\MyPA2_CSE\\Keys and Certificates\\CAsigned.crt");
        X509Certificate ServerCert =(X509Certificate)cf.generateCertificate(fis_server);
        ServerCert.checkValidity();
        ServerCert.verify(CA_PublicKey);
        System.out.println("Hello World");

        // If we get to here, means that Server's cert is verified. We can now extract the Server's public key from it.
        PublicKey Server_PublicKey = ServerCert.getPublicKey();





    }

}




