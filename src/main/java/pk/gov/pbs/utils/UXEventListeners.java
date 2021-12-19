package pk.gov.pbs.utils;

public abstract class UXEventListeners {
    public interface ConfirmDialogueEventsListener extends AlertDialogueEventListener {
        void onCancel();
    }

    public interface AlertDialogueEventListener {
        void onOK();
    }
}
