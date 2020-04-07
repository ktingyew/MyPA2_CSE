import java.security.PrivateKey;

public class PrivateKeyReaderTester {

    public static void main(String[] args) throws Exception{
        System.out.println("Hello World!");
        String filepath = "C:\\Users\\kting\\Documents\\GitHub\\MyPA2_CSE\\Keys and Certificates\\private_key.der";

        PrivateKey privateKey = PrivateKeyReader.get(filepath);
        System.out.println(privateKey);



    }

}
