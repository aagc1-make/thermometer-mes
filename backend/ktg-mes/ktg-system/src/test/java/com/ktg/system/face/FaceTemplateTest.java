package com.ktg.system.face;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

/**
 * 人脸特征解析与比对的核心规则。
 *
 * 这些用例不依赖数据库、不依赖浏览器，用来锁定"哪些特征能入库、多少距离算同一人"。
 */
public class FaceTemplateTest {

    private static final String ALGORITHM = "face-api-0.22.2";

    private static double[] vector(double seed) {
        double[] values = new double[FaceTemplate.DIMENSION];
        for (int i = 0; i < values.length; i++) {
            values[i] = Math.sin(seed + i * 0.37D) * 0.3D;
        }
        return values;
    }

    @Test
    public void formatsAndParsesRoundTrip() {
        double[] original = vector(1.2D);
        String text = FaceTemplate.format(original);
        double[] parsed = FaceTemplate.parse(text);
        Assert.assertNotNull(parsed);
        Assert.assertArrayEquals(original, parsed, 1.0E-12D);
    }

    @Test
    public void rejectsWrongDimensionOrBrokenValues() {
        Assert.assertNull(FaceTemplate.parse(null));
        Assert.assertNull(FaceTemplate.parse(""));
        Assert.assertNull(FaceTemplate.parse("1,2,3"));
        Assert.assertNull(FaceTemplate.parse(vectorTextWithBadToken()));
        Assert.assertFalse(FaceTemplate.isUsable(new double[FaceTemplate.DIMENSION]));
        Assert.assertFalse(FaceTemplate.isUsable(vectorButAllZero()));
        Assert.assertTrue(FaceTemplate.isUsable(vector(2.0D)));
    }

    private String vectorTextWithBadToken() {
        String[] parts = new String[FaceTemplate.DIMENSION];
        Arrays.fill(parts, "0.1");
        parts[17] = "NaN";
        return String.join(",", parts);
    }

    private double[] vectorButAllZero() {
        return new double[FaceTemplate.DIMENSION];
    }

    @Test
    public void samePersonStaysUnderThresholdAndDifferentPersonDoesNot() {
        double[] enrolled = vector(3.1D);
        Assert.assertTrue(FaceTemplate.distance(enrolled, enrolled) < 0.55D);

        double[] samePerson = new double[FaceTemplate.DIMENSION];
        for (int i = 0; i < enrolled.length; i++) {
            samePerson[i] = enrolled[i] + 0.01D;
        }
        Assert.assertTrue(FaceTemplate.distance(enrolled, samePerson) < 0.55D);

        Assert.assertTrue(FaceTemplate.distance(enrolled, vector(9.7D)) > 0.55D);
    }

    @Test
    public void averageSmoothsFrames() {
        double[] first = vector(0.5D);
        double[] second = vector(0.9D);
        List<double[]> frames = new ArrayList<double[]>();
        frames.add(first);
        frames.add(second);
        double[] average = FaceTemplate.average(frames);
        Assert.assertNotNull(average);
        for (int i = 0; i < FaceTemplate.DIMENSION; i++) {
            Assert.assertEquals((first[i] + second[i]) / 2D, average[i], 1.0E-12D);
        }
    }

    @Test
    public void convertsJsonNumbersAndRejectsShortLists() {
        List<Double> values = new ArrayList<Double>();
        for (double value : vector(6.6D)) {
            values.add(value);
        }
        Assert.assertArrayEquals(vector(6.6D), FaceTemplate.toArray(values), 1.0E-12D);
        Assert.assertNull(FaceTemplate.toArray(values.subList(0, 64)));
        values.set(3, null);
        Assert.assertNull(FaceTemplate.toArray(values));
    }

    @Test(expected = IllegalArgumentException.class)
    public void distanceRequiresSameDimension() {
        FaceTemplate.distance(new double[FaceTemplate.DIMENSION], new double[8]);
    }
}
