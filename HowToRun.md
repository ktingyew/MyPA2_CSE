# Programming Assignment 2

## Secure File Transfer

### Koh Ting Yew , Davis - 1003339

### Doreen Ng -  1003837



# How to Run

## To test out AP + CP1
1. Open up CP1Client.java
2. Specify the filepath of the file that is to be transferred with the variable name "filename".
3. Specify the filepath of the public key of the CA certificate (cacse.crt) file through the variable "CA_Cert_pubkey_filepath".
4. Specify the filepath of where to save the CA-signed certificate file using the variable "CAfile_fromServer_filepath".
5. Open up CP1Server.java
6. Specify the filepath of the CA-signed certificate using the variable "CA_Signed_Cert_filepath".
7. Specify the filepath of the private key file using the variable "server_PrivateKey_filepath".
8. Save the two files CP1Client.java and CP1Server.java and build the files.
9. Run CP1Server.java and wait for "CP1Server" to be printed out in the terminal.
10. Then run CP1Client.java.

## To test out AP + CP2
1. Repeat steps 1 to 10 as written above, but for files CP2Client.java and CP2Server.java


