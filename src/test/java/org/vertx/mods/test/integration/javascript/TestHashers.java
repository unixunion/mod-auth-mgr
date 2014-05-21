package org.vertx.mods.test.integration.javascript;

/**
 * Created by keghol on 5/21/14.
 */

import org.junit.Assert;
import org.junit.Test;
import org.vertx.mods.BCrypt;
//import sun.misc.BASE64Decoder;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import static org.junit.Assert.*;

public class TestHashers {

    @Test
    public void testBCGenHash() {
        long startTime = System.currentTimeMillis();
        String hashed = BCrypt.hashpw("somepassword", BCrypt.gensalt(4));
        long endTime = System.currentTimeMillis();
        long timeEnded =  (endTime-startTime);
        System.out.println("BC hashing took: " + timeEnded + " string: " + hashed);
    }


    @Test
    public void testGenString() {
        long startTime = System.currentTimeMillis();
        String somestring = new String("somepassword");
        long endTime = System.currentTimeMillis();
        long timeEnded =  (endTime-startTime);
        System.out.println("string took: " + timeEnded + " string: " + somestring);
    }

    @Test
    public void testPHGenHash() throws InvalidKeySpecException, NoSuchAlgorithmException {
        long startTime = System.currentTimeMillis();
        String hashed = PasswordHash.createHash("somepassword");
        long endTime = System.currentTimeMillis();
        long timeEnded =  (endTime-startTime);
        System.out.println("PH hashing took: " + timeEnded + " string: " + hashed);
    }


    @Test
    public void testDigestGenHash() throws UnsupportedEncodingException, NoSuchAlgorithmException {
        long startTime = System.currentTimeMillis();
        String hashed = getHash("somepassword").toString();
        long endTime = System.currentTimeMillis();
        long timeEnded =  (endTime-startTime);
        System.out.println("DG hashing took: " + timeEnded + " string: " + hashed);
    }

    @Test
    public void testDigestGenHash2() throws UnsupportedEncodingException, NoSuchAlgorithmException {
        long startTime = System.currentTimeMillis();
        String hashed = getHash2("somepassword").toString();
        long endTime = System.currentTimeMillis();
        long timeEnded =  (endTime-startTime);
        System.out.println("DG hashing took: " + timeEnded + " string: " + hashed);
    }

    @Test
    public void testDigestGenHash3() throws UnsupportedEncodingException, NoSuchAlgorithmException {
        long startTime = System.currentTimeMillis();
        String hashed = getHash3("somepassword").toString();
        long endTime = System.currentTimeMillis();
        long timeEnded =  (endTime-startTime);
        System.out.println("DG hashing took: " + timeEnded + " string: " + hashed);
    }


    public byte[] getHash(String password) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        digest.reset();
        byte[] input = digest.digest(password.getBytes("UTF-8"));
        return input;
    }

    public String getHash2(String password) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        digest.reset();
        byte[] input = digest.digest(password.getBytes("UTF-8"));

        //convert the byte to hex format method 1
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < input.length; i++) {
            sb.append(Integer.toString((input[i] & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }

    public String getHash3(String password) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        digest.reset();
        byte[] input = digest.digest(password.getBytes("UTF-8"));

        //convert the byte to hex format method 2
        StringBuffer hexString = new StringBuffer();
        for (int i=0;i<input.length;i++) {
            hexString.append(Integer.toHexString(0xFF & input[i]));
        }

        return hexString.toString();
    }


}
