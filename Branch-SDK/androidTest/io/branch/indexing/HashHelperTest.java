package io.branch.indexing;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class HashHelperTest {

    @Before
    public void setUp() {

    }

    @After
    public void tearDown() {

    }

    @Test
    public void testHelloWoldMD5() {
        byte[] hash = new byte[] {
                94, -74, 59, -69, -32, 30, -18, -48, -109, -53, 34, -69, -113, 90, -51, -61
        };
        String expected = new String(hash);

        HashHelper helper = new HashHelper();
        String actual = helper.hashContent("hello world");

        // verify the hash is not MD5
        Assert.assertFalse(expected.equals(actual));
    }

    @Test
    public void testHelloWorldSHA256() {
        byte[] hash = new byte[] {
                -71, 77, 39, -71, -109, 77, 62, 8, -91, 46, 82, -41, -38, 125, -85, -6, -60, -124, -17, -29, 122, 83, -128, -18, -112, -120, -9, -84, -30, -17, -51, -23
        };
        String expected = new String(hash);

        HashHelper helper = new HashHelper();
        String actual = helper.hashContent("hello world");

        // verify the hash is SHA256
        Assert.assertTrue(expected.equals(actual));
    }

}
