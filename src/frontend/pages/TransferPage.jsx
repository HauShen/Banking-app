import { useForm } from "react-hook-form";
import Button from "../components/ui/Button";
import Input from "../components/ui/Input";

export default function TransferPage() {
  const { register, handleSubmit, reset } = useForm({
    defaultValues: {
      toAccount: "",
      amount: "",
      note: "",
    },
  });

  const onSubmit = async (values) => {
    try {
      // TODO: replace with real API call, e.g. await createTransfer(values)
      console.log("Transfer payload:", values);
      alert("Transfer submitted (demo).");
      reset();
    } catch (e) {
      alert("Transfer failed.");
    }
  };

  return (
    <div style={{ maxWidth: 520, margin: "24px auto" }}>
      <h1>Transfer</h1>

      <form onSubmit={handleSubmit(onSubmit)} style={{ display: "grid", gap: 12 }}>
        <Input
          placeholder="Recipient account number"
          {...register("toAccount", { required: true })}
        />

        <Input
          placeholder="Amount (RM)"
          type="number"
          step="0.01"
          min="0"
          {...register("amount", { required: true, min: 0.01 })}
        />

        <Input
          placeholder="Note (optional)"
          {...register("note")}
        />

        <Button type="submit">Send Transfer</Button>
      </form>
    </div>
  );
}