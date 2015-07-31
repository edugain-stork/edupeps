package umu.eadmin.servicios.umu2stork;

/*
 * Copyright (C) 2015 Jordi Ortiz      (jordi.ortiz@um.es)
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

import javax.servlet.ServletException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.Provider;
import java.security.PrivateKey;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.logging.Logger;

import javax.crypto.Cipher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import sun.misc.BASE64Decoder;

import org.bouncycastle.util.encoders.Base64;


/**
 * @author Jordi Ortiz <jordi.ortiz@um.es>
 *
 */
class UtilesRsa {
    private final static Logger logger = Logger.getLogger(umu.eadmin.servicios.umu2stork.UtilesRsa.class.getName());

    PrivateKey pk;

    /**
     *
     */
    public UtilesRsa() {
        // add the security provider
        BouncyCastleProvider bcp = new BouncyCastleProvider();
        java.security.Security.addProvider((Provider)bcp);
        pk = null;
    }
	
    public PrivateKey readPrivateKey(String filename) throws IOException
	{
            try {
                logger.info("Start decoding RSA key in PEM format: " + filename);
                File keyFile = new File(filename);

                BufferedReader br = new BufferedReader(new FileReader(keyFile));
                StringBuffer keyBase64 = new StringBuffer();
                String line = br.readLine ();
                while(line != null) {
                    if ( !(line.startsWith("-----BEGIN")) && !(line.startsWith("-----END"))) {
                        keyBase64.append (line);
                    }
                    line = br.readLine ();
                }
                br.close();
                logger.info("Decode RSA Key");
                byte [] fileBytes = new BASE64Decoder().decodeBuffer (keyBase64.toString ());

                PKCS8EncodedKeySpec ks = new PKCS8EncodedKeySpec(fileBytes);
                KeyFactory kf = KeyFactory.getInstance("RSA");
			
                logger.info("RSA Decode End: " + filename);
                pk = kf.generatePrivate(ks);
                return (pk);
            } catch (InvalidKeySpecException ex1) {
                throw new IOException("Invalid Key Spec: " + ex1.toString());
            } catch (NoSuchAlgorithmException ex2) {
                throw new IOException("No Such Algorithm: " + ex2.toString());
            }	
	}
	
    public String encode(String data) throws javax.servlet.ServletException
	{
            String output = "";
            if (pk == null)
                throw new ServletException("Private ciphering key does not exist!");
            try {
                logger.info("RSA encoder load");
                Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                logger.info("RSA encoder init");
                rsaCipher.init(Cipher.ENCRYPT_MODE,pk);
                int inputSize = data.length();
                int inputBlockSize = rsaCipher.getOutputSize(1);
                int numBlocks = inputSize / inputBlockSize;
                logger.info("#Blocks: " + numBlocks);
                int resto = inputSize % inputBlockSize;

                byte[][] rawOutput = null;
                if (resto != 0)
                    rawOutput = new byte[numBlocks+1][];
                else
                    rawOutput = new byte[numBlocks][];

                int blockCount = 0;
                while (blockCount < numBlocks) {
                    int index = blockCount*inputBlockSize;
                    rawOutput[blockCount] = rsaCipher.doFinal(data.getBytes(), index, inputBlockSize);
                    logger.info("Block encoded: " + new String(rawOutput[blockCount]));
                    blockCount++;
                }


                if (resto != 0) {
                    numBlocks++;
                    int index = blockCount * inputBlockSize;
                    rawOutput[blockCount] = rsaCipher.doFinal(data.getBytes(), index, resto);
                }

                int totalSize = 0;
                for (int i=0; i<numBlocks; i++)
                    totalSize += rawOutput[i].length;

                byte[] fullOutput = new byte[totalSize];

                int count = 0;
                for (int i = 0; i < numBlocks; i++) {
                    int blockSize = rawOutput[i].length;
                    for (int j = 0; j<blockSize; j++) {
                        fullOutput[count] = rawOutput[i][j];
                        count++;
                    }
                }

                byte[] encoded64Data = Base64.encode(fullOutput);

                output = new String(encoded64Data);
                logger.info("RSA Encoding ends");

            }
            catch (Exception e) {
                logger.warning("Encoding aborted due Exception: "+e);
                output = "";
            }
            return output;
	}
	
    public String decode (String data) throws ServletException
        {
            logger.info("Decoding started : "+data);
            if (pk == null)
    		throw new ServletException("No ciphering key found");
            String output = "";
            try {
    		logger.info("RSA decoder load");
    		Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
    		rsaCipher.init(Cipher.DECRYPT_MODE,pk);
    		byte[] decodedData = new org.apache.commons.codec.binary.Base64().decode(data);
    		int inputSize = decodedData.length;
    		int inputBlockSize = rsaCipher.getOutputSize(1);
    		int numBlocks = inputSize / inputBlockSize;
    		logger.info("#Blocks: "+ numBlocks);
    		int resto = inputSize % inputBlockSize;

    		byte[][] rawOutput = null;
    		if (resto != 0)
                    rawOutput = new byte[numBlocks+1][];
    		else
                    rawOutput = new byte[numBlocks][];

    		int blockCount = 0;
    		while (blockCount < numBlocks) {
                    int index = blockCount*inputBlockSize;
                    rawOutput[blockCount] = rsaCipher.doFinal(decodedData, index, inputBlockSize);
                    logger.info("Decoded block: "+new String(rawOutput[blockCount]));
                    blockCount++;
    		}


    		if (resto != 0) {
                    numBlocks++;
                    int index = blockCount*inputBlockSize;
                    rawOutput[blockCount] = rsaCipher.doFinal(decodedData, index, resto);
    		}

    		int totalSize = 0;
    		for (int i=0; i<numBlocks;i++)
                    totalSize += rawOutput[i].length;

    		byte[] fullOutput = new byte[totalSize];

    		int count =0;
    		for (int i=0; i<numBlocks;i++) {
                    int blockSize = rawOutput[i].length;
                    for (int j=0; j<blockSize; j++) {
                        fullOutput[count] = rawOutput[i][j];
                        count++;
                    }
    		}

    		output = new String(fullOutput);
    		logger.info("RSA Decoding ends");

            }
            catch (Exception e) {
    		logger.warning("RSA Decoding aborted due Exception : "+e);
    		output = "";
            }

            return output;
        }

}
