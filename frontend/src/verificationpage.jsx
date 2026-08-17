import { useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import "./App.css";

function Verificationpage() {
    const navigate = useNavigate();
    const location = useLocation();
    const email = location.state?.email || "";

    const [token, setToken] = useState("");

    const handleSendToken = async () => {
        try {
            const res = await fetch("http://localhost:8080/api/send-token", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ email }),
            });
            const data = await res.text();
            alert(data);
        } catch (err) {
            console.error(err);
            alert("Error sending token");
        }
    };

    const handleVerify = async (e) => {
        e.preventDefault();
        try {
            const res = await fetch("http://localhost:8080/api/verify-token", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ email, token }),
            });
            const data = await res.text();
            alert(data);
            if (res.ok) navigate("/login");
        } catch (err) {
            console.error(err);
            alert("Error verifying token");
        }
    };

    return (
        <div className="card">
            <h2>Email Verification </h2>
            <p>Email: {email}</p>
            <button onClick={handleSendToken} className="btn btn-secondary">
                Send Verification Token
            </button>
            <form onSubmit={handleVerify}>
                <input
                    type="text"
                    placeholder="Enter Token"
                    value={token}
                    onChange={(e) => setToken(e.target.value)}
                    required
                />
                <button type="submit" className="btn btn-primary">Verify Email</button>
            </form>
        </div>
    );
}

export default Verificationpage;
