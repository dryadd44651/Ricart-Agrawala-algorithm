

import java.io.Serializable;

public class Message implements Comparable<Message>, Serializable {
    private int clock;
    private final String type;
    private final int from;
    private final int to;
    private String content = "";
    private final String fileName;
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
    public void setClock(int newClock) {
        this.clock = newClock;
    }
    public int getClock() {
        return clock;
    }

    public String getType() {
        return type;
    }

    public int getFrom() {
        return from;
    }

    public int getTo() {
        return to;
    }

    public String getFileName() {
        return fileName;
    }

    public Message(int counter, String type, int from, int to, String fileName) {
        this.clock = counter;
        this.type = type;
        this.from = from;
        this.to = to;
        this.fileName = fileName;
    }

    public static void main(String[] args) {

    }

    @Override
    public int compareTo(Message o) {
        int diff = this.clock - o.clock;
        return diff != 0 ? diff : this.getFrom() - o.getFrom();
    }

    @Override
    public String toString() {
        return clock + "," + type + "," + from + "," + to + "," + fileName;
    }
}

