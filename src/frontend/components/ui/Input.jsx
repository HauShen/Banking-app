import React from "react";

const Input = React.forwardRef(function Input(props, ref) {
  return (<input ref={ref} {...props} className={`form-control ${props.className ?? ""}`} />);
  });

export default Input;