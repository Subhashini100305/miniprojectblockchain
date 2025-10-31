import { useState } from "react";
import { useNavigate } from "react-router-dom";
import "./App.css";

function Registrationpage() {
    const navigate = useNavigate();
    const [name, setName] = useState("");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");

    const handleRegister = async (e) => {
        e.preventDefault();
        try {
            const res = await fetch("http://localhost:8080/api/register", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    name: name,
                    email: email,
                    passwordHash: password  // match backend field
                }),
            });

            const data = await res.text(); // backend returns a plain string

            if (res.ok) {
                alert(data); // show backend message
                navigate("/verify", { state: { email } });
            } else {
                alert(data);
            }
        } catch (err) {
            console.error(err);
            alert("Error connecting to backend");
        }
    };

    return (
        <div className="card">
            <h2>Register 📝</h2>
            <form onSubmit={handleRegister}>
                <input
                    type="text"
                    placeholder="Full Name"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    required
                />
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
                <button type="submit" className="btn btn-primary">Register</button>
            </form>
            <p>
                Already have an account? <span className="link" onClick={() => navigate("/login")}>Login</span>
            </p>
        </div>
    );
}

export default Registrationpage;
