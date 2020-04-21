import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.Base64;

public class CP2Server {

    public static void main(String[] args) {

        int port = 4321;
        if (args.length > 0) port = Integer.parseInt(args[0]);

        ServerSocket welcomeSocket = null;
        Socket connectionSocket = null;
        DataOutputStream toClient = null;
        DataInputStream fromClient = null;

        FileOutputStream fileOutputStream = null;
        BufferedOutputStream bufferedFileOutputStream = null;

        String CA_Signed_Cert_filepath = "C:\\Users\\kting\\Documents\\GitHub\\MyPA2_CSE\\Keys and Certificates\\CAsigned.crt";
        String server_PrivateKey_filepath = "C:\\Users\\kting\\Documents\\GitHub\\MyPA2_CSE\\Keys and Certificates\\private_key.der";

        try {
            welcomeSocket = new ServerSocket(port);
            connectionSocket = welcomeSocket.accept();
            fromClient = new DataInputStream(connectionSocket.getInputStream());
            toClient = new DataOutputStream(connectionSocket.getOutputStream());

            // Prep Server's Private Key Cipher Object for decrypting
            PrivateKey Server_PrivateKey = PrivateKeyReader.get(server_PrivateKey_filepath);
            Cipher decipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            decipher.init(Cipher.DECRYPT_MODE, Server_PrivateKey);

            // Prep Server's Private key Cipher Object for encrypting
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, Server_PrivateKey);

            // Prep the session key cipher first. Once Server obtain the session key from Client, then can perform init.
            Cipher aes_Cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");

            int i = 1;
            while (!connectionSocket.isClosed()) {

                int packetType = fromClient.readInt();

                // If the packet is for sending nonce by Client.
                if (packetType == 2){
                    System.out.println("Receiving nonce...");

                    int numBytes = fromClient.readInt();

                    byte [] nonce_bytearray = new byte[numBytes];
                    fromClient.readFully(nonce_bytearray, 0, numBytes);
                    String nonce = new String(nonce_bytearray);
                    System.out.println("Nonce from Client: " + nonce);

                    System.out.println("Encrypting Nonce with Private Key");
                    byte [] encrypted_nonce = cipher.doFinal(nonce_bytearray);
                    System.out.println("Encrypted Nonce is: " + Base64.getEncoder().encodeToString(encrypted_nonce));

                    // Server sends back the encrpyted nonce.
                    System.out.println("Sending Encrypted Nonce to Client.");
                    toClient.writeInt(0);
                    toClient.writeInt(encrypted_nonce.length);
                    toClient.write(encrypted_nonce);

                    // Server sends its CA-signed certificate which contain the Server's public key
                    byte[] CAsignedCert = Files.readAllBytes(Paths.get(CA_Signed_Cert_filepath));
                    toClient.writeInt(CAsignedCert.length);
                    toClient.write(CAsignedCert);

                }

                if (packetType == 3){
                    System.out.println("Receiving Encrypted Session Key (AES) from Client");
                    int numBytes = fromClient.readInt();
                    byte [] aeskey_bytearray = new byte[numBytes];
                    fromClient.readFully(aeskey_bytearray, 0, numBytes);
                    System.out.println(Base64.getEncoder().encodeToString(aeskey_bytearray));

                    // Decrypting the Encrypted Session Key (using Server's private key)
                    System.out.println("Decrypting Encrypted Session Key from Client");
                    byte [] plain_aeskey = decipher.doFinal(aeskey_bytearray);
                    System.out.println(Arrays.toString(plain_aeskey));
                    SecretKey aesKey = new SecretKeySpec(plain_aeskey, 0, plain_aeskey.length, "AES");

                    // Construct AES Session Key Cipher Object for decryption
                    aes_Cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
                    aes_Cipher.init(Cipher.DECRYPT_MODE, aesKey);
                }



                // If the packet is for transferring the filename sent by the client.
                else if (packetType == 0) {

                    System.out.println("Receiving file...");

                    int numBytes = fromClient.readInt();
                    byte [] filename = new byte[numBytes];

                    fromClient.readFully(filename, 0, numBytes);

                    fileOutputStream = new FileOutputStream("recv_"+new String(filename, 0, numBytes));
                    bufferedFileOutputStream = new BufferedOutputStream(fileOutputStream);

                }
                // If the packet is for transferring a chunk of the file
                else if (packetType == 1) {

                    // Read the size of this packet (in bytes). It should be 117, except for the last packet that might be smaller.
                    int numBytes = fromClient.readInt();


                    byte [] enc_block = new byte[128];
                    fromClient.readFully(enc_block);

                    System.out.println("Receiving packet " + i + " of size " + numBytes);

                    if (numBytes > 0){
                        byte [] dec_byte = aes_Cipher.doFinal(enc_block);
                        bufferedFileOutputStream.write(dec_byte, 0, numBytes);


//                        String ciphertext64 = Base64.getEncoder().encodeToString(enc_block);
//                        String plaintext64 = Base64.getEncoder().encodeToString(dec_byte);
//                        System.out.println("Received Encrypted chunk: " + ciphertext64);
//                        System.out.println("Unencrypted chunk: " + plaintext64);


                    }

                    if (numBytes < 117) {
                        toClient.writeInt(420);
                        System.out.println("Closing connection...");

                        if (bufferedFileOutputStream != null) bufferedFileOutputStream.close();
                        if (bufferedFileOutputStream != null) fileOutputStream.close();
                        fromClient.close();
                        toClient.close();
                        connectionSocket.close();
                    }
                    i++;
                }

            }
        } catch (Exception e) {e.printStackTrace();}

    }

}
