package pk.gov.pbs.utils;

//Todo: Add functionality to write error into a file
//Todo: Add functionality to also upload error detail when endpoint has been setup
public class ExceptionReporter {
    public static void handle(Throwable exception){
        printStackTrace(exception);
    }

    public static void printStackTrace(Throwable exception){
        if (Constants.DEBUG_MODE) {
            throw new RuntimeException(exception);
        }
    }
}
