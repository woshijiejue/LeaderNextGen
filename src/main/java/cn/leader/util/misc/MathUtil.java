package cn.leader.util.misc;

import java.security.SecureRandom;

public final class MathUtil {

    public static float getRandomInRange(float min, float max) {
        SecureRandom random = new SecureRandom();
        return random.nextFloat() * (max - min) + min;
    }

    public static long getRandomInRange(long min, long max) {
        SecureRandom random = new SecureRandom();
        return (long) (random.nextFloat() * (max - min) + min);
    }

    private MathUtil() {
    }
}
