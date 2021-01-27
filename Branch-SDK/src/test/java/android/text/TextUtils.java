package android.text;

/**
 * All android methods don't exist when running unit tests, so we have to manually provide the
 * implementation ourselves.
 */
public class TextUtils {
    public static boolean isEmpty(CharSequence str) {
        return str == null || str.length() == 0;
    }
}
