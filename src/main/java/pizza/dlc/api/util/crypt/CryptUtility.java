package pizza.dlc.api.util.crypt;

import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import pizza.dlc.PizzaDLC;

public final class CryptUtility {
   public static byte[] decryptData(byte[] encryptedData, String password) {
      try {
         if (encryptedData.length < 32) {
            throw new IllegalArgumentException("Invalid input");
         }

         byte[] salt = Arrays.copyOfRange(encryptedData, 0, 16);
         byte[] iv = Arrays.copyOfRange(encryptedData, 16, 32);
         byte[] ciphertext = Arrays.copyOfRange(encryptedData, 32, encryptedData.length);
         SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
         KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 128);
         SecretKey tmp = factory.generateSecret(spec);
         SecretKeySpec secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");
         Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
         cipher.init(2, secretKey, new IvParameterSpec(iv));
         return cipher.doFinal(ciphertext);
      } catch (Exception ex) {
         PizzaDLC.LOGGER.error("Failed to decrypt data", ex);
         return null;
      }
   }

   public static byte[] encryptData(byte[] data, String password) {
      try {
         SecureRandom random = new SecureRandom();
         byte[] salt = new byte[16];
         random.nextBytes(salt);
         SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
         KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 128);
         SecretKey tmp = factory.generateSecret(spec);
         SecretKeySpec secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");
         byte[] iv = new byte[16];
         random.nextBytes(iv);
         IvParameterSpec ivSpec = new IvParameterSpec(iv);
         Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
         cipher.init(1, secretKey, ivSpec);
         byte[] encrypted = cipher.doFinal(data);
         ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
         outputStream.write(salt);
         outputStream.write(iv);
         outputStream.write(encrypted);
         return outputStream.toByteArray();
      } catch (Throwable ex) {
         throw new RuntimeException(ex);
      }
   }

   private CryptUtility() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
