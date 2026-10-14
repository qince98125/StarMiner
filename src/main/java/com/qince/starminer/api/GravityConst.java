package com.qince.starminer.api;

/** Constants shared by the gravity API. */
public final class GravityConst {
    /** Axis-tie tolerance used to stop gravity direction flicker near a field boundary. */
    public static final double AXIS_BUFFER_WIDTH = 1.5D;

    /** The five gravity-field shapes a Gravity Core can project. */
    public static final int FIELD_TYPE_SPHERE = 0;
    public static final int FIELD_TYPE_SQUARE = 1;
    public static final int FIELD_TYPE_X_CYLINDER = 2;
    public static final int FIELD_TYPE_Y_CYLINDER = 3;
    public static final int FIELD_TYPE_Z_CYLINDER = 4;
    public static final int FIELD_TYPE_COUNT = 5;

    public static final String[] FIELD_TYPE_NAMES = {"sph", "cub", "xcyl", "ycyl", "zcyl"};

    private GravityConst() {
    }

    public static int clampFieldType(int type) {
        return type < 0 || type >= FIELD_TYPE_COUNT ? FIELD_TYPE_SPHERE : type;
    }

    public static String fieldTypeTranslationKey(int type) {
        return "starInfo.type." + FIELD_TYPE_NAMES[clampFieldType(type)];
    }
}
