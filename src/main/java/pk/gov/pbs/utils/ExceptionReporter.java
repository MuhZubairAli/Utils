package pk.gov.pbs.utils;

public class ExceptionReporter {
    public static void printStackTrace(Exception exception){
        if (Constants.DEBUG_MODE)
            exception.printStackTrace();
    }
}
