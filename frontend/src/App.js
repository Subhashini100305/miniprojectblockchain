import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import Welcomepage from "./welcomepage";
import Loginpage from "./loginpage";
import Registrationpage from "./registrationpage";
import Verificationpage from "./verificationpage";
import VerificationPage2 from "./VerificationPage2";
import TouristSelectPage from "./TouristSelectPage";
import ReviewPage from "./Review";
import Dashboard from "./Dashboard";
import PlaceReviews from "./PlaceReviews";
import MyReviews from "./MyReviews";
import ProtectedRoute from "./ProtectedRoute";

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<Welcomepage />} />
        <Route path="/login" element={<Loginpage />} />
        <Route path="/register" element={<Registrationpage />} />
        <Route path="/verify" element={<Verificationpage />} />
        <Route
          path="/verify2"
          element={
            <ProtectedRoute>
              <VerificationPage2 />
            </ProtectedRoute>
          }
        />
        <Route
          path="/select-place"
          element={
            <ProtectedRoute>
              <TouristSelectPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/review"
          element={
            <ProtectedRoute>
              <ReviewPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/dashboard"
          element={
            <ProtectedRoute>
              <Dashboard />
            </ProtectedRoute>
          }
        />
        <Route
          path="/place-reviews"
          element={
            <ProtectedRoute>
              <PlaceReviews />
            </ProtectedRoute>
          }
        />
        <Route
          path="/my-reviews"
          element={
            <ProtectedRoute>
              <MyReviews />
            </ProtectedRoute>
          }
        />
      </Routes>
    </Router>
  );
}

export default App;
