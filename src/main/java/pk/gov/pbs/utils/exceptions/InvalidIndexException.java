package pk.gov.pbs.utils.exceptions;

public class InvalidIndexException extends Exception {
    public InvalidIndexException(){
        super("Invalid index received;");
    }

    public InvalidIndexException(Object value){
        super("Invalid index received as : " + value.toString());
    }
    public InvalidIndexException(Object value, String ruleBeingBroken){
        super("Invalid index received as : " + value.toString() + " | because " + ruleBeingBroken);
    }
}
