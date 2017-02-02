package okcoin_balancedvolume_bot;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MD5Util {

    /**
     * Generate signature results (new version used)
     *
     * @param sArray The array to be signed
     * @param secretKey
     * @return The signature result string
     */
    public static String buildMysignV1(Map<String, String> sArray,
            String secretKey) {
        String mysign = "";
        try {
            String prestr = createLinkString(sArray); // The array of all the elements, in accordance with the "parameter = parameter value" mode with "&" character stitching into a string
            prestr = prestr + "&secret_key=" + secretKey; // The stitching of the string and then check the security code to link up
            mysign = getMD5(prestr);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //System.out.println("Sign=" + mysign);
        return mysign;
    }

    /**
     * All the elements of the array sort, and in accordance with the "parameter
     * = parameter value" mode with "&" character stitching into a string
     *
     * @param params Parameter groups that need sorting and participating in
     * character stitching
     * @return The stitching string
     */
    public static String createLinkString(Map<String, String> params) {

        List<String> keys = new ArrayList<String>(params.keySet());
        Collections.sort(keys);
        String prestr = "";
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            String value = params.get(key);
            if (i == keys.size() - 1) {// Stitching does not include the last & character
                prestr = prestr + key + "=" + value;
            } else {
                prestr = prestr + key + "=" + value + "&";
            }
        }
        return prestr;
    }

    /**
     * 生成32位大写MD5值
     */
    private static final char HEX_DIGITS[] = {'0', '1', '2', '3', '4', '5',
        '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    public static String getMD5(String input) {
        byte[] source;
        try {
            //Get byte according by specified coding.
            source = input.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            source = input.getBytes();
        }
        String result = null;
        char hexDigits[] = {'0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(source);
            //The result should be one 128 integer
            byte temp[] = md.digest();
            char str[] = new char[16 * 2];
            int k = 0;
            for (int i = 0; i < 16; i++) {
                byte byte0 = temp[i];
                str[k++] = hexDigits[byte0 >>> 4 & 0xf];
                str[k++] = hexDigits[byte0 & 0xf];
            }
            result = new String(str);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    //public static void main(String[] args) throws NoSuchAlgorithmException {
      //  System.out.println(getMD5("Javarmi.com"));
 //   }
}
