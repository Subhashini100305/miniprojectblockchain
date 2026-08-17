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
            localStorage.setItem("userEmail", email);

           
            localStorage.setItem(
                "token",
                data.token
            );

            
            if (data.emailVerified) {

                // save login state if needed later
                localStorage.setItem("token", data.token);

                navigate("/dashboard");

            } else {
            
                navigate("/verify", { state: { email } });
            }
        } else if (res.status === 403) {
            alert(data.message || "Email verification required");
            navigate("/verify", { state: { email } });
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
        <h2>Login </h2>
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
