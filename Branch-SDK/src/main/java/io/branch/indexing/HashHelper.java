package io.branch.indexing;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

class HashHelper {

    MessageDigest messageDigest_;

    HashHelper() {
        try {
            messageDigest_ = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ignore) {
        }
    }

    String hashContent(String content) {
        String hashedVal = "";
        if (messageDigest_ != null) {
            messageDigest_.reset();
            messageDigest_.update(content.getBytes());
            // No need to worry about char set here since CD use this only to check uniqueness
            hashedVal = new String(messageDigest_.digest());
        }
        return hashedVal;
    }
}
