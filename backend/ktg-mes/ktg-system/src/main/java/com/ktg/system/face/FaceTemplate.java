package com.ktg.system.face;

import java.util.List;

/**
 * 人脸特征向量的解析、校验与距离计算。
 *
 * 约定：前端（face-api.js）输出 128 维 float 特征；服务端只存特征、不存照片。
 * 距离采用欧氏距离，同一人通常在 0.3~0.5，不同人通常大于 0.6。
 */
public final class FaceTemplate {

    /** face-api.js FaceRecognitionNet 输出维度。 */
    public static final int DIMENSION = 128;

    /** 方差下限：全 0 或常量向量视为无效采集，避免"空模板"匹配任何人。 */
    private static final double MIN_VARIANCE = 1.0E-6;

    /** 数值上限：异常大的分量直接拒绝，防止脏数据进入比对。 */
    private static final double MAX_ABS_VALUE = 100.0D;

    private FaceTemplate() {
    }

    /** 解析数据库中的特征文本；格式不合法返回 null。 */
    public static double[] parse(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        String[] parts = text.trim().split(",");
        if (parts.length != DIMENSION) {
            return null;
        }
        double[] values = new double[DIMENSION];
        for (int i = 0; i < DIMENSION; i++) {
            try {
                values[i] = Double.parseDouble(parts[i].trim());
            } catch (NumberFormatException e) {
                return null;
            }
            if (!isFinite(values[i])) {
                return null;
            }
        }
        return values;
    }

    /** 把前端提交的数组转换为特征向量；维度或数值不合法返回 null。 */
    public static double[] toArray(List<? extends Number> values) {
        if (values == null || values.size() != DIMENSION) {
            return null;
        }
        double[] result = new double[DIMENSION];
        for (int i = 0; i < DIMENSION; i++) {
            Number value = values.get(i);
            if (value == null) {
                return null;
            }
            result[i] = value.doubleValue();
            if (!isFinite(result[i])) {
                return null;
            }
        }
        return result;
    }

    /** 序列化为数据库存储格式。 */
    public static String format(double[] values) {
        if (values == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder(values.length * 12);
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(values[i]);
        }
        return builder.toString();
    }

    /** 特征是否可用于比对：维度正确、有限、非常量、数值范围合理。 */
    public static boolean isUsable(double[] values) {
        if (values == null || values.length != DIMENSION) {
            return false;
        }
        double min = values[0];
        double max = values[0];
        double sum = 0D;
        double sumSquare = 0D;
        for (double value : values) {
            if (!isFinite(value) || Math.abs(value) > MAX_ABS_VALUE) {
                return false;
            }
            min = Math.min(min, value);
            max = Math.max(max, value);
            sum += value;
            sumSquare += value * value;
        }
        double mean = sum / DIMENSION;
        double variance = sumSquare / DIMENSION - mean * mean;
        return (max - min) > 0D && variance > MIN_VARIANCE;
    }

    /** 欧氏距离；维度不一致直接抛 IllegalArgumentException。 */
    public static double distance(double[] left, double[] right) {
        if (left == null || right == null || left.length != right.length) {
            throw new IllegalArgumentException("人脸特征维度不一致");
        }
        double sum = 0D;
        for (int i = 0; i < left.length; i++) {
            double diff = left[i] - right[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    /** 多帧平均，降低单帧抖动；用于录入时提升稳定性。 */
    public static double[] average(List<double[]> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        int dimension = values.get(0).length;
        double[] result = new double[dimension];
        for (double[] value : values) {
            if (value == null || value.length != dimension) {
                throw new IllegalArgumentException("人脸特征维度不一致");
            }
            for (int i = 0; i < dimension; i++) {
                result[i] += value[i];
            }
        }
        for (int i = 0; i < dimension; i++) {
            result[i] /= values.size();
        }
        return result;
    }

    private static boolean isFinite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }
}
