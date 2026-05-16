import React from "react";
export default function Button({ children, ...props }) {
  return (
    <button
      {...props}
      style={{
        padding: "10px 14px",
        borderRadius: 8,
        border: "1px solid #ddd",
        background: "#2563eb",
        color: "white",
        cursor: "pointer",
      }}
    >
      {children}
    </button>
  );
}