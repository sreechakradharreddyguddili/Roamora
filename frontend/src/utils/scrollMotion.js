import { useEffect } from "react";

export function useScrollMotion() {
  useEffect(() => {
    const elements = [...document.querySelectorAll("[data-motion]")];

    if (!elements.length) return;

    let frame = null;
    let ticking = false;

    const update = () => {
      const viewportHeight = window.innerHeight;

      elements.forEach((element) => {
        const rect = element.getBoundingClientRect();

        const center = rect.top + rect.height / 2;

        const distance =
          (center - viewportHeight / 2) / viewportHeight;

        const strength =
          Number(element.dataset.strength || 1);

        const x =
          Number(element.dataset.x || 0) *
          distance *
          strength;

        const y =
          Number(element.dataset.y || 0) *
          distance *
          strength;

        const rotate =
          Number(element.dataset.rotate || 0) *
          distance *
          strength;

        const scale =
          1 +
          Number(element.dataset.scale || 0) *
          Math.abs(distance) *
          strength;

        element.style.setProperty(
          "--motion-x",
          `${x}px`
        );

        element.style.setProperty(
          "--motion-y",
          `${y}px`
        );

        element.style.setProperty(
          "--motion-rotate",
          `${rotate}deg`
        );

        element.style.setProperty(
          "--motion-scale",
          scale
        );
      });

      ticking = false;
    };

    const requestUpdate = () => {
      if (!ticking) {
        frame = requestAnimationFrame(update);
        ticking = true;
      }
    };

    window.addEventListener(
      "scroll",
      requestUpdate,
      { passive: true }
    );

    window.addEventListener(
      "resize",
      requestUpdate
    );

    update();

    return () => {
      window.removeEventListener(
        "scroll",
        requestUpdate
      );

      window.removeEventListener(
        "resize",
        requestUpdate
      );

      if (frame) {
        cancelAnimationFrame(frame);
      }
    };
  }, []);
}


export function useReveal() {
  useEffect(() => {
    const elements =
      document.querySelectorAll(".reveal");

    if (!elements.length) return;

    const observer =
      new IntersectionObserver(
        (entries) => {
          entries.forEach((entry) => {
            if (entry.isIntersecting) {
              entry.target.classList.add(
                "is-visible"
              );

              observer.unobserve(entry.target);
            }
          });
        },
        {
          threshold: 0.12,
          rootMargin: "0px 0px -70px 0px",
        }
      );

    elements.forEach((element) => {
      observer.observe(element);
    });

    return () => observer.disconnect();
  }, []);
}