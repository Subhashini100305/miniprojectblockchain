import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import "./App.css";

export default function ReviewPage() {
    const navigate = useNavigate();
    const [review, setReview] = useState("");
    const [reviews, setReviews] = useState([]);
    const [status, setStatus] = useState("");
    const userEmail = localStorage.getItem("userEmail");

    // ✅ Fetch the user’s verification status
    const fetchStatus = async () => {
        try {
        const res = await fetch(
            `http://localhost:8080/api/verification/status?email=${userEmail}`
        );
        const data = await res.json();
        if (data.length > 0) {
            setStatus(data[data.length - 1].status);
        }
        } catch (error) {
        console.error("Error fetching status:", error);
        }
    };

    useEffect(() => {
        if (userEmail) fetchStatus();
    }, []);

    // ✅ Add review if verified
    const handleAddReview = (e) => {
        e.preventDefault();

        if (status.toLowerCase() !== "verified") {
        alert("You can post a review only after verification!");
        return;
        }

        if (review.trim() === "") {
        alert("Please enter your review!");
        return;
        }

        setReviews([...reviews, review]);
        setReview("");
        alert("Review added successfully!");
    };

    return (
        <div className="card">
        <h2>Share Your Experience 💬</h2>

        {/* Show verification status */}
        <p>
            Status:{" "}
            <strong
            style={{
                color:
                status.toLowerCase() === "verified"
                    ? "green"
                    : status.toLowerCase() === "rejected"
                    ? "red"
                    : "gray",
            }}
            >
            {status || "Not Verified"}
            </strong>
        </p>

        {/* Review form */}
        <form onSubmit={handleAddReview}>
            <textarea
            placeholder="Write your review about the tourist place..."
            value={review}
            onChange={(e) => setReview(e.target.value)}
            rows="4"
            required
            />

            <button type="submit" className="btn btn-primary">
            Add Review
            </button>
        </form>

        {/* Display added reviews */}
        {reviews.length > 0 && (
            <div className="reviews-list">
            <h3>Your Reviews:</h3>
            <ul>
                {reviews.map((rev, index) => (
                <li key={index}>{rev}</li>
                ))}
            </ul>
            </div>
        )}

        <button
            className="btn"
            onClick={() => navigate("/verify2")}
            style={{ marginTop: "15px" }}
        >
            🔙 Back to Verification Page
        </button>
        </div>
    );
}
