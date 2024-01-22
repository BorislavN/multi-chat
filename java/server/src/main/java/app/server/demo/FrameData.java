package app.server.demo;

public class FrameData {
    private boolean isFinished;
    private int opcode;
    private boolean isMasked;
    private int length;
    private byte[] mask;

    public FrameData(boolean isFinished, int opcode, boolean isMasked, int length) {
        this.isFinished = isFinished;
        this.opcode = opcode;
        this.isMasked = isMasked;
        this.length = length;
    }

    public boolean isFinished() {
        return this.isFinished;
    }

    public void setFinished(boolean finished) {
        isFinished = finished;
    }

    public int getOpcode() {
        return this.opcode;
    }

    public void setOpcode(int opcode) {
        this.opcode = opcode;
    }

    public boolean isMasked() {
        return this.isMasked;
    }

    public void setMasked(boolean masked) {
        isMasked = masked;
    }

    public int getLength() {
        return this.length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public byte[] getMask() {
        return this.mask;
    }

    public void setMask(byte[] mask) {
        this.mask = mask;
    }

    @Override
    public String toString() {
        String delimiter = "///////////////////////";

        return String.format("%s%nIs finished: %s%nOpcode: 0x%s%nIs masked: %s%nLength: %s%nMask: %s%n%s%n"
                , delimiter
                , this.isFinished
                , Integer.toHexString(this.opcode)
                , this.isMasked
                , this.length
                , arrayAsBinaryString(this.mask, " ")
                , delimiter
        );
    }

    private String arrayAsBinaryString(byte[] data, String delimiter) {
        StringBuilder output = new StringBuilder();

        for (int index = 0; index < data.length; index++) {
            output.append(data[index] & 255);

            if (index < data.length - 1) {
                output.append(delimiter);
            }
        }

        return output.toString();
    }
}