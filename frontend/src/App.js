import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import Welcomepage from "./welcomepage";
import Loginpage from "./loginpage";
import Registrationpage from "./registrationpage";
import Verificationpage from "./verificationpage";
import VerificationPage2 from "./VerificationPage2";
import TouristSelectPage from "./TouristSelectPage";
import ReviewPage from "./Review";

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<Welcomepage />} />
        <Route path="/login" element={<Loginpage />} />
        <Route path="/register" element={<Registrationpage />} />
        <Route path="/verify" element={<Verificationpage />} />
        <Route path="/verify2" element={<VerificationPage2 />} />
        <Route path="/select-place" element={<TouristSelectPage />} />
        <Route path="/review" element={<ReviewPage />} />
      </Routes>
    </Router>
  );
}

export default App;
