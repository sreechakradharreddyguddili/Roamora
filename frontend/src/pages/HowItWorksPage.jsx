import {
  ArrowDown,
  ArrowRight,
  CloudSun,
  Compass,
  MapPinned,
  ShieldCheck,
  Navigation,
  CheckCircle2,
} from "lucide-react";

import { Link } from "react-router-dom";

import "./HowItWorksPage.css";
import { handleImageError } from "../utils/imageFallback";

const steps = [
  {
    number: "01",
    icon: MapPinned,
    title: "Enter your journey",
    text:
      "Start with your origin and destination. Roamora turns your trip into a route that can be understood and evaluated.",
  },
  {
    number: "02",
    icon: Compass,
    title: "Explore route options",
    text:
      "Look beyond a single path. Compare available routes using the information that matters to your journey.",
  },
  {
    number: "03",
    icon: CloudSun,
    title: "Understand conditions",
    text:
      "Bring weather and journey conditions into the route instead of treating them as separate information.",
  },
  {
    number: "04",
    icon: ShieldCheck,
    title: "Evaluate risk",
    text:
      "Use the available journey context to understand potential risk and conditions along the way.",
  },
  {
    number: "05",
    icon: Navigation,
    title: "Make your decision",
    text:
      "Choose the route that makes sense for your journey with the important context visible in one place.",
  },
];

export default function HowItWorksPage() {
  return (
    <main className="how-page">

      {/* HERO */}

      <section className="how-hero">

        <div className="how-hero-copy">

          <span className="how-eyebrow">
            ROAMORA / HOW IT WORKS
          </span>

          <h1>
            From destination
            <span> to decision.</span>
          </h1>

          <p>
            Roamora connects every important part of
            the journey so you can understand your route
            before you take it.
          </p>

          <Link
            to="/planner"
            className="how-primary-button"
          >
            Try the planner
            <ArrowRight size={17} />
          </Link>

        </div>


        <div className="how-process-visual how-process-photo">
          <img src="https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?auto=format&fit=crop&w=1200&q=82" alt="Mountain road and peaks in a scenic destination" loading="lazy" onError={handleImageError} />
          <div className="how-process-caption"><span>YOUR JOURNEY, IN FOUR STEPS</span><strong>Choose. Compare. Prepare. Go.</strong></div>
        </div>

      </section>


      {/* STEPS */}

      <section className="how-steps">

        <div className="how-heading">

          <span>
            01 / THE PROCESS
          </span>

          <h2>
            Planning becomes
            <em> clearer.</em>
          </h2>

        </div>


        <div className="how-step-list">

          {steps.map((step, index) => {

            const Icon = step.icon;

            return (
              <article
                className="how-step"
                key={step.number}
              >

                <div className="how-step-number">
                  {step.number}
                </div>

                <div className="how-step-icon">
                  <Icon size={24} />
                </div>

                <div className="how-step-content">

                  <h3>
                    {step.title}
                  </h3>

                  <p>
                    {step.text}
                  </p>

                </div>

                {index < steps.length - 1 && (
                  <ArrowDown className="how-step-arrow" />
                )}

              </article>
            );
          })}

        </div>

      </section>


      {/* DECISION */}

      <section className="how-decision">

        <div className="how-decision-copy">

          <span>
            02 / THE RESULT
          </span>

          <h2>
            Don't just find
            <br />
            a route.
            <em>
              understand it.
            </em>
          </h2>

          <p>
            Roamora is designed to bring the information
            around a journey together instead of making
            you piece it together yourself.
          </p>

          <div className="how-checks">

            <div>
              <CheckCircle2 size={17} />
              Route context
            </div>

            <div>
              <CheckCircle2 size={17} />
              Weather context
            </div>

            <div>
              <CheckCircle2 size={17} />
              Risk context
            </div>

          </div>

        </div>


        <div className="how-decision-visual how-decision-photo">
          <img
            src="https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=1400&q=85"
            alt="A scenic travel landscape with a winding route through the countryside"
            loading="lazy"
            onError={handleImageError}
          />
          <div className="how-decision-photo-caption">
            <span>THE BIGGER PICTURE</span>
            <strong>Every journey has more context.</strong>
          </div>
        </div>

      </section>


      {/* CTA */}

      <section className="how-cta">

        <span>
          READY?
        </span>

        <h2>
          Plan with
          <em> context.</em>
        </h2>

        <Link
          to="/planner"
          className="how-cta-button"
        >
          Open planner
          <ArrowRight size={17} />
        </Link>

      </section>

    </main>
  );
}
