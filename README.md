# MyPA2_CSE
This is my PA2 submission.

## What is the problem?
I think it's about nonce. Without it, there's the playback attack. T can impersonate S by simply playing back what S sends over to C. To elaborate, T will send over the digest (signed with Ks-) to C, along with the certificate signed by CA. C will perform the checks, and indeed thinks that T is indeed S. 

This is a huge problem is C sends unencrypted data over to T, think it's S.

How about if C encrypts the data before sending over? Well, T will not be able to read it, because she needs S's private key (Ks-) to decrypt it. But nevertheless, this is still a problem (although not as big) since C is getting bamboozled by sending data to an unintended recipient.

## To fix it

C should send a nonce over to S. 

S replies with Ks-(nonce), its cert signed by CA, and maybe some message (can be plaintext or encrypted with Ks-) .

C obtains CA's public key, use it to verify S's certificate, then extracts Ks+.

C uses Ks+ to decrypt Ks-(nonce) to get nonce, and compares it with what it sends earlier to S. If they are both the same, then there's no dirty work here.

C can trust the accompanying plain text sent by S. Or had S sent over encrypted text, then C can decrypt the text with Ks+, but at least now it can trust the contents of the decryption.

C can proceed with the handshake.







