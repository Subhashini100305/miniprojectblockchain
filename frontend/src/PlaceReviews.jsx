import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import "./App.css";
import { getAuthHeader } from "./utils/auth";

export default function PlaceReviews() {

    const navigate = useNavigate();
    const [filteredReviews, setFilteredReviews] = useState([]);
    const [searchPlace, setSearchPlace] = useState("");
    const [loading, setLoading] = useState(false);
    const [hasSearched, setHasSearched] = useState(false);

    const handleSearch = async () => {
        if (!searchPlace.trim()) {
            setFilteredReviews([]);
            setHasSearched(false);
            return;
        }

        setHasSearched(true);
        setLoading(true);
        try {
            const res = await fetch(
                "http://localhost:8080/api/reviews/search-with-trust?place="
                    + encodeURIComponent(searchPlace),
                { headers: getAuthHeader() }
            );
            const data = await res.json();
            setFilteredReviews(data);
        } catch (error) {
            console.error("Search error:", error);
        } finally {
            setLoading(false);
        }
    };

    const handleFlag = async (reviewId) => {
        try {
            const res = await fetch(
                "http://localhost:8080/api/reviews/flag",
                {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json",
                        ...getAuthHeader()
                    },
                    body: JSON.stringify({ reviewId: reviewId }),
                }
            );

            const text = await res.text();
            alert(text);
            handleSearch();
        } catch (error) {
            console.error(error);
            alert("Error flagging review");
        }
    };

    const getScoreColor = (score) => {
        if (score >= 80) return "#2ecc71";
        if (score >= 60) return "#f39c12";
        return "#e74c3c";
    };

    return (
        <div className="card">
            <h2>Place Reviews 🏞️</h2>

            <div style={{ marginBottom: "20px" }}>
                <input
                    type="text"
                    placeholder="Enter tourist place..."
                    value={searchPlace}
                    onChange={(e) => setSearchPlace(e.target.value)}
                />

                <button
                    className="btn btn-primary"
                    onClick={handleSearch}
                >
                    Search
                </button>
            </div>

            {loading && <p>Loading reviews...</p>}

            {hasSearched && !loading && filteredReviews.length === 0 ? (
                <p>No reviews found.</p>
            ) : hasSearched && !loading ? (
                <ul style={{ listStyle: "none", padding: 0 }}>
                    {filteredReviews.map((rev) => (
                        <li
                            key={rev.id}
                            style={{
                                background: "#f5f5f5",
                                padding: "15px",
                                marginBottom: "12px",
                                borderRadius: "10px",
                                borderLeft: "4px solid " + getScoreColor(rev.finalScore)
                            }}
                        >
                            <h4> {rev.placeName}</h4>
                            <p>Rating: {rev.rating}</p>
                            <p>{rev.reviewText}</p>

                            <div style={{
                                marginTop: "8px",
                                fontSize: "0.9em",
                                color: "#444",
                                background: "#eee",
                                padding: "8px",
                                borderRadius: "6px"
                            }}>
                                <span>
                                    Review Score: {rev.trustPoints?.toFixed(1) ?? "N/A"}
                                </span>

                                <span style={{ marginLeft: "15px" }}>
                                     User Reliability: {rev.userTrustScore?.toFixed(1) ?? "N/A"}/100
                                </span>

                                <span style={{
                                    marginLeft: "15px",
                                    fontWeight: "bold",
                                    color: getScoreColor(rev.finalScore)
                                }}>
                                    Final Score: {rev.finalScore?.toFixed(1) ?? "N/A"}/100
                                </span>
                            </div>

                            {rev.isDisputed && (
                                <p style={{
                                    color: "red",
                                    fontWeight: "bold"
                                }}>
                                    DISPUTED REVIEW
                                </p>
                            )}

                            <button
                                onClick={() => handleFlag(rev.id)}
                                style={{
                                    marginTop: "8px",
                                    backgroundColor: "#ff4d4d",
                                    color: "white",
                                    border: "none",
                                    padding: "8px 12px",
                                    borderRadius: "8px",
                                    cursor: "pointer",
                                }}
                            >
                                 Flag Review
                            </button>
                        </li>
                    ))}
                </ul>
            ) : null}

            <button
                onClick={() => navigate("/dashboard")}
                className="btn btn-secondary"
            >
                🔙 Back to Dashboard
            </button>
        </div>
    );
}
