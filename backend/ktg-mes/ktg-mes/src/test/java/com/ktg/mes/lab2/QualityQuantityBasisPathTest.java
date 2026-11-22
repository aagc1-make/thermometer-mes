package com.ktg.mes.lab2;

import com.ktg.common.exception.ServiceException;
import com.ktg.mes.linkage.FormLinkagePolicy;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** Seven short-circuit predicates give eight independent quantity-validation paths. */
class QualityQuantityBasisPathTest {
    private final FormLinkagePolicy policy = new FormLinkagePolicy();

    static Stream<Arguments> basisPaths() {
        return Stream.of(
                Arguments.of("P1 checked missing", null, "8", "2", false),
                Arguments.of("P2 qualified missing", "10", null, "2", false),
                Arguments.of("P3 unqualified missing", "10", "8", null, false),
                Arguments.of("P4 checked zero", "0", "0", "0", false),
                Arguments.of("P5 qualified negative", "10", "-1", "11", false),
                Arguments.of("P6 unqualified negative", "10", "11", "-1", false),
                Arguments.of("P7 quantities not conserved", "10", "8", "1", false),
                Arguments.of("P8 valid conserved quantities", "10", "8", "2", true)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("basisPaths")
    void followsExpectedBasisPath(String path, String checked, String qualified,
                                 String unqualified, boolean accepted) {
        if (accepted) {
            assertDoesNotThrow(() -> policy.assertQualityQuantities(
                    decimal(checked), decimal(qualified), decimal(unqualified)), path);
        } else {
            ServiceException error = assertThrows(ServiceException.class,
                    () -> policy.assertQualityQuantities(
                            decimal(checked), decimal(qualified), decimal(unqualified)), path);
            assertEquals("检测数量必须等于合格数量与不合格数量之和", error.getMessage(), path);
        }
    }

    private static BigDecimal decimal(String value) {
        return value == null ? null : new BigDecimal(value);
    }
}
