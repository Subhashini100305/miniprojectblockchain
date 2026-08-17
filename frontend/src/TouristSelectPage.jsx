import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import Select from "react-select";
import "./App.css";

export default function TouristSelectPage() {
    const navigate = useNavigate();

    const [inputValue, setInputValue] = useState("");
    const [options, setOptions] = useState([]);
    const [selectedPlace, setSelectedPlace] = useState(null);

    // 🔍 Fetch from Nominatim
    const fetchPlaces = async (query) => {
        if (query.length < 3) return;

        try {
            const response = await fetch(
                `https://nominatim.openstreetmap.org/search?q=${query}&format=json&addressdetails=1&limit=5`,
                {
                    headers: {
                        "User-Agent": "tourist-app (subhashini10.3.2005@gmail.com)",
                    },
                }
            );

            const data = await response.json();

            const formatted = data.map((place) => ({
                label: place.display_name,
                value: place.display_name,
                lat: place.lat,
                lon: place.lon,
            }));

            setOptions(formatted);
        } catch (error) {
            console.error("Error fetching places:", error);
        }
    };


    useEffect(() => {
        const delay = setTimeout(() => {
            fetchPlaces(inputValue);
        }, 500); // 500ms delay

        return () => clearTimeout(delay);
    }, [inputValue]);

    const handleSubmit = (e) => {
        e.preventDefault();

        if (!selectedPlace) {
            alert("Please select a tourist place!");
            return;
        }

        localStorage.setItem("selectedPlace", selectedPlace.value);
        localStorage.setItem("selectedLat", selectedPlace.lat);
        localStorage.setItem("selectedLon", selectedPlace.lon);
        navigate("/verify2");
    };

    return (
        <div className="card">
            <h2>Select Tourist Place </h2>

            <form onSubmit={handleSubmit}>
                <Select
                    options={options}
                    value={selectedPlace}
                    onChange={setSelectedPlace}
                    onInputChange={(value) => setInputValue(value)}
                    placeholder="Start typing a place..."
                    isSearchable
                    noOptionsMessage={() => "Type at least 3 letters"}
                />

                <button type="submit" className="btn btn-primary">
                    Continue
                </button>
            </form>
        </div>
    );
}