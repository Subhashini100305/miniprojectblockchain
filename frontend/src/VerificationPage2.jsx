import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import "./App.css";

export default function VerificationPage2() {
    const [file, setFile] = useState(null);
    const [verifications, setVerifications] = useState([]);
    const userEmail = localStorage.getItem("userEmail");
    const navigate = useNavigate();

    const fetchStatus = async () => {
        try {
            const res = await fetch(`http://localhost:8080/api/verification/status?email=${userEmail}`);
            const data = await res.json();
            setVerifications(data);
        } catch (error) {
            console.error("Error fetching status:", error);
        }
    };

    const handleUpload = async (e) => {
        e.preventDefault();
        if (!file) return alert("Please choose a file!");

        const formData = new FormData();
        formData.append("email", userEmail);
        formData.append("file", file);
        formData.append("selectedPlace", localStorage.getItem("selectedPlace"));

        try {
            const res = await fetch("http://localhost:8080/api/verification/upload", {
                method: "POST",
                body: formData,
            });
            const text = await res.text();

            if (text.startsWith("VERIFIED:")) {
                const place = text.split(":")[1];
                alert(`Proof verified for ${place}! ✅`);
                localStorage.setItem("verifiedPlace", place);
                navigate("/review");
            } else if (text.startsWith("REJECTED:")) {
                alert(`Proof uploaded but not verified ❌ (${text.split(":")[1]})`);
            } else {
                alert(text);
            }

            fetchStatus();
        } catch (error) {
            alert("Upload failed. Please try again.");
            console.error(error);
        }
    };

    useEffect(() => {
        if (userEmail) fetchStatus();
    }, []);

    return (
        <div className="card">
            <h2>Account Verification ✅</h2>

            <p>
                Selected Place:{" "}
                <strong>{localStorage.getItem("selectedPlace") || "None"}</strong>
            </p>

            <form onSubmit={handleUpload}>
                <input
                    type="file"
                    accept="image/*,application/pdf"
                    onChange={(e) => setFile(e.target.files[0])}
                    required
                />
                <p style={{ fontSize: "0.9em", color: "#666", marginTop: "8px" }}>
                    Upload travel proof for the selected tourist place.
                </p>
                <button type="submit" className="btn btn-primary">
                    Upload Proof
                </button>
            </form>

            <h3 style={{ marginTop: "25px" }}>Your Verifications</h3>
            {verifications.length === 0 ? (
                <p>No verifications yet.</p>
            ) : (
                <ul>
                    {verifications.map((v, index) => (
                        <li key={index}>
                            <strong>{v.placeName || "Unknown Place"}:</strong>{" "}
                            <span
                                style={{
                                    color:
                                        v.status === "VERIFIED"
                                            ? "green"
                                            : v.status === "REJECTED"
                                            ? "red"
                                            : "gray",
                                }}
                            >
                                {v.status}
                            </span>
                        </li>
                    ))}
                </ul>
            )}

            <button
                onClick={() => navigate("/select-place")}
                className="btn btn-secondary"
                style={{ marginTop: "15px" }}
            >
                Select Tourist Place
            </button>
        </div>
    );
}
