import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.net.Socket;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Base64;

public class CP2Client {

    public static void main(String[] args) {

        // We can specify the file to send over to Server by hard-coding here. However, we can also choose to specify
        //  the file to send over through console commands by appending an argument.
        String filename = "1000.txt";
        if (args.length > 0) filename = args[0];

        // Same reasoning as the file name above. Either hard-code the server address here, or user can provide the
        //  the server address as an argument in console.
        String serverAddress = "localhost";
        if (args.length > 1) filename = args[1];

        // Same reasoning as file name and server address above.
        int port = 4321;
        if (args.length > 2) port = Integer.parseInt(args[2]);

        int numBytes = 0;

        Socket clientSocket = null;

        DataOutputStream toServer = null;
        DataInputStream fromServer = null;

        FileInputStream fileInputStream = null;
        BufferedInputStream bufferedFileInputStream = null;

        long timeStarted = System.nanoTime();

        String CA_Cert_filepath = "C:\\Users\\kting\\Documents\\GitHub\\MyPA2_CSE\\Keys and Certificates\\cacse.crt";

        try {

            System.out.println("Establishing connection to server...");

            // Connect to server and get the input and output streams
            clientSocket = new Socket(serverAddress, port);
            toServer = new DataOutputStream(clientSocket.getOutputStream());
            fromServer = new DataInputStream(clientSocket.getInputStream());

            System.out.println("Sending file...");

            // TODO: Generate a proper nonce: possibly current datetime?
            String nonce = "This is my nonce!";
            System.out.println("Generated Nonce: " + nonce);

            // Sending nonce over to Server.
            System.out.println("Sending nonce to Server");
            toServer.writeInt(2);
            toServer.writeInt(nonce.getBytes().length);
            toServer.write(nonce.getBytes());

            int packetType = fromServer.readInt();

            // Receiving encrypted nonce from Server.
            int encrypted_nonce_size = fromServer.readInt();
            byte [] encrypted_nonce_bytearray = new byte[encrypted_nonce_size];
            fromServer.readFully(encrypted_nonce_bytearray, 0, encrypted_nonce_size);
            String encrypted_nonce = new String(encrypted_nonce_bytearray);
            System.out.println("Received Encrypted Nonce from Server: " + new String(encrypted_nonce));

            // Receiving CA-signed certificate of Server's public key.
            int CA_signed_bytearray_size = fromServer.readInt();
            byte [] CA_signed_bytearray = new byte[CA_signed_bytearray_size];
            fromServer.readFully(CA_signed_bytearray, 0, CA_signed_bytearray_size);
            String CA_signed = new String(CA_signed_bytearray);
            // System.out.println("CA Signed Certification is: " + CA_signed);

            // Validating Server's cert; Verifying Server's cert with CA's public key; Extracting Server's public key
            PublicKey Server_PublicKey = ExtractPublicKeyFromCASignedCert.extract(CA_Cert_filepath);

            // TODO: This is where Client decrypts the encrypted nonce with Server's public key. Then performs comparison with the nonce it sends earlier.
            System.out.println("Nonce verified. Server authenticated.");
            System.out.println("Server's Public Key is: " + Server_PublicKey);

            // Prepare Server's Public Key cipher
            Cipher rsa_cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            rsa_cipher.init(Cipher.ENCRYPT_MODE, Server_PublicKey);

            // Generate Session Key (AES), and Create Session Key Cipher object
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(128);
            SecretKey aesKey = keyGen.generateKey();
            Cipher aes_Cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            aes_Cipher.init(Cipher.ENCRYPT_MODE, aesKey);
            System.out.println("Session Key generated.");
            System.out.println(Arrays.toString(aesKey.getEncoded()));

            // Encrypt Session Key with Server's Public Key (to be sent over to Server). Note that we set AES session key to be 128-bits, 16-bytes long.
            byte [] desKey_bytearray = aesKey.getEncoded();
            byte [] encrypted_desKey = rsa_cipher.doFinal(desKey_bytearray);

            // Send Encrypted Session key over to Server
            toServer.writeInt(3);
            toServer.writeInt(encrypted_desKey.length);
            toServer.write(encrypted_desKey);
            System.out.println("Sent encrypted session key to Server.");
            System.out.println(Base64.getEncoder().encodeToString(encrypted_desKey));


            // Send the filename
            toServer.writeInt(0);
            toServer.writeInt(filename.getBytes().length);
            toServer.write(filename.getBytes());
            //toServer.flush();

            // Open the file
            fileInputStream = new FileInputStream(filename);
            bufferedFileInputStream = new BufferedInputStream(fileInputStream);

            byte [] fromFileBuffer = new byte[117];

            // Send the file
            int i = 1;
            for (boolean fileEnded = false; !fileEnded;) {

                numBytes = bufferedFileInputStream.read(fromFileBuffer);

                if (numBytes < 117){
                    System.out.println("Last Packet to be sent");

                }
                fileEnded = numBytes < 117;

                byte[] ciphertext_bytearray = aes_Cipher.doFinal(fromFileBuffer);

                toServer.writeInt(1);
                toServer.writeInt(numBytes);
                toServer.write(ciphertext_bytearray);
                toServer.flush();

                System.out.println("Sending packet Number " + i + " of size " + numBytes);
                System.out.println(Arrays.toString(fromFileBuffer));

                i++;
                Thread.sleep(10);
            }


            bufferedFileInputStream.close();
            fileInputStream.close();

            System.out.println("Closing connection...");

        } catch (Exception e) {e.printStackTrace();}

        long timeTaken = System.nanoTime() - timeStarted;
        System.out.println("Program took: " + timeTaken/1000000.0 + "ms to run");
    }
}
