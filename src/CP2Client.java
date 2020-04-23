import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.*;
import java.net.Socket;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Base64;
import java.util.Random;

public class CP2Client {

    public static void main(String[] args) {

        // We can specify the file to send over to Server by hard-coding here. However, we can also choose to specify
        //  the file to send over through console commands by appending an argument.
        String filename = "buggy.txt";
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

        FileOutputStream CA_fos = null;

        // Client will have access to this file because everyone has access to CA's public key, cos it's well known.
        String CA_Cert_pubkey_filepath = "Keys and Certificates\\cacse.crt";

        try {

            System.out.println("Establishing connection to server...");

            // Connect to server and get the input and output streams
            clientSocket = new Socket(serverAddress, port);
            toServer = new DataOutputStream(clientSocket.getOutputStream());
            fromServer = new DataInputStream(clientSocket.getInputStream());

            // Generating nonce
            Random rand = new Random(System.currentTimeMillis());
            int nonce = rand.nextInt();
            System.out.println("Generated Nonce: " + nonce);

            // Sending nonce over to Server.
            System.out.println("Sending nonce to Server");
            toServer.writeInt(2);
            toServer.writeInt(Integer.toString(nonce).getBytes().length);
            toServer.write(Integer.toString(nonce).getBytes());

            int packetType = fromServer.readInt();

            // Receiving encrypted nonce from Server.
            int encrypted_nonce_size = fromServer.readInt();
            byte [] encrypted_nonce_bytearray = new byte[encrypted_nonce_size];
            fromServer.readFully(encrypted_nonce_bytearray, 0, encrypted_nonce_size);
            String encrypted_nonce = Base64.getEncoder().encodeToString(encrypted_nonce_bytearray);
            System.out.println("Received Encrypted Nonce from Server: " + encrypted_nonce);

            // Receiving CA-signed certificate of Server's public key.
            int CA_signed_bytearray_size = fromServer.readInt();
            byte [] CA_signed_bytearray = new byte[CA_signed_bytearray_size];
            fromServer.readFully(CA_signed_bytearray, 0, CA_signed_bytearray_size);
            String CA_signed = new String(CA_signed_bytearray);
            // System.out.println("CA Signed Certification is: " + CA_signed);

            // Saving the CA-signed certificate into a physical file.
            String CAfile_fromServer_filepath = "Keys and Certificates\\CA_cert_from_Server.crt";
            File CAfile = new File(CAfile_fromServer_filepath);
            CA_fos = new FileOutputStream(CAfile);
            CA_fos.write(CA_signed_bytearray);

            // Client now has 2 FILES: (1) CA's Public Key Certificate. (2) Server's public key that is signed from CA.
            // Validating Server's cert; Verifying Server's cert with CA's public key; Extracting Server's public key
            PublicKey Server_PublicKey = ExtractPublicKeyFromCASignedCert.extract(CA_Cert_pubkey_filepath, CAfile_fromServer_filepath);
            System.out.println("Server's Public Key is: " + Server_PublicKey);

            // Prep Server's public key as use for deciphering object (to be used to decrypt the encrypted nonce).
            Cipher decipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            decipher.init(Cipher.DECRYPT_MODE, Server_PublicKey);

            // Decrypting encrypted nonce with Server's public key and verifying if it's the same as the nonce the Client sent at start,
            byte [] decrypted_nonce_bytearray = decipher.doFinal(encrypted_nonce_bytearray);
            String decrypted_nonce = new String(decrypted_nonce_bytearray);
            System.out.println("Decrypted Nonce from Server: " + decrypted_nonce);
            if (decrypted_nonce.equals(Integer.toString(nonce))){
                System.out.println("Nonce verified. Server authenticated.");
            }
            else{
                throw new IllegalStateException("Nonce do not tally. Do not trust Server.");
            }

            // Prepare Server's Public Key cipher (this will be used to encrypt the session key to be sent over to Server)
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

            System.out.println("Sending file...");

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

                String plaintext64 = Base64.getEncoder().encodeToString(fromFileBuffer);
                String ciphertext64 = Base64.getEncoder().encodeToString(ciphertext_bytearray);

//                System.out.println("Sending packet Number " + i + " of size " + numBytes);
//                System.out.println("Unencrypted chunk: " + plaintext64);
//                System.out.println("Encrypted chunk: " + ciphertext64);


                i++;
            }

            // Client waits for Server to receive all the files before closing. Client waits for Server to send over a message.
            while (fromServer.readInt() != 420){
                bufferedFileInputStream.close();
                fileInputStream.close();

                System.out.println("Closing connection...");
            }

        } catch (Exception e) {e.printStackTrace();}

        long timeTaken = System.nanoTime() - timeStarted;
        System.out.println("Program took: " + timeTaken/1000000.0 + "ms to run");
    }
}
