/**
 * 
 */
package umu.eadmin.servicios.umu2stork;

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
			logger.info("Inicio da decodificación da clave RSA en formato PEM de: " + filename);
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
			logger.info("Decodificando a clave RSA");
			byte [] fileBytes = new BASE64Decoder().decodeBuffer (keyBase64.toString ());

			PKCS8EncodedKeySpec ks = new PKCS8EncodedKeySpec(fileBytes);
			KeyFactory kf = KeyFactory.getInstance("RSA");
			
			logger.info("Fin de decodificación de clave RSA en formato PEM de: " + filename);
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
			throw new ServletException("No existe clave privada para cifrar");
		try {
			logger.info("Cargando codificador RSA");
			Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			logger.info("Cargado codificador RSA");
			logger.info("Inicializando codificador RSA");
			rsaCipher.init(Cipher.ENCRYPT_MODE,pk);
			logger.info("Inicializado codificador RSA");
			logger.info("Codificando datos");
			logger.info("Datos decodificados");
			int inputSize = data.length();
			int inputBlockSize = rsaCipher.getOutputSize(1);
			int numBlocks = inputSize / inputBlockSize;
			logger.info("Número de bloques: "+ numBlocks);
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
				logger.info("Codificado bloque: "+new String(rawOutput[blockCount]));
				blockCount++;
			}


			if (resto != 0) {
				numBlocks++;
				int index = blockCount*inputBlockSize;
				rawOutput[blockCount] = rsaCipher.doFinal(data.getBytes(), index, resto);
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

			byte[] encoded64Data = Base64.encode(fullOutput);

			output = new String(encoded64Data);
			logger.info("Finalizada codificación");

		}
		catch (Exception e) {
			logger.warning("Abortada codificación por excepción: "+e);
			output = "";
		}
		return output;
	}
	
    public String decode (String data) throws ServletException 
    {
    	logger.info("Iniciando decodificaci de: "+data);
    	if (pk == null)
    		throw new ServletException("No existe clave privada para cifrar");
    	String output = "";
    	try {
    		logger.info("Cargando decodificador RSA");
    		Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
    		logger.info("Cargado decodificador RSA");
    		logger.info("Inicializando decodificador RSA");
    		rsaCipher.init(Cipher.DECRYPT_MODE,pk);
    		logger.info("Inicializado decodificador RSA");
    		logger.info("Decodificando datos");
    		byte[] decodedData = new org.apache.commons.codec.binary.Base64().decode(data);
    		logger.info("Datos decodificados");
    		int inputSize = decodedData.length;
    		int inputBlockSize = rsaCipher.getOutputSize(1);
    		int numBlocks = inputSize / inputBlockSize;
    		logger.info("Número de bloques: "+ numBlocks);
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
    			logger.info("Decodificado bloque: "+new String(rawOutput[blockCount]));
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
    		logger.info("Finalizada decodificaci");

    	}
    	catch (Exception e) {
    		logger.warning("Abortada decodificaci por excepci: "+e);
    		output = "";
    	}

    return output;
    }

}
