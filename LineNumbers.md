1. ### On the client side CP1:

   1. Get server.crt from server: `fromServer.readFully(CA_signed_bytearray, 0, CA_signed_bytearray_size);` 

      (line 77)

   2. Verify (and decrypt) the server.crt using CA cert (cacse.crt): `PublicKey Server_PublicKey = ExtractPublicKeyFromCASignedCert.extract(CA_Cert_pubkey_filepath, CAfile_fromServer_filepath);` 

      (line 89)

   3. Extract server's public key from the certificate: `PublicKey Server_PublicKey = ExtractPublicKeyFromCASignedCert.extract(CA_Cert_pubkey_filepath, CAfile_fromServer_filepath);`

      (line 89)

   4. Encrypt file chunks with server’s public key: `byte[] ciphertext_bytearray = cipher.doFinal(fromFileBuffer);` 

      (line 136)

2. ### On the client side CP2:

   1. Generate symmetric key: `KeyGenerator keyGen = KeyGenerator.getInstance("AES");`

      (line 114)

   2. Send symmetric key to server (encrypted with server’s public key): 

      1. `byte [] encrypted_desKey = rsa_cipher.doFinal(desKey_bytearray);` 

         (line 124)

      2. `toServer.write(encrypted_desKey);`

         (line 129)

   3. Encrypt file chunk with symmetric key: `byte[] ciphertext_bytearray = aes_Cipher.doFinal(fromFileBuffer);`

      (line 159)

3. ### On the server side CP1:

   1. Sending of server certificate to the client: `toClient.write(CAsignedCert);`

      (line 75)

   2. Encrypt nonce with private key: `byte [] encrypted_nonce = cipher.doFinal(nonce_bytearray);`

      (line 63)

   3. Decrypt file chunks with private key: `byte [] dec_byte = decipher.doFinal(enc_block);`

      (line 107)

4. ### On the server side CP2:

   1. Decrypt symmetric key with private key: `byte [] plain_aeskey = decipher.doFinal(aeskey_bytearray);`

      (line 94)

   2. Decrypt file chunks with symmetric key: `byte [] dec_byte = aes_Cipher.doFinal(enc_block);`

      (line 132)





