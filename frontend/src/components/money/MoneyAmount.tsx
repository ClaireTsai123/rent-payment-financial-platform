import { formatMoney } from "../../utils/formatters";

export function MoneyAmount({ amount, currency = "USD" }: { amount: number; currency?: string }) {
  return <span className="money-amount">{formatMoney(amount, currency)}</span>;
}
