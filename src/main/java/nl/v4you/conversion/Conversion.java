package nl.v4you.conversion;

public class Conversion {
    // Modelled after SmallFloat.floatToByte from the Lucene core library
    // mantissa=11 bits and zeroExp=20
    // result is an int where the 2 least significant bytes contain the result
    // range of this float is 9.5414E-7 to 4095.0
    public static int floatToInt1120(float f) {
    int fzero = (127-20)<<11;
    int bits = Float.floatToRawIntBits(f);
    int smallfloat = bits >> (23-11);
    if (smallfloat <= fzero) {
            return (bits<=0) ? 0 : 1;
        } else if (smallfloat > fzero + 0xffff) {
            return 0xffff;
        } else {
            return (smallfloat - fzero);
        }
    }

    // Modelled after SmallFloat.byteToFloat from the Lucene core library
    // mantissa=11 bits and zeroExp=20
    // reverses the conversion as done by floatToInt1115
    public static float int1120ToFloat(int b) {
        if (b == 0) return 0.0f;
        int bits = (b & 0xffff) << (23-11);
        bits += (127-20) << 23;
        return Float.intBitsToFloat(bits);
    }
}
