import { useNavigate } from "react-router-dom";
import "./App.css";
function Welcomepage() {
    const navigate = useNavigate();

    return (
        <div className="card">
        <h1>Welcome to Review System 🌟</h1>
        <p>Please choose an option below:</p>
        <button className="btn btn-primary" onClick={() => navigate("/register")}>
            Register
        </button>
        <button className="btn btn-secondary" onClick={() => navigate("/login")}>
            Login
        </button>
        </div>
    );
}

export default Welcomepage;
