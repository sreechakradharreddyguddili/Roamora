import {
  ArrowRight,
  CloudSun,
  Compass,
  ShieldCheck,
  Navigation,
  Sparkles,
  Route,
} from "lucide-react";

import { Link } from "react-router-dom";

import "./FeaturesPage.css";

const features = [
  {
    number: "01",
    icon: Compass,
    title: "Route Intelligence",
    description:
      "Understand where you are going and compare the important route alternatives before you start.",
  },
  {
    number: "02",
    icon: CloudSun,
    title: "Weather Intelligence",
    description:
      "See weather conditions across the journey instead of checking isolated locations.",
  },
  {
    number: "03",
    icon: ShieldCheck,
    title: "Travel Risk",
    description:
      "Turn changing journey conditions into a clear signal that helps you understand potential risks.",
  },
  {
    number: "04",
    icon: Navigation,
    title: "Decision Support",
    description:
      "Bring route, weather and risk information together so your planning starts with better context.",
  },
];

export default function FeaturesPage() {
  return (
    <main className="features-page">

      {/* HERO */}

      <section className="features-hero">

        <div className="features-hero-copy">

          <span className="features-eyebrow">
            ROAMORA / FEATURES
          </span>

          <h1>
            Everything you need
            <span> to understand the journey.</span>
          </h1>

          <p>
            Roamora brings route intelligence, weather,
            travel risk and decision support together
            into one focused planning experience.
          </p>

          <Link
            to="/planner"
            className="features-primary-button"
          >
            Plan your journey
            <ArrowRight size={17} />
          </Link>

        </div>


        <div className="features-hero-visual">

          <div className="features-orbit orbit-one" />
          <div className="features-orbit orbit-two" />
          <div className="features-orbit orbit-three" />

          <div className="features-center">
            <Sparkles size={30} />
          </div>

          <div className="features-floating-card card-route">
            <Route size={16} />
            <span>Route</span>
          </div>

          <div className="features-floating-card card-weather">
            <CloudSun size={16} />
            <span>Weather</span>
          </div>

          <div className="features-floating-card card-risk">
            <ShieldCheck size={16} />
            <span>Risk</span>
          </div>

        </div>

      </section>


      {/* FEATURES */}

      <section className="features-list-section">

        <div className="features-section-heading">

          <span>
            01 / WHAT ROAMORA DOES
          </span>

          <h2>
            Four layers.
            <br />
            <em>One clearer journey.</em>
          </h2>

        </div>


        <div className="features-grid">

          {features.map((feature) => {

            const Icon = feature.icon;

            return (
              <article
                className="feature-card"
                key={feature.number}
              >

                <div className="feature-card-top">

                  <span>
                    {feature.number}
                  </span>

                  <Icon size={23} />

                </div>

                <div className="feature-card-icon">
                  <Icon size={28} />
                </div>

                <h3>
                  {feature.title}
                </h3>

                <p>
                  {feature.description}
                </p>

              </article>
            );
          })}

        </div>

      </section>


      {/* FLOW */}

      <section className="features-flow">

        <div className="features-flow-heading">

          <span>
            02 / CONNECTED INTELLIGENCE
          </span>

          <h2>
            Information becomes
            <em> context.</em>
          </h2>

        </div>


        <div className="features-flow-line">

          <div className="flow-item">
            <span>01</span>
            <strong>Route</strong>
            <small>Where are you going?</small>
          </div>

          <div className="flow-arrow">
            <ArrowRight />
          </div>

          <div className="flow-item">
            <span>02</span>
            <strong>Weather</strong>
            <small>What will you encounter?</small>
          </div>

          <div className="flow-arrow">
            <ArrowRight />
          </div>

          <div className="flow-item">
            <span>03</span>
            <strong>Risk</strong>
            <small>What should you consider?</small>
          </div>

          <div className="flow-arrow">
            <ArrowRight />
          </div>

          <div className="flow-item">
            <span>04</span>
            <strong>Decision</strong>
            <small>Which route makes sense?</small>
          </div>

        </div>

      </section>


      {/* CTA */}

      <section className="features-cta">

        <div>

          <span>
            READY TO EXPLORE?
          </span>

          <h2>
            See Roamora
            <em> in action.</em>
          </h2>

        </div>

        <Link
          to="/explore"
          className="features-cta-button"
        >
          Explore Roamora
          <ArrowRight size={17} />
        </Link>

      </section>

    </main>
  );
}