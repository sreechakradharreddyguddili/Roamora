import { ArrowLeft, Compass } from "lucide-react";
import { Link } from "react-router-dom";

import "./NotFoundPage.css";

export default function NotFoundPage() {
  return (
    <main className="roamora-not-found">
      <div className="not-found-orbit orbit-one" />
      <div className="not-found-orbit orbit-two" />
      <div className="not-found-card">
        <div className="not-found-icon"><Compass size={26} /></div>
        <span>404 / OFF THE MAP</span>
        <h1>This route doesn't exist.</h1>
        <p>The destination you're looking for isn't part of Roamora yet. Return to the journey hub and start again.</p>
        <Link to="/" className="not-found-link"><ArrowLeft size={16} /> Back to Roamora</Link>
      </div>
    </main>
  );
}
