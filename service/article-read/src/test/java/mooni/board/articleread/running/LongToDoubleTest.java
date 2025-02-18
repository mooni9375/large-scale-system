package mooni.board.articleread.running;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

public class LongToDoubleTest {

    @Test
    void longToDoubleTest() {

        // long   64비트 정수
        // double 64비트 부동소수점
        long longValue = 111_111_111_111_111_111L;
        System.out.println("longValue   = " + longValue);

        double doubleValue = longValue;
        System.out.println("doubleValue = " + new BigDecimal(doubleValue).toString());

        long longValue2 = (long) doubleValue;
        System.out.println("longValue2  = " + longValue2);
    }
}
