import React from "react";
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
    <div className="container mt-4" style={{ maxWidth: 520 }}>
      <h2 className="mb-3">Transfer</h2>
      <form onSubmit={handleSubmit(onSubmit)}>
        <div className="mb-3"><Input placeholder="Recipient account number" {...register("toAccount", { required: true })} /></div>
        <div className="mb-3"><Input placeholder="Amount (RM)" type="number" step="0.01" min="0" {...register("amount", { required: true, min: 0.01 })} /></div>
        <div className="mb-3"><Input placeholder="Note (optional)" {...register("note")} /></div>
        <Button type="submit" className="w-100">Send Transfer</Button>
      </form>
    </div>
  );
}