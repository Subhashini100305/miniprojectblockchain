package com.miniproject.verificationApp.nlp;

import java.util.List;

public class QualityResult {

    private int score;
    private boolean accepted;
    private List<String> issues;

    public QualityResult(int score, boolean accepted, List<String> issues) {
        this.score = score;
        this.accepted = accepted;
        this.issues = issues;
    }

    public int getScore() {
        return score;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public List<String> getIssues() {
        return issues;
    }
}