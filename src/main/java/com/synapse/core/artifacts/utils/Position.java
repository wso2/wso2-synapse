package com.synapse.core.artifacts.utils;

public class Position {
    private int lineNo;
    private String fileName;
    private String hierarchy;

    public Position(int lineNo, String fileName, String hierarchy) {
        this.lineNo = lineNo;
        this.fileName = fileName;
        this.hierarchy = hierarchy;
    }

    public Position() {

    }

    public int getLineNo() { return lineNo; }
    public String getFileName() { return fileName; }
    public String getHierarchy() { return hierarchy; }

    public void setHierarchy(String hierarchy) {
        this.hierarchy = hierarchy;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setLineNo(int lineNo) {
        this.lineNo = lineNo;
    }
}