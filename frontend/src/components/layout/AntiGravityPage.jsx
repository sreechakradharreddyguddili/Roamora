import { useEffect, useRef } from "react";

import "./AntiGravityPage.css";

const TARGET_SELECTOR = [
  "main article",
  "main [class*='-card']",
  "main [class*='-panel']",
  "main [class*='-tile']",
  "main [class*='-visual']",
  "main [class*='-stat']",
  "main [class*='-floating']",
].join(",");

function hash(value) {
  let result = 0;
  for (let index = 0; index < value.length; index += 1) {
    result = (result * 31 + value.charCodeAt(index)) | 0;
  }
  return Math.abs(result);
}

export default function AntiGravityPage({ children, className = "" }) {
  const scopeRef = useRef(null);

  useEffect(() => {
    const scope = scopeRef.current;
    if (!scope) return undefined;

    const reduceMotion = window.matchMedia(
      "(prefers-reduced-motion: reduce)"
    );

    const elements = new Set();
    let frame = null;
    let ticking = false;
    let observer = null;
    let pointerX = 0;
    let pointerY = 0;
    let pointerActive = false;

    const register = () => {
      scope.querySelectorAll(TARGET_SELECTOR).forEach((element) => {
        if (element.closest(".no-anti-gravity")) return;
        if (element.dataset.agRegistered === "true") return;

        element.dataset.agRegistered = "true";
        element.classList.add("anti-gravity-item");
        element.style.setProperty(
          "--ag-phase",
          `${(hash(`${element.tagName}-${element.className}`) % 628) / 100}`
        );
        elements.add(element);
      });
    };

    const reset = () => {
      elements.forEach((element) => {
        element.style.translate = "0 0";
        element.style.rotate = "0deg";
        element.style.scale = "1";
      });
    };

    const update = () => {
      ticking = false;
      if (reduceMotion.matches) {
        reset();
        return;
      }

      const viewport = window.innerHeight || 800;
      const scrollY = window.scrollY || 0;

      elements.forEach((element) => {
        if (!element.isConnected) {
          elements.delete(element);
          return;
        }

        const rect = element.getBoundingClientRect();
        if (rect.bottom < -180 || rect.top > viewport + 180) {
          return;
        }

        const phase = Number(element.style.getPropertyValue("--ag-phase")) || 0;
        const centerDistance =
          (rect.top + rect.height / 2 - viewport / 2) / viewport;
        const drift = Math.max(-1.4, Math.min(1.4, centerDistance));
        const wave = Math.sin(scrollY * 0.0012 + phase);
        const cross = Math.cos(scrollY * 0.0009 + phase * 1.7);
        const hover = element.dataset.agHover === "true" ? 1 : 0;
        const pointerInfluence = pointerActive ? 1 : 0;

        const x =
          wave * 2.8 +
          drift * ((phase % 2) - 1) * 2.2 +
          pointerX * (1.6 + (phase % 1.5)) * pointerInfluence;
        const y =
          cross * 2.4 -
          drift * 2.1 +
          pointerY * (1.2 + (phase % 1.1)) * pointerInfluence;
        const rotation =
          wave * 0.55 + drift * 0.45 + pointerX * 0.12 * pointerInfluence;
        const scale =
          1 + hover * 0.012 + Math.abs(drift) * 0.006 +
          Math.min(0.008, Math.hypot(pointerX, pointerY) * 0.0025) * pointerInfluence;

        element.style.translate = `${x.toFixed(2)}px ${y.toFixed(2)}px`;
        element.style.rotate = `${rotation.toFixed(2)}deg`;
        element.style.scale = scale.toFixed(4);
      });
    };

    const requestUpdate = () => {
      if (ticking) return;
      ticking = true;
      frame = requestAnimationFrame(update);
    };

    const attachHover = () => {
      elements.forEach((element) => {
        if (element.dataset.agHoverBound === "true") return;
        element.dataset.agHoverBound = "true";

        const enter = () => {
          element.dataset.agHover = "true";
          requestUpdate();
        };
        const leave = () => {
          element.dataset.agHover = "false";
          requestUpdate();
        };

        element.addEventListener("pointerenter", enter, { passive: true });
        element.addEventListener("pointerleave", leave, { passive: true });
        element._roamoraAgCleanup = () => {
          element.removeEventListener("pointerenter", enter);
          element.removeEventListener("pointerleave", leave);
        };
      });
    };

    const scan = () => {
      register();
      attachHover();
      requestUpdate();
    };

    observer = new MutationObserver(scan);
    observer.observe(scope, { childList: true, subtree: true });

    const handlePointerMove = (event) => {
      pointerX = ((event.clientX / window.innerWidth) - 0.5) * 2;
      pointerY = ((event.clientY / window.innerHeight) - 0.5) * 2;
      pointerActive = true;
      requestUpdate();
    };

    const handlePointerLeave = () => {
      pointerX = 0;
      pointerY = 0;
      pointerActive = false;
      requestUpdate();
    };

    window.addEventListener("scroll", requestUpdate, { passive: true });
    window.addEventListener("resize", requestUpdate, { passive: true });
    window.addEventListener("pointermove", handlePointerMove, { passive: true });
    window.addEventListener("pointerleave", handlePointerLeave, { passive: true });
    reduceMotion.addEventListener?.("change", requestUpdate);

    scan();

    return () => {
      observer?.disconnect();
      window.removeEventListener("scroll", requestUpdate);
      window.removeEventListener("resize", requestUpdate);
      window.removeEventListener("pointermove", handlePointerMove);
      window.removeEventListener("pointerleave", handlePointerLeave);
      reduceMotion.removeEventListener?.("change", requestUpdate);
      if (frame) cancelAnimationFrame(frame);

      elements.forEach((element) => {
        element._roamoraAgCleanup?.();
        delete element._roamoraAgCleanup;
        delete element.dataset.agRegistered;
        delete element.dataset.agHoverBound;
        delete element.dataset.agHover;
        element.classList.remove("anti-gravity-item");
        element.style.translate = "";
        element.style.rotate = "";
        element.style.scale = "";
      });
    };
  }, []);

  return (
    <div ref={scopeRef} className={`anti-gravity-scope ${className}`.trim()}>
      {children}
    </div>
  );
}
