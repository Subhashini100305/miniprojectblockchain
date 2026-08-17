import { useEffect, useState } from "react";
import { Navigate } from "react-router-dom";
import { getAuthHeader } from "./utils/auth";

export default function ProtectedRoute({ children }) {
    const [isAuthenticated, setIsAuthenticated] = useState(null);

    useEffect(() => {
        const token = localStorage.getItem("token");

        if (!token) {
            setIsAuthenticated(false);
            return;
        }

        fetch("http://localhost:8080/api/auth/validate", {
            headers: getAuthHeader()
        })
            .then((response) => {
                if (!response.ok) {
                    localStorage.removeItem("token");
                }

                setIsAuthenticated(response.ok);
            })
            .catch(() => {
                localStorage.removeItem("token");
                setIsAuthenticated(false);
            });
    }, []);

    if (isAuthenticated === null) {
        return null;
    }

    return isAuthenticated
        ? children
        : <Navigate to="/login" replace />;
}
