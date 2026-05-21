import React from "react";
import { useForm } from "react-hook-form";
import { useNavigate } from "react-router-dom";
import { v4 as uuidv4 } from "uuid";
import Button from "../components/ui/Button";
import Input from "../components/ui/Input";
import { createTransfer } from "../api/transactionApi";
import { getAccountSummary } from "../api/accountApi";
import { useQuery } from "@tanstack/react-query";

export default function TransferPage() {
  const navigate = useNavigate();
  const { register, handleSubmit, reset, formState: { errors, isSubmitting } } = useForm();

  // Load user's own accounts to populate "From" dropdown
  const { data: summary } = useQuery({
    queryKey: ["account-summary"],
    queryFn: getAccountSummary,
  });
  const accounts = summary?.accounts ?? [];

  const onSubmit = async (values) => {
    try {
      await createTransfer({
        fromAccountNumber: values.fromAccount,
        toAccountNumber:   values.toAccount,
        amount:            parseFloat(values.amount),
        description:       values.note || "",
        idempotencyKey:    uuidv4(),   // unique per submission
      });
      alert("Transfer successful!");
      reset();
      navigate("/dashboard");
    } catch (e) {
      alert(e?.response?.data?.message || "Transfer failed.");
    }
  };

  return (
    <div className="container mt-4" style={{ maxWidth: 520 }}>
      <h2 className="mb-3">New Transfer</h2>
      <form onSubmit={handleSubmit(onSubmit)}>

        {/* From account dropdown */}
        <div className="mb-3">
          <label className="form-label">From Account</label>
          <select className="form-select" {...register("fromAccount", { required: true })}>
            <option value="">Select your account...</option>
            {accounts.map((acc) => (
              <option key={acc.accountId} value={acc.accountNumber}>
                {acc.accountType} — {acc.accountNumber} ({acc.accountCurrency})
              </option>
            ))}
          </select>
          {errors.fromAccount && <small className="text-danger">Required</small>}
        </div>

        <div className="mb-3">
          <Input placeholder="Recipient account number"
            {...register("toAccount", { required: true })} />
          {errors.toAccount && <small className="text-danger">Required</small>}
        </div>

        <div className="mb-3">
          <Input placeholder="Amount" type="number" step="0.01" min="0.01"
            {...register("amount", { required: true, min: 0.01 })} />
          {errors.amount && <small className="text-danger">Enter a valid amount</small>}
        </div>

        <div className="mb-3">
          <Input placeholder="Description (optional)" {...register("note")} />
        </div>

        <Button type="submit" className="w-100" disabled={isSubmitting}>
          {isSubmitting ? "Sending..." : "Send Transfer"}
        </Button>
      </form>
    </div>
  );
}