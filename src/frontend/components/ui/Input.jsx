import React from "react";

const Input = React.forwardRef(function Input(props, ref) {
  return (
    <input
      ref={ref}
      {...props}
      style={{
        width: "100%",
        padding: "10px 12px",
        border: "1px solid #ddd",
        borderRadius: 8,
      }}
    />
  );
});

export default Input;