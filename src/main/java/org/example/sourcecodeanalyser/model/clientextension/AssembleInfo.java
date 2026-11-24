package org.example.sourcecodeanalyser.model.clientextension;

public class AssembleInfo {

    private String from;
    private String into;

    public AssembleInfo(String from, String into) {
        this.from = from;
        this.into = into;
    }

    public String getFrom() {
        return from;
    }

    public String getInto() {
        return into;
    }
}