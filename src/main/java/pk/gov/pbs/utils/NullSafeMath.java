package pk.gov.pbs.utils;

public class NullSafeMath {
    public static int sum(Integer... elements){
        int total = 0;
        for (Integer number : elements) {
            if (number != null)
                total += number;
        }
        return total;
    }

    public static double sum(Double... elements){
        double total = 0;
        for (Double number : elements) {
            if (number != null)
                total += number;
        }
        return total;
    }

    public static long sum(Long... elements){
        long total = 0;
        for (Long number : elements) {
            if (number != null)
                total += number;
        }
        return total;
    }

    public static float sum(Float... elements){
        float total = 0;
        for (Float number : elements) {
            if (number != null)
                total += number;
        }
        return total;
    }

    public static int multiply(Null treatNullAs, Integer... elements){
        Integer total = null;
        for (Integer number : elements) {
            if (number == null) {
                if (treatNullAs == Null.Ignore)
                    continue;

                number = treatNullAs == Null.Zero ? 0 : 1;
            }
            total = (total == null) ? number : total * number;
        }
        return total == null ? 0 : total;
    }

    public static int multiply(Integer... elements){
        return multiply(Null.Ignore, elements);
    }

    public static double multiply(Null treatNullAs, Double... elements){
        Double total = null;
        for (Double number : elements) {
            if (number == null) {
                if (treatNullAs == Null.Ignore)
                    continue;

                number = treatNullAs == Null.Zero ? 0.0 : 1.0;
            }
            total = (total == null) ? number : total * number;
        }
        return total == null ? 0 : total;
    }

    public static double multiply(Double... elements){
        return multiply(Null.Ignore, elements);
    }

    public static long multiply(Null treatNullAs, Long... elements){
        Long total = null;
        for (Long number : elements) {
            if (number == null) {
                if (treatNullAs == Null.Ignore)
                    continue;

                number = treatNullAs == Null.Zero ? 0L : 1L;
            }
            total = (total == null) ? number : total * number;
        }
        return total == null ? 0 : total;
    }

    public static long multiply(Long... elements){
        return multiply(Null.Ignore, elements);
    }

    public static float multiply(Null treatNullAs, Float... elements){
        Float total = null;
        for (Float number : elements) {
            if (number == null) {
                if (treatNullAs == Null.Ignore)
                    continue;

                number = treatNullAs == Null.Zero ? 0.0F : 1.0F;
            }
            total = (total == null) ? number : total * number;
        }
        return total == null ? 0 : total;
    }

    public static float multiply(Float... elements){
        return multiply(Null.Ignore, elements);
    }

    public static boolean equal(Number number1, Number number2){
        if (number1 == null && number2 == null)
            return true;
        else if (number1 == null || number2 == null)
            return false;
        return number1.doubleValue() == number2.doubleValue();
    }

    protected boolean equalStrings(String value1, String value2){
        if (value1 == null && value2 == null)
            return true;
        else if (value1 == null || value2 == null)
            return false;
        return value1.equalsIgnoreCase(value2);
    }

    public enum Null {
        Ignore,
        Zero,
        Unity
    }
}
