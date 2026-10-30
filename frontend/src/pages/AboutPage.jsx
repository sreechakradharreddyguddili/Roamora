import {
  ArrowRight,
  CloudSun,
  ShieldCheck,
  Route,
  Sparkles,
  Navigation,
} from "lucide-react";

import { Link } from "react-router-dom";

import "./AboutPage.css";
import { handleImageError } from "../utils/imageFallback";

export default function AboutPage() {
  return (
    <main className="about-page">

      {/* HERO */}

      <section className="about-hero">

        <div className="about-hero-copy">

          <span className="about-eyebrow">
            ROAMORA / ABOUT
          </span>

          <h1>
            A better journey
            <span> starts with context.</span>
          </h1>

          <p>
            Roamora is built around a simple idea:
            planning a journey should mean understanding
            more than just the road between two places.
          </p>

          <Link
            to="/explore"
            className="about-primary-button"
          >
            Explore Roamora
            <ArrowRight size={17} />
          </Link>

        </div>


        <div className="about-hero-photo">
          <img
            src="https://images.unsplash.com/photo-1476514525535-07fb3b4ae5f1?auto=format&fit=crop&w=1400&q=85"
            alt="A peaceful alpine lake and mountain landscape, representing thoughtful travel"
            loading="lazy"
            onError={handleImageError}
          />
          <div className="about-hero-photo-caption">
            <span>ROAM WITH CONTEXT</span>
            <strong>See more than the distance.</strong>
          </div>
        </div>

      </section>


      {/* STORY */}

      <section className="about-story">

        <div className="about-story-label">
          01 / THE IDEA
        </div>

        <div className="about-story-content">

          <h2>
            Routes are only
            <em> one part</em>
            of a journey.
          </h2>

          <p>
            A route can look perfect on a map while
            the conditions around it tell a different
            story. Weather changes. Roads change.
            Conditions change.
          </p>

          <p>
            Roamora brings these pieces together so
            travelers can see the journey as a whole
            instead of making decisions from isolated
            information.
          </p>

        </div>

      </section>


      {/* PRINCIPLES */}

      <section className="about-principles">

        <div className="about-principles-heading">

          <span>
            02 / WHAT WE BELIEVE
          </span>

          <h2>
            Simple information.
            <br />
            <em>Better decisions.</em>
          </h2>

        </div>


        <div className="about-principles-grid">

          <article>
            <div>
              <Route size={22} />
            </div>

            <span>01</span>

            <h3>
              Context over distance
            </h3>

            <p>
              A shorter route is not automatically
              the route that makes the most sense.
            </p>
          </article>


          <article>
            <div>
              <CloudSun size={22} />
            </div>

            <span>02</span>

            <h3>
              Conditions matter
            </h3>

            <p>
              Weather and journey conditions should
              be part of the planning experience.
            </p>
          </article>


          <article>
            <div>
              <ShieldCheck size={22} />
            </div>

            <span>03</span>

            <h3>
              Risk should be understandable
            </h3>

            <p>
              Complex journey conditions should be
              presented in a way that is easy to understand.
            </p>
          </article>


          <article>
            <div>
              <Navigation size={22} />
            </div>

            <span>04</span>

            <h3>
              The traveler decides
            </h3>

            <p>
              Roamora brings context together so the
              traveler can make the final decision.
            </p>
          </article>

        </div>

      </section>


      {/* VISION */}

      <section className="about-vision">

        <div className="about-vision-symbol">
          <Sparkles size={35} />
        </div>

        <span>
          03 / OUR VISION
        </span>

        <h2>
          Make every journey
          <br />
          <em>more understandable.</em>
        </h2>

        <p>
          Roamora is designed to become a focused
          intelligence layer between the traveler and
          the journey — helping turn information into
          understanding.
        </p>

      </section>


      {/* CTA */}

      <section className="about-cta">

        <div>

          <span>
            YOUR JOURNEY STARTS HERE
          </span>

          <h2>
            See what is
            <em> ahead.</em>
          </h2>

        </div>

        <Link
          to="/planner"
          className="about-cta-button"
        >
          Start planning
          <ArrowRight size={17} />
        </Link>

      </section>

    </main>
  );
}