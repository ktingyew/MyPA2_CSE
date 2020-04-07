import java.security.PrivateKey;
import java.security.PublicKey;

public class PublicKeyReaderTester {

    public static void main(String[] args) throws Exception{

        String filepath = "C:\\Users\\kting\\Documents\\GitHub\\MyPA2_CSE\\Keys and Certificates\\public_key.der";

        PublicKey publicKey = PublicKeyReader.get(filepath);
        System.out.println(publicKey);

    }

}
