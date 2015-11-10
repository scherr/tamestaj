package tamestaj.util;

@SuppressWarnings("unused")
public final class Forcing {
    private Forcing() { }

    public static <V> V force(V value) { return value; }
    public static boolean force(boolean value) { return value; }
    public static int force(int value) { return value; }
    public static long force(long value) { return value; }
    public static float force(float value) { return value; }
    public static double force(double value) { return value; }
    public static byte force(byte value) { return value; }
    public static char force(char value) { return value; }
    public static short force(short value) { return value; }
}
