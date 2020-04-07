import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.net.Socket;

public class ClientWithoutSecurity {

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

		try {

			System.out.println("Establishing connection to server...");

			// Connect to server and get the input and output streams
			clientSocket = new Socket(serverAddress, port);
			toServer = new DataOutputStream(clientSocket.getOutputStream());
			fromServer = new DataInputStream(clientSocket.getInputStream()); // Not used because we shouldn't be receiving any input from Server. HMM.

			System.out.println("Sending file...");

			// Send a nonce over
			String nonce = "This is my nonce!";
			toServer.writeInt(2);
			toServer.writeInt(nonce.getBytes().length);
			toServer.write(nonce.getBytes());



			// Send the filename
			toServer.writeInt(0); // This sends a packet that just contains the value '0'. It tells the Server that the very next packet will contain the name of the file to be sent over.
			toServer.writeInt(filename.getBytes().length); // Send the length of the file name over in another packet.
			toServer.write(filename.getBytes()); // Send the actual file name itself, in yet another packet.
			//toServer.flush();

			// Open the file
			fileInputStream = new FileInputStream(filename);
			bufferedFileInputStream = new BufferedInputStream(fileInputStream);

	        byte [] fromFileBuffer = new byte[117]; // Why 117? I'm not sure.

	        // Send the file
	        for (boolean fileEnded = false; !fileEnded;) {
				numBytes = bufferedFileInputStream.read(fromFileBuffer);
				fileEnded = numBytes < 117;

				// NOTE: File is broken down into chunks of 117 bytes big. We send each chunk as a packet to Server. But, for every chunk-packet we send over,
				//  we send 2 preceding packets. So, each chunk of file is transmitted in groups of 3 packets. The first packet identifies that Client will be
				//  sending over a packet-chunk. The second packet is the length of chunk of file being sent over (it's 117 for the most part, except probably
				// for the last chunk. The 3rd packet is the actual chunk of file itself.
				toServer.writeInt(1); // When Server receives this '1', Server will know that the subsequent packet is the length of the chunk of file.
				toServer.writeInt(numBytes); // Server is coded to read this 'numbytes'. So that it will know how big the next packet will be (which actually contains the chunk of the file).
				toServer.write(fromFileBuffer); // This is where Client actually sends the chunk of file in a packet.
				toServer.flush();
			}

	        bufferedFileInputStream.close();
	        fileInputStream.close();

			System.out.println("Closing connection...");

		} catch (Exception e) {e.printStackTrace();}

		long timeTaken = System.nanoTime() - timeStarted;
		System.out.println("Program took: " + timeTaken/1000000.0 + "ms to run");
	}
}
