import { useState } from "react";
import { useNavigate } from "react-router-dom";
import "./App.css";

function Loginpage() {
    const navigate = useNavigate();
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");

    const handleLogin = async (e) => {
        e.preventDefault();

        try {
        const res = await fetch("http://localhost:8080/api/login", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
            email: email,
            passwordHash: password,
            }),
        });

        const data = await res.json();

        if (res.ok) {
            alert(data.message);

            // ✅ Save email for later (for upload page or other pages)
            localStorage.setItem("userEmail", email);

            // ✅ If email is verified → go to tourist place selection page
            if (data.emailVerified) {
            navigate("/select-place");
            } else {
            // If not verified → go to email verification page
            navigate("/verify");
            }
        } else {
            alert(data.message || "Login failed");
        }
        } catch (err) {
        console.error(err);
        alert("Error connecting to backend");
        }
    };

    return (
        <div className="card">
        <h2>Login 🔐</h2>
        <form onSubmit={handleLogin}>
            <input
            type="email"
            placeholder="Email Address"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            />
            <input
            type="password"
            placeholder="Password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            />
            <button type="submit" className="btn btn-primary">
            Login
            </button>
        </form>
        <p>
            Don’t have an account?{" "}
            <span className="link" onClick={() => navigate("/register")}>
            Register
            </span>
        </p>
        </div>
    );
}

export default Loginpage;
