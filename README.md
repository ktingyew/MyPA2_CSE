# MyPA2_CSE
This is my PA2 submission. File transfer works for text files, and image files too.

## How to run?

Run the server first, then run the client after. If you run the client without the server alrady running, then the client will just terminate by itself.

For example, to try out the CP1 file transfer method, run `CP1Server.java`, followed by `CP1Client.java`.

Note that the place to specify the filepath of the file to be transferred is located somewhere near the top of the Client-side file.

## What's done.

Generation of the public-private key using OpenSSL - done. See `./Keys and Certificates` directory. 

Generation of CA-signed cert of the Server's public key - done. See `./Keys and Certificates` directory. 

CP1 and CP2 is done. They are the 4 `.java` files.

## What's left to do

What's not done is that both the client and server terminates after ONE file is transceived. Handout explicity says that program terminates upon request by user, not by successful file transfer.

A readme file: clear and succinct instructions of how to run your programs + **name of your pair.** **ONLY ONE person should submit.** 

A Submission Handout containing: 

- Specifications for the protocols AP, CPI1, and CPI2. Follow Fig. 1 for the format of your specifications.
  - AP stands for 'Authentication Protocol'. This is the nonce.
- Answers to questions posed in this handout as you read along
- Plots of achieved data throughput of CPI1 and CPI2 against a range of file sizes.




## What is the problem with the figure in the handout?
I think it's about nonce. Without it, there's the playback attack. T can impersonate S by simply playing back what S sends over to C. To elaborate, T will send over the digest (signed with Ks-) to C, along with the certificate signed by CA. C will perform the checks, and indeed thinks that T is indeed S. 

This is a huge problem if C sends unencrypted data over to T, thinking it's S.

How about if C encrypts the data before sending over? Well, T will not be able to read it, because she needs S's private key (Ks-) to decrypt it. But nevertheless, this is still a problem (although not as big) since C is getting bamboozled by sending data to an unintended recipient.

## To fix it

C should send a nonce over to S. 

S replies with Ks-(nonce), its cert signed by CA, and maybe some message (can be plaintext or encrypted with Ks-) .

C obtains CA's public key, use it to verify S's certificate, then extracts Ks+.

C uses Ks+ to decrypt Ks-(nonce) to get nonce, and compares it with what it sends earlier to S. If they are both the same, then there's no dirty work here.

C can trust the accompanying plain text sent by S. Or had S sent over encrypted text, then C can decrypt the text with Ks+, but at least now it can trust the contents of the decryption.

C can proceed with the handshake.















