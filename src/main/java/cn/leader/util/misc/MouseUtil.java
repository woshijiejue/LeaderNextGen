package cn.leader.util.misc;


public final class MouseUtil {
    public static boolean isHovered(float mouseX, float mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private MouseUtil() {
    }
}
