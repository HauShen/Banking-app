export default function Input(props) {
  return (
    <input
      {...props}
      style={{
        width: "100%",
        padding: "10px 12px",
        border: "1px solid #ddd",
        borderRadius: 8,
      }}
    />
  );
}