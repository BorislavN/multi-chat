package app.client.websocket;

import app.util.Constants;

public class MessageProperty {
    private ValueListener valueListener;
    private String value;

    public MessageProperty(String initialValue) {
        this.value = initialValue;
        this.valueListener = null;
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        String oldValue = this.value;
        this.value = value;

        if (this.valueListener != null && this.value != null) {
            if (this.value.startsWith(Constants.EXCEPTION_FLAG) || !this.value.equals(oldValue)) {
                this.valueListener.onSet(this.value);
            }
        }
    }

    public void setValueListener(ValueListener valueListener) {
        this.valueListener = valueListener;
    }
}