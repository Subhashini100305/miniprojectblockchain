import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import "./App.css";
import { getAuthHeader } from "./utils/auth";

export default function MyReviews() {

    const [reviews, setReviews] = useState([]);
    const [trustScore, setTrustScore] = useState(null);
    const navigate = useNavigate();

    const fetchMyReviews = async () => {
        try {
            const res = await fetch(
                "http://localhost:8080/api/reviews/my-reviews",
                { headers: getAuthHeader() }
            );
            const data = await res.json();
            setReviews(data);
        } catch (error) {
            console.error("Error fetching my reviews:", error);
        }
    };

    const fetchTrustScore = async () => {
        try {
            const res = await fetch(
                "http://localhost:8080/api/user/trust-score",
                { headers: getAuthHeader() }
            );
            const data = await res.json();
            setTrustScore(data);
        } catch (error) {
            console.error("Error fetching trust score:", error);
        }
    };

    useEffect(() => {
        fetchMyReviews();
        fetchTrustScore();
    }, []);

    const getScoreColor = (score) => {
        if (score >= 80) return "#2ecc71";
        if (score >= 60) return "#f39c12";
        return "#e74c3c";
    };

    return (
        <div className="card">
            <h2>My Posted Reviews </h2>

            {trustScore && (
                <div style={{
                    background: "#f0f8ff",
                    padding: "15px",
                    borderRadius: "10px",
                    marginBottom: "20px",
                    borderLeft: "4px solid " + getScoreColor(trustScore.userTrustScore)
                }}>
                    <h3 style={{
                        color: getScoreColor(trustScore.userTrustScore)
                    }}>
                         Your Trust Score: {trustScore.userTrustScore?.toFixed(1)}/100
                    </h3>

                    <p>
                         Email Verified: {trustScore.emailVerified ? "Yes " : "No "}
                    </p>

                    <p>
                         Photo Verified: {trustScore.photoVerified ? "Yes " : "No "}
                    </p>

                    <p>
                         GPS Verified: {trustScore.gpsVerified ? "Yes " : "No "}
                    </p>

                    <p>
                         Avg AI Confidence: {trustScore.avgAiConfidence?.toFixed(1)}%
                    </p>
                </div>
            )}

            {reviews.length === 0 ? (
                <p>You have not posted any reviews yet.</p>
            ) : (
                <ul style={{ listStyle: "none", padding: 0 }}>
                    {reviews.map((rev) => (
                        <li
                            key={rev.id}
                            style={{
                                background: "#f5f5f5",
                                padding: "15px",
                                marginBottom: "12px",
                                borderRadius: "10px",
                            }}
                        >
                            <h4> {rev.placeName}</h4>
                            <p> Rating: {rev.rating}</p>
                            <p>{rev.reviewText}</p>

                            <p style={{
                                fontSize: "0.85em",
                                color: "#666"
                            }}>
                                Review Trust: {rev.trustPoints?.toFixed(1) ?? "N/A"}
                            </p>

                            {rev.isDisputed && (
                                <p style={{
                                    color: "red",
                                    fontWeight: "bold"
                                }}>
                                     DISPUTED REVIEW
                                </p>
                            )}
                        </li>
                    ))}
                </ul>
            )}

            <button
                onClick={() => navigate("/dashboard")}
                className="btn btn-secondary"
            >
                🔙 Back to Dashboard
            </button>
        </div>
    );
}
