import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerWithoutSecurity {

	public static void main(String[] args) {

    	int port = 4321;
    	if (args.length > 0) port = Integer.parseInt(args[0]);

		ServerSocket welcomeSocket = null;
		Socket connectionSocket = null;
		DataOutputStream toClient = null;
		DataInputStream fromClient = null;

		FileOutputStream fileOutputStream = null;
		BufferedOutputStream bufferedFileOutputStream = null;

		try {
			welcomeSocket = new ServerSocket(port);
			connectionSocket = welcomeSocket.accept();
			fromClient = new DataInputStream(connectionSocket.getInputStream());
			toClient = new DataOutputStream(connectionSocket.getOutputStream());

			while (!connectionSocket.isClosed()) {

				int packetType = fromClient.readInt();

				// If the packet is for sending nonce by Client.
				if (packetType == 2){
					System.out.println("Receiving nonce...");

					int numBytes = fromClient.readInt();

					byte [] nonce_bytearray = new byte[numBytes];
					fromClient.readFully(nonce_bytearray, 0, numBytes);
					String nonce = new String(nonce_bytearray);

					System.out.println("Nonce is: " + nonce);

					String encrypted_nonce = nonce + " plus some encryption";
					System.out.println("Encrypted Nonce is: " + encrypted_nonce);

					

				}


				// If the packet is for transferring the filename sent by the client.
				else if (packetType == 0) {
					// Client is coded s.t. of all the packets it sends over, the first packet is of type == 0. All subsequent packets are of type == 1.

					System.out.println("Receiving file...");

					int numBytes = fromClient.readInt(); // Server expects Client to send over a packet describing the length of the filename over. If client is intending to transmit the file 100.txt, then numBytes is the length of '100.txt', i.e. 7.
					byte [] filename = new byte[numBytes]; // Create a byte array of numBytes length. Following example, size is 7 bytes.
					// Nat: Must use readFully()!
					// Nat: See: https://stackoverflow.com/questions/25897627/datainputstream-read-vs-datainputstream-readfully
					// Server expects that client will send over another packet containing the name of the file itself.
					//  Subsequent line of code is such that it reads exactly numBytes bytes from this incoming packet
					//  (i.e. the entirety of the name of the file). and then stores it into the byte array 'filename'.
					fromClient.readFully(filename, 0, numBytes);

					// Names the output file.
					fileOutputStream = new FileOutputStream("recv_"+new String(filename, 0, numBytes));
					// Generate the output file.
					bufferedFileOutputStream = new BufferedOutputStream(fileOutputStream);

				}
				// If the packet is for transferring a chunk of the file
				else if (packetType == 1) {

					// Read the size of this packet (in bytes). It should be 117, except for the last packet that might be smaller.
					int numBytes = fromClient.readInt();

					// Reads all 117 bytes of the packet and stores the content in the byte array 'block'.
					byte [] block = new byte[numBytes];
					fromClient.readFully(block, 0, numBytes);

					// If there is non-zero bytes of content in byte array 'block', then write it to the output file.
					if (numBytes > 0)
						bufferedFileOutputStream.write(block, 0, numBytes);

					// All intermediate packets have sisze of 117 bytes. If we detect a packet whose size is less than 117, means
					//  we have already just written the contents of the last packet into the output file, and it's time to close
					//  the connection.
					if (numBytes < 117) {
						System.out.println("Closing connection...");

						if (bufferedFileOutputStream != null) bufferedFileOutputStream.close();
						if (bufferedFileOutputStream != null) fileOutputStream.close();
						fromClient.close();
						toClient.close();
						connectionSocket.close();
					}
				}

			}
		} catch (Exception e) {e.printStackTrace();}

	}

}
