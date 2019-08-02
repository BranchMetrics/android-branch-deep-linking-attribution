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
                94, -17, -65, -67, 59, -17, -65, -67, -17, -65, -67, 30, -17, -65, -67, -48, -109, -17, -65, -67, 34, -17, -65, -67, -17, -65, -67, 90, -17, -65, -67, -17, -65, -67
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
                -17, -65, -67, 77, 39, -17, -65, -67, -17, -65, -67, 77, 62, 8, -17, -65, -67, 46, 82, -17, -65, -67, -17, -65, -67, 125, -17, -65, -67, -17, -65, -67, -60, -124, -17, -65, -67, -17, -65, -67, 122, 83, -17, -65, -67, -18, -112, -120, -17, -65, -67, -17, -65, -67, -17, -65, -67, -17, -65, -67, -17, -65, -67, -17, -65, -67
        };
        String expected = new String(hash);

        HashHelper helper = new HashHelper();
        String actual = helper.hashContent("hello world");

        // verify the hash is SHA256
        Assert.assertTrue(expected.equals(actual));
    }

}
