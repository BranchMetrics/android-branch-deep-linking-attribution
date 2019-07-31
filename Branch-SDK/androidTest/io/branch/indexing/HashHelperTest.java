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
        String expected = "^�;��\u001E�Г�\"��Z��";
        HashHelper helper = new HashHelper();
        String actual = helper.hashContent("hello world");

        // verify the hash is not MD5
        Assert.assertFalse(expected.equals(actual));
    }

    @Test
    public void testHelloWorldSHA256() {
        String expected = "�M'��M>\b�.R��}��Ą��zS�\uE408������";
        HashHelper helper = new HashHelper();
        String actual = helper.hashContent("hello world");

        // verify the hash is SHA256
        Assert.assertTrue(expected.equals(actual));
    }

}
