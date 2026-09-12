package com.ysh.util.pdf.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 金额转中文大写（人民币），如 123.45 -> 壹佰贰拾叁圆肆角伍分。
 * 整数部分最多支持 12 位（千亿级），小数部分四舍五入到 2 位。
 */
public final class ChineseAmountUtil {

    private static final char[] DIGITS = "零壹贰叁肆伍陆柒捌玖".toCharArray();
    private static final String[] SECTION_UNITS = {"", "拾", "佰", "仟"};
    private static final String[] GROUP_UNITS = {"", "万", "亿"};
    private static final int MAX_INTEGER_DIGITS = 12;

    private ChineseAmountUtil() {
    }

    public static String convert(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("金额不能为 null");
        }
        BigDecimal value = amount.setScale(2, RoundingMode.HALF_UP);
        if (value.precision() - value.scale() > MAX_INTEGER_DIGITS) {
            throw new IllegalArgumentException("整数部分超过 " + MAX_INTEGER_DIGITS + " 位：" + amount);
        }

        boolean negative = value.signum() < 0;
        value = value.abs();

        long cents = value.movePointRight(2).longValueExact();
        long yuan = cents / 100;
        int jiao = (int) (cents / 10 % 10);
        int fen = (int) (cents % 10);

        StringBuilder sb = new StringBuilder();
        if (negative) {
            sb.append('负');
        }
        sb.append(yuanPart(yuan));
        if (yuan > 0) {
            sb.append('圆');
        }

        if (jiao == 0 && fen == 0) {
            sb.append(yuan == 0 ? "零圆整" : "整");
        } else {
            if (jiao > 0) {
                sb.append(DIGITS[jiao]).append('角');
            } else if (yuan > 0) {
                // 角为 0 分不为 0，圆后需补零，如 10.05 -> 壹拾圆零伍分
                sb.append('零');
            }
            if (fen > 0) {
                sb.append(DIGITS[fen]).append('分');
            } else {
                sb.append('整');
            }
        }
        return sb.toString();
    }

    public static String convert(String amount) {
        return convert(new BigDecimal(amount));
    }

    /**
     * 整数部分按 4 位一组（万/亿）转换，组内零折叠，组间按需补零。
     */
    private static String yuanPart(long yuan) {
        if (yuan == 0) {
            return "";
        }
        String s = Long.toString(yuan);
        int groupCount = (s.length() + 3) / 4;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < groupCount; i++) {
            int start = i * 4 - (groupCount * 4 - s.length());
            int end = start + 4;
            String group = s.substring(Math.max(start, 0), Math.min(end, s.length()));
            if (Long.parseLong(group) == 0) {
                continue;
            }
            // 首组是原数字的最高位，本身不会以零开头；其余组保留前导零用于补零
            sb.append(section(group, i > 0)).append(GROUP_UNITS[groupCount - 1 - i]);
        }
        return sb.toString();
    }

    private static String section(String digits, boolean keepLeadingZero) {
        StringBuilder sb = new StringBuilder();
        boolean zeroPending = false;
        for (int i = 0; i < digits.length(); i++) {
            int d = digits.charAt(i) - '0';
            if (d == 0) {
                zeroPending = true;
            } else {
                if (zeroPending && (keepLeadingZero || sb.length() > 0)) {
                    sb.append('零');
                }
                zeroPending = false;
                sb.append(DIGITS[d]).append(SECTION_UNITS[digits.length() - 1 - i]);
            }
        }
        return sb.toString();
    }
}
