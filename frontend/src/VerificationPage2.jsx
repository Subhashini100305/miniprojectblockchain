import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import "./App.css";
import { getAuthHeader } from "./utils/auth";

export default function VerificationPage2() {

    const [file, setFile] = useState(null);
    const [verifications, setVerifications] = useState([]);

    const navigate = useNavigate();

    const fetchStatus = async () => {

        try {

            const res = await fetch(
                "http://localhost:8080/api/verification/status",
                { headers: getAuthHeader() }
            );

            const data = await res.json();

            setVerifications(data);

        } catch (error) {

            console.error("Error fetching status:", error);
        }
    };

    const handleUpload = async (e) => {

        e.preventDefault();

        if (!file) {
            return alert("Please choose a file!");
        }

        const formData = new FormData();

        formData.append("file", file);

        formData.append(
            "selectedPlace",
            localStorage.getItem("selectedPlace")
        );

        formData.append(
            "selectedLat",
            localStorage.getItem("selectedLat")
        );

        formData.append(
            "selectedLon",
            localStorage.getItem("selectedLon")
        );

        try {

            // Loading message
            alert("🔍 AI is analyzing your image... Please wait.");

            const res = await fetch(
                "http://localhost:8080/api/verification/upload",
                {
                    method: "POST",
                    headers: getAuthHeader(),
                    body: formData,
                }
            );

            const data = await res.json();

            if (data.status === "VERIFIED") {
                alert(
`   Proof verified for ${data.place}!

    AI Confidence: ${data.confidence}%
    Verification Type: ${data.verificationType}
    GPS Verified: ${data.gpsVerified}
     Distance: ${data.distanceMeters} meters`
                );
                localStorage.setItem("verifiedPlace", data.place);
                navigate("/review");

            } else if (data.status === "REJECTED") {
                alert(
`    Verification Failed

    Reason: ${data.reason}
    AI Confidence: ${data.confidence}%
     GPS Status: ${data.gpsReason}

Please upload a clearer and valid travel proof image.`
                );

            } else {
                alert(data.message || "Something went wrong");
            }

            // Refresh verification history
            fetchStatus();

        } catch (error) {

            alert("Upload failed. Please try again.");

            console.error(error);
        }
    };

    useEffect(() => {

        fetchStatus();

    }, []);

    return (

        <div className="card">

            <h2>Account Verification ✅</h2>

            <p>
                Selected Place:{" "}
                <strong>
                    {localStorage.getItem("selectedPlace") || "None"}
                </strong>
            </p>

            <form onSubmit={handleUpload}>

                <input
                    type="file"
                    accept="image/*,application/pdf"
                    onChange={(e) =>
                        setFile(e.target.files[0])
                    }
                    required
                />

                <p
                    style={{
                        fontSize: "0.9em",
                        color: "#666",
                        marginTop: "8px",
                    }}
                >
                    Upload travel proof for the selected tourist place.
                </p>

                <button
                    type="submit"
                    className="btn btn-primary"
                >
                    Upload Proof
                </button>

            </form>

            <h3 style={{ marginTop: "25px" }}>
                Your Verifications
            </h3>

            {verifications.length === 0 ? (

                <p>No verifications yet.</p>

            ) : (

                <ul>

                    {verifications.map((v, index) => (

                        <li key={index}>

                            <strong>
                                {v.placeName || "Unknown Place"}:
                            </strong>{" "}

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
