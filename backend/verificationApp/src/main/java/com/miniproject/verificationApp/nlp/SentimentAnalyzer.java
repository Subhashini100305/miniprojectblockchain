package com.miniproject.verificationApp.nlp;

import edu.stanford.nlp.pipeline.*;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Service
public class SentimentAnalyzer {

    private final StanfordCoreNLP pipeline;

    public SentimentAnalyzer() {

        Properties props = new Properties();

        props.setProperty(
                "annotators",
                "tokenize,ssplit,pos,parse,sentiment"
        );

        pipeline = new StanfordCoreNLP(props);
    }

    public synchronized String analyzeSentiment(String text) {

        CoreDocument document = new CoreDocument(text);

        pipeline.annotate(document);

        if (document.sentences().isEmpty()) {
    return "Neutral";
}

int positive = 0;
int negative = 0;

for (CoreSentence sentence : document.sentences()) {

    String sentiment = sentence.sentiment();

        if (sentiment.equalsIgnoreCase("Positive")
                || sentiment.equalsIgnoreCase("Very positive")) {
            positive++;
        }
        else if (sentiment.equalsIgnoreCase("Negative")
                || sentiment.equalsIgnoreCase("Very negative")) {
            negative++;
        }
        }

        if (positive > negative) {
            return "Positive";
        }
        else if (negative > positive) {
            return "Negative";
        }
        else {
            return "Neutral";
        }
    }
}