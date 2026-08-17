import "./App.css";
import { useNavigate } from "react-router-dom";
import { getAuthHeader } from "./utils/auth";

function Dashboard() {

    const navigate = useNavigate();

    const logout = async () => {
        try {
            await fetch("http://localhost:8080/api/logout", {
                method: "POST",
                headers: getAuthHeader()
            });
        } finally {
            [
                "token",
                "userEmail",
                "selectedPlace",
                "selectedLat",
                "selectedLon",
                "verifiedPlace"
            ].forEach((key) => localStorage.removeItem(key));

            navigate("/login");
        }
    };

    return (
        <div className="card">
            <h2>Dashboard </h2>
            <p>Choose one of the actions below.</p>

            <button
                className="btn btn-primary"
                onClick={() => navigate("/place-reviews")}
            >
                View Place Reviews
            </button>

            <button
                className="btn btn-secondary"
                onClick={() => navigate("/my-reviews")}
            >
                View My Reviews
            </button>

            <button
                className="btn btn-secondary"
                onClick={() => navigate("/select-place")}
            >
                Post a Review
            </button>

            <button onClick={logout}>
                Logout
            </button>
        </div>
    );
}

export default Dashboard;
