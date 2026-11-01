package com.ktg.system.face;

/**
 * 一次 1:1 人脸比对的结论。
 *
 * reason 只用于服务端日志与排障，不直接回给前端，避免向未登录者透露
 * "该账号是否录入过人脸"这类信息。
 */
public class FaceMatchResult {

    public static final String MATCHED = "MATCHED";
    public static final String NOT_MATCHED = "NOT_MATCHED";
    public static final String NOT_ENROLLED = "NOT_ENROLLED";
    public static final String INVALID_TEMPLATE = "INVALID_TEMPLATE";

    private final boolean matched;
    private final double distance;
    private final String reason;

    private FaceMatchResult(boolean matched, double distance, String reason) {
        this.matched = matched;
        this.distance = distance;
        this.reason = reason;
    }

    public static FaceMatchResult matched(double distance) {
        return new FaceMatchResult(true, distance, MATCHED);
    }

    public static FaceMatchResult notMatched(double distance) {
        return new FaceMatchResult(false, distance, NOT_MATCHED);
    }

    public static FaceMatchResult notEnrolled() {
        return new FaceMatchResult(false, Double.NaN, NOT_ENROLLED);
    }

    public static FaceMatchResult invalidTemplate() {
        return new FaceMatchResult(false, Double.NaN, INVALID_TEMPLATE);
    }

    public boolean isMatched() {
        return matched;
    }

    public double getDistance() {
        return distance;
    }

    public String getReason() {
        return reason;
    }
}
