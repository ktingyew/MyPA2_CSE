import javax.crypto.Cipher;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.net.Socket;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Base64;

public class CP3Client {

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




            // Send the filename
            toServer.writeInt(0);
            toServer.writeInt(filename.getBytes().length);
            toServer.write(filename.getBytes());
            //toServer.flush();

            // Open the file
            fileInputStream = new FileInputStream(filename);
            bufferedFileInputStream = new BufferedInputStream(fileInputStream);

            // Prepare all the ciphering stuff.
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, Server_PublicKey);
            // ========================================
            // Prepare all the deciphering objects
            String server_PrivateKey_filepath = "C:\\Users\\kting\\Documents\\GitHub\\MyPA2_CSE\\Keys and Certificates\\private_key.der";
            PrivateKey Server_PrivateKey = PrivateKeyReader.get(server_PrivateKey_filepath);
            Cipher decipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            decipher.init(Cipher.DECRYPT_MODE, Server_PrivateKey);
            // ========================================

            byte [] fromFileBuffer = new byte[117];

            // Send the file
            int i = 1;
            for (boolean fileEnded = false; !fileEnded;) {

                numBytes = bufferedFileInputStream.read(fromFileBuffer);
                if (numBytes < 117){
                    fromFileBuffer = Arrays.copyOfRange(fromFileBuffer, 0, numBytes);
                }


                fileEnded = numBytes < 117;

                byte[] ciphertext_bytearray = cipher.doFinal(fromFileBuffer);
                String plaintext64 = Base64.getEncoder().encodeToString(fromFileBuffer);
                String ciphertext64 = Base64.getEncoder().encodeToString(ciphertext_bytearray);


                toServer.writeInt(1);
                toServer.writeInt(numBytes);
                toServer.write(fromFileBuffer);
                toServer.write(ciphertext_bytearray);
                toServer.flush();

                System.out.println("Sending packet Number " + i + " of size " + numBytes + " " + fromFileBuffer.length + ", encrypted size of " + ciphertext_bytearray.length);
                //System.out.println(new String(decipher.doFinal(ciphertext_bytearray)));
                System.out.println(Arrays.toString(fromFileBuffer));
                System.out.println(ciphertext64);
                i++;

                Thread.sleep(50);
            }

            Thread.sleep(1000);

            bufferedFileInputStream.close();
            fileInputStream.close();

            System.out.println("Closing connection...");

        } catch (Exception e) {e.printStackTrace();}

        long timeTaken = System.nanoTime() - timeStarted;
        System.out.println("Program took: " + timeTaken/1000000.0 + "ms to run");
    }
}
