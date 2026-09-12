package com.samu.customerservice.score;

public class ScoreResponse {

    private String cpf;
    private Integer score;
    private String classification;

    public ScoreResponse() {
    }

    public ScoreResponse(String cpf, Integer score, String classification) {
        this.cpf = cpf;
        this.score = score;
        this.classification = classification;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getClassification() {
        return classification;
    }

    public void setClassification(String classification) {
        this.classification = classification;
    }
}
