package pk.gov.pbs.utils;

public class ExceptionReporter {
    //Todo: Add functionality to write error into a file
    //Todo: Add functionality to also upload error detail is any endpoint has be setup
    public static void printStackTrace(Exception exception){
        if (Constants.DEBUG_MODE) {
            exception.printStackTrace();
            throw new RuntimeException(exception);
        }
    }
}
