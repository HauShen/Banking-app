import { useQuery } from "@tanstack/react-query";
import { getTransactions } from "../api/transactionApi";

export default function TransactionsPage() {
  const { data, isLoading } = useQuery({
    queryKey: ["transactions"],
    queryFn: () => getTransactions(),
  });

  if (isLoading) return <p>Loading transactions...</p>;

  return (
    <div>
      <h1>Transactions</h1>
      <ul>
        {(data?.items || []).map((tx) => (
          <li key={tx.id}>
            {tx.type} - RM {tx.amount} - {tx.description}
          </li>
        ))}
      </ul>
    </div>
  );
}