import { useEffect, useState } from "react";
import { storage } from "./storage";

export function useStorageState(type, initial) {
  const [value, setValue] = useState(initial);
  useEffect(() => {
    const handler = () => setValue(type === "history" ? storage.getHistory() : storage.getSaved());
    window.addEventListener("roamora:storage", handler);
    return () => window.removeEventListener("roamora:storage", handler);
  }, [type]);
  return [value, setValue];
}
