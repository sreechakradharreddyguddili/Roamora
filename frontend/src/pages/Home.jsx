import { ArrowRight, Compass, MapPin, ShieldCheck, Utensils, BedDouble, Route, CloudSun } from "lucide-react";
import { Link, useNavigate } from "react-router-dom";
import { useState } from "react";
import "./Home.css";
import { handleImageError } from "../utils/imageFallback";

const destinations = [
  { name: "Kyoto", country: "Japan", image: "https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?auto=format&fit=crop&w=900&q=82", alt: "Traditional Kyoto street and Japanese architecture" },
  { name: "Paris", country: "France", image: "https://images.unsplash.com/photo-1502602898657-3e91760cbb34?auto=format&fit=crop&w=900&q=82", alt: "Paris skyline with the Eiffel Tower" },
  { name: "Marrakech", country: "Morocco", image: "https://images.unsplash.com/photo-1539020140153-e479b8c22e70?auto=format&fit=crop&w=900&q=82", alt: "Warm-toned architecture in Morocco" },
  { name: "Cape Town", country: "South Africa", image: "https://images.unsplash.com/photo-1580060839134-75a5edca2e99?auto=format&fit=crop&w=900&q=82", alt: "Cape Town and Table Mountain" },
];

const features = [
  { icon: Route, title: "Journeys with context", copy: "Compare supported routes, journey times and useful travel details in one place.", link: "/planner" },
  { icon: CloudSun, title: "Conditions that matter", copy: "Review available weather signals and understand what they may mean for your plans.", link: "/journey-intelligence" },
  { icon: BedDouble, title: "A place to stay", copy: "Discover mapped accommodation around the destination you choose.", link: "/stays" },
  { icon: Utensils, title: "Find local flavour", copy: "Search mapped restaurants and cafés, then open their location in your map app.", link: "/explore" },
];

export default function Home() {
  const [destinationQuery, setDestinationQuery] = useState("");
  const navigate = useNavigate();
  const handleDestinationSearch = (event) => {
    event.preventDefault();
    const value = destinationQuery.trim();
    if (value.length >= 2) navigate(`/explore?destination=${encodeURIComponent(value)}`);
  };

  return <main className="home-page">
    <section className="home-hero">
      <div className="home-hero-copy">
        <div className="home-kicker"><Compass size={15} /> Travel, with more context</div>
        <h1>Go somewhere.<br/><em>Feel ready for it.</em></h1>
        <p>Plan your journey, discover places worth stopping for, and understand the details that can shape your trip — without the noise.</p>
        <form className="home-quick-search" onSubmit={handleDestinationSearch} role="search">
          <MapPin size={18}/>
          <input aria-label="Search a destination" value={destinationQuery} onChange={(event)=>setDestinationQuery(event.target.value)} placeholder="Where do you want to go?" />
          <button type="submit" disabled={destinationQuery.trim().length < 2}>Search <ArrowRight size={15}/></button>
        </form>
        <div className="home-hero-actions">
          <Link className="home-button home-button-primary" to="/planner">Plan a journey <ArrowRight size={17}/></Link>
          <Link className="home-button home-button-secondary" to="/explore">Explore destinations</Link>
        </div>
        <div className="home-trust-note"><ShieldCheck size={16}/> Clear about what is verified — and what is not.</div>
      </div>
      <div className="home-hero-photo-wrap">
        <img className="home-hero-photo" src="https://images.unsplash.com/photo-1476514525535-07fb3b4ae5f1?auto=format&fit=crop&w=1400&q=85" alt="A lake surrounded by mountains on a clear travel day" fetchPriority="high" onError={handleImageError} />
        <div className="home-photo-caption"><span>TAKE THE SCENIC WAY</span><strong>Make the journey part of the story.</strong></div>
      </div>
    </section>

    <section className="home-destinations section-shell">
      <div className="home-section-heading"><div><span className="home-eyebrow">A little inspiration</span><h2>Somewhere new <em>is calling.</em></h2></div><Link to="/explore" className="home-text-link">Explore all places <ArrowRight size={16}/></Link></div>
      <div className="destination-grid">{destinations.map((destination) => <Link className="destination-card" key={destination.name} to={`/explore?destination=${encodeURIComponent(`${destination.name}, ${destination.country}`)}`}>
        <img src={destination.image} alt={destination.alt} loading="lazy" onError={handleImageError} />
        <div className="destination-card-shade"/><div className="destination-card-label"><span>{destination.country}</span><strong>{destination.name}</strong></div><span className="destination-card-arrow"><ArrowRight size={17}/></span>
      </Link>)}</div>
    </section>

    <section className="home-tools-section"><div className="section-shell"><div className="home-section-heading"><div><span className="home-eyebrow">One trip, fewer tabs</span><h2>Useful travel tools.<br/><em>All in one place.</em></h2></div><p>Keep the essentials close, from your first route search to finding somewhere good to eat.</p></div><div className="home-feature-grid">{features.map(({icon:Icon,title,copy,link})=><Link to={link} className="home-feature-card" key={title}><span className="home-feature-icon"><Icon size={21}/></span><h3>{title}</h3><p>{copy}</p><span className="home-feature-link">Open feature <ArrowRight size={15}/></span></Link>)}</div></div></section>

    <section className="home-bottom-cta section-shell"><div><span className="home-eyebrow">Your next trip starts here</span><h2>Less guesswork.<br/><em>More going places.</em></h2><p>Start with a destination. Roamora will help you explore the travel information it can verify.</p></div><Link to="/planner" className="home-button home-button-light">Start planning <ArrowRight size={17}/></Link></section>
  </main>;
}
