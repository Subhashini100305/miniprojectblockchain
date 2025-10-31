import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import Select from "react-select";
import "./App.css";

export default function TouristSelectPage() {
    const navigate = useNavigate();
    const [selectedPlace, setSelectedPlace] = useState(null);

    // 🏞️ Full list of major Indian tourist places
    const touristPlaces = [
        "Taj Mahal (Agra)",
        "Jaipur City Palace (Rajasthan)",
        "Hampi (Karnataka)",
        "Mysore Palace (Karnataka)",
        "Gateway of India (Mumbai)",
        "Meenakshi Temple (Madurai)",
        "Charminar (Hyderabad)",
        "Qutub Minar (Delhi)",
        "Red Fort (Delhi)",
        "Golden Temple (Amritsar)",
        "Backwaters (Kerala)",
        "Rann of Kutch (Gujarat)",
        "Sundarbans (West Bengal)",
        "Kedarnath Temple (Uttarakhand)",
        "Badrinath Temple (Uttarakhand)",
        "Manali (Himachal Pradesh)",
        "Shimla (Himachal Pradesh)",
        "Ladakh (Jammu & Kashmir)",
        "Dal Lake (Srinagar)",
        "Darjeeling (West Bengal)",
        "Ooty (Tamil Nadu)",
        "Coorg (Karnataka)",
        "Munnar (Kerala)",
        "Kodaikanal (Tamil Nadu)",
        "Andaman Islands",
        "Rameswaram (Tamil Nadu)",
        "Ellora Caves (Maharashtra)",
        "Ajanta Caves (Maharashtra)",
        "Khajuraho Temples (Madhya Pradesh)",
        "Konark Sun Temple (Odisha)",
        "Chikmagalur (Karnataka)",
        "Mahabalipuram (Tamil Nadu)",
        "Varanasi Ghats (Uttar Pradesh)",
        "Sanchi Stupa (Madhya Pradesh)",
        "Lonavala (Maharashtra)",
        "Mount Abu (Rajasthan)",
        "Puri Jagannath Temple (Odisha)",
        "Kaziranga National Park (Assam)",
        "Jim Corbett Park (Uttarakhand)",
        "Valley of Flowers (Uttarakhand)",
        "Gulmarg (Jammu & Kashmir)",
        "Kovalam Beach (Kerala)",
        "Varkala Beach (Kerala)",
        "Hampi Ruins (Karnataka)",
        "Amarnath Cave (J&K)",
        "Dwarkadhish Temple (Gujarat)",
        "Somnath Temple (Gujarat)",
        "Pangong Lake (Ladakh)",
        "Auroville (Pondicherry)",
        "Thanjavur Temple (Tamil Nadu)",
        "Rishikesh (Uttarakhand)",
        "Haridwar (Uttarakhand)",
        "Bandhavgarh National Park (Madhya Pradesh)",
        "Kanha National Park (Madhya Pradesh)",
        "Hogenakkal Falls (Tamil Nadu)",
        "Athirapally Falls (Kerala)",
        "Belur & Halebidu (Karnataka)",
        "Chennai Marina Beach (Tamil Nadu)",
        "Bangalore Lalbagh Garden (Karnataka)",
        "Gangaikonda Cholapuram (Tamil Nadu)",
        "Mahabaleshwar (Maharashtra)",
        "Pondicherry Promenade (Puducherry)",
        "Agumbe (Karnataka)",
        "Tawang Monastery (Arunachal Pradesh)",
        "Ziro Valley (Arunachal Pradesh)",
        "Spiti Valley (Himachal Pradesh)",
        "Nainital (Uttarakhand)",
        "Ranthambore National Park (Rajasthan)",
        "Udaipur Lake Palace (Rajasthan)",
        "Pushkar (Rajasthan)",
        "Jaisalmer Fort (Rajasthan)",
        "Amer Fort (Jaipur)",
        "Gokarna Beach (Karnataka)",
        "Alleppey (Kerala)",
        "Cherrapunji (Meghalaya)",
        "Shillong (Meghalaya)",
        "Sikkim (Gangtok)",
        "Madurai (Tamil Nadu)",
        "Vijayawada Kanaka Durga Temple (Andhra Pradesh)",
        "Visakhapatnam Beaches (Andhra Pradesh)",
        "Bhubaneswar Temples (Odisha)",
        "Patna Sahib (Bihar)",
        "Nalanda Ruins (Bihar)",
        "Diu Fort (Diu)",
        "Daman Beach (Daman)",
        "Ranakpur Temple (Rajasthan)",
        "Murudeshwar Temple (Karnataka)",
        "Sringeri Temple (Karnataka)",
        "Pachmarhi (Madhya Pradesh)",
        "Gir National Park (Gujarat)",
        "Ravangla (Sikkim)",
        "Aizawl (Mizoram)",
        "Imphal (Manipur)",
        "Majuli Island (Assam)",
        "Loktak Lake (Manipur)",
        "Lakshadweep Islands",
    ].map((place) => ({ label: place, value: place }));

    const handleSubmit = (e) => {
        e.preventDefault();
        if (!selectedPlace) {
            alert("Please select a tourist place!");
            return;
        }

        localStorage.setItem("selectedPlace", selectedPlace.value);
        navigate("/verify2"); // Redirect to VerificationPage2
    };

    return (
        <div className="card">
            <h2>Select Tourist Place 🏞️</h2>

            <form onSubmit={handleSubmit}>
                <Select
                    options={touristPlaces}
                    value={selectedPlace}
                    onChange={setSelectedPlace}
                    placeholder="Search or select a tourist place..."
                    isSearchable
                    styles={{
                        control: (base) => ({
                            ...base,
                            backgroundColor: "white",
                            borderRadius: "8px",
                            marginBottom: "15px",
                        }),
                    }}
                />

                <button type="submit" className="btn btn-primary">
                    Continue
                </button>
            </form>
        </div>
    );
}
