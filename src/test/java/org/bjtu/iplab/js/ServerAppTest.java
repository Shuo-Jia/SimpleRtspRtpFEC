package org.bjtu.iplab.js;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Unit test for simple Server.
 */
public class ServerAppTest {
    /**
     * Rigorous Test :-)
     */
    public static void main(String[] args) {
        Map<Integer, String> map = new HashMap<>();
        int a = 1;
        while (true) {
            String b = new String(String.valueOf(a));
            map.put(a, b);
            a++;
        }
    }
}
