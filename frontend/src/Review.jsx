import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import "./App.css";
import { getAuthHeader } from "./utils/auth";

export default function ReviewPage() {
    const navigate = useNavigate();
    const [review, setReview] = useState("");
    const [reviews, setReviews] = useState([]);
    const [rating, setRating] = useState(5);
    const [status, setStatus] = useState("");
    const userEmail = localStorage.getItem("userEmail");

    const fetchStatus = async () => {
        try {
            const res = await fetch(
                "http://localhost:8080/api/verification/status",
                { headers: getAuthHeader() }
            );
            const data = await res.json();
            if (data.length > 0) {
                setStatus(data[0].status);
            }
        } catch (error) {
            console.error("Error fetching status:", error);
        }
    };

    const fetchReviews = async () => {
        const res = await fetch(
            "http://localhost:8080/api/reviews/all",
            { headers: getAuthHeader() }
        );
        const data = await res.json();
        setReviews(data.content || []);
    };

    useEffect(() => {
        fetchReviews();
    }, []);

    useEffect(() => {
        fetchStatus();
    }, []);

    const handleAddReview = async (e) => {
        e.preventDefault();

        if ((status || "").toLowerCase() !== "verified") {
            alert("You can post a review only after verification!");
            return;
        }

        try {
            const res = await fetch("http://localhost:8080/api/reviews/add", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    ...getAuthHeader()
                },
                body: JSON.stringify({
                    email: userEmail,
                    review: review,
                    place: localStorage.getItem("selectedPlace"),
                    rating: rating
                }),
            });

            const text = await res.text();
            alert(text);
            setReview("");
            fetchReviews();

        } catch (err) {
            console.error(err);
            alert("Error saving review");
        }
    };

    return (
        <div className="card" style={{ padding: "25px" }}>
            <h2 style={{ marginBottom: "10px" }}>Share Your Experience </h2>

            <p>
                Status:{" "}
                <strong style={{
                    color:
                        (status || "").toLowerCase() === "verified"
                            ? "green"
                            : "red"
                }}>
                    {status || "Not Verified"}
                </strong>
            </p>

            {/* ================= FORM ================= */}
            <form onSubmit={handleAddReview}>
                <select
                    value={rating}
                    onChange={(e) => setRating(Number(e.target.value))}
                    style={{
                        width: "100%",
                        padding: "10px",
                        marginBottom: "10px",
                        borderRadius: "8px"
                    }}
                >
                    <option value="1">1 Star</option>
                    <option value="2">2 Stars</option>
                    <option value="3">3 Stars</option>
                    <option value="4">4 Stars</option>
                    <option value="5">5 Stars</option>
                </select>

                <textarea
                    placeholder="Write your review..."
                    value={review}
                    onChange={(e) => setReview(e.target.value)}
                    rows="4"
                    maxLength={2000}
                    style={{
                        width: "100%",
                        padding: "10px",
                        borderRadius: "8px",
                        marginBottom: "10px"
                    }}
                />

                {/*  FIXED ADD REVIEW BUTTON */}
                <button
                    type="submit"
                    style={{
                        width: "100%",
                        padding: "12px",
                        backgroundColor: "#4CAF50",
                        color: "white",
                        border: "none",
                        borderRadius: "10px",
                        fontSize: "16px",
                        cursor: "pointer"
                    }}
                >
                    ➕ Add Review
                </button>
            </form>

            {/* ================= REVIEWS ================= */}
            <h3 style={{ marginTop: "25px" }}>All Reviews</h3>

            <ul style={{ listStyle: "none", padding: 0 }}>
                {reviews.map((rev) => (
                    <li
                        key={rev.id}
                        style={{
                            background: "#f7f7f7",
                            padding: "12px",
                            marginBottom: "10px",
                            borderRadius: "10px"
                        }}
                    >
                         {rev.rating} - {rev.reviewText} ({rev.placeName})

                        {rev.isDisputed && (
                            <span style={{
                                color: "red",
                                marginLeft: "10px",
                                fontWeight: "bold"
                            }}>
                                 DISPUTED
                            </span>
                        )}

                        {/*  FIXED FLAG BUTTON */}
                        <button
                            onClick={async () => {
                                const res = await fetch(
                                    "http://localhost:8080/api/reviews/flag",
                                    {
                                        method: "POST",
                                        headers: {
                                            "Content-Type": "application/json",
                                            ...getAuthHeader()
                                        },
                                        body: JSON.stringify({
                                            reviewId: rev.id
                                        })
                                    }
                                );

                                const text = await res.text();
                                alert(text);
                                fetchReviews();
                            }}
                            style={{
                                marginTop: "8px",
                                marginLeft: "10px",
                                padding: "6px 10px",
                                backgroundColor: "#ff4d4d",
                                color: "white",
                                border: "none",
                                borderRadius: "6px",
                                cursor: "pointer"
                            }}
                        >
                            Flag
                        </button>
                    </li>
                ))}
            </ul>

            <button
                onClick={() => navigate("/verify2")}
                style={{
                    marginTop: "20px",
                    width: "100%",
                    padding: "10px",
                    backgroundColor: "#333",
                    color: "white",
                    borderRadius: "10px",
                    border: "none"
                }}
            >
                 Back
            </button>
        </div>
    );
}
