import { useQuery } from "@tanstack/react-query";
import { Link, useParams } from "react-router-dom";
import { ArrowLeft, ArrowRight } from "lucide-react";
import { getPaymentPlan, listMoneyMovements } from "../../api/renterPortal";
import { useAuth } from "../../auth/authContext";
import { EmptyState } from "../../components/feedback/EmptyState";
import { ErrorNotice } from "../../components/feedback/ErrorNotice";
import { MoneyAmount } from "../../components/money/MoneyAmount";
import { StatusPill } from "../../components/status/StatusPill";
import { formatDate, formatDateTime, humanizeEnum } from "../../utils/formatters";

export function PaymentPlanDetailPage() {
  const { paymentPlanId = "" } = useParams();
  const { token } = useAuth();

  const planQuery = useQuery({
    queryKey: ["payment-plan", paymentPlanId],
    queryFn: () => getPaymentPlan(token ?? "", paymentPlanId),
    enabled: Boolean(token && paymentPlanId)
  });

  const movementsQuery = useQuery({
    queryKey: ["money-movements", paymentPlanId],
    queryFn: () => listMoneyMovements(token ?? "", paymentPlanId),
    enabled: Boolean(token && paymentPlanId)
  });

  const plan = planQuery.data;

  return (
    <div className="page-stack">
      <Link className="back-link" to="/">
        <ArrowLeft aria-hidden="true" size={16} />
        Dashboard
      </Link>

      {planQuery.error ? <ErrorNotice message={String(planQuery.error)} /> : null}

      <header className="detail-header">
        <div>
          <span className="eyebrow">Payment plan</span>
          <h1>{plan?.billingObligationId ?? "Loading plan"}</h1>
        </div>
        {plan ? <StatusPill value={plan.status} /> : null}
      </header>

      {plan ? (
        <section className="detail-grid">
          <div className="detail-item">
            <span>Rent amount</span>
            <strong><MoneyAmount amount={plan.rentAmount} /></strong>
          </div>
          <div className="detail-item">
            <span>Initial collection</span>
            <strong><MoneyAmount amount={plan.initialCollectionAmount} /></strong>
          </div>
          <div className="detail-item">
            <span>Repayment amount</span>
            <strong><MoneyAmount amount={plan.repaymentAmount} /></strong>
          </div>
          <div className="detail-item">
            <span>Rent due</span>
            <strong>{formatDate(plan.rentDueDate)}</strong>
          </div>
          <div className="detail-item">
            <span>Repayment due</span>
            <strong>{formatDate(plan.repaymentDueDate)}</strong>
          </div>
          <div className="detail-item">
            <span>Updated</span>
            <strong>{formatDateTime(plan.updatedAt)}</strong>
          </div>
        </section>
      ) : null}

      <section className="content-band">
        <div className="section-heading">
          <h2>Plan activity</h2>
        </div>

        {movementsQuery.data?.empty ? <EmptyState title="No movements for this plan" /> : null}
        {movementsQuery.data?.content.length ? (
          <div className="data-table" role="table" aria-label="Plan money movements">
            <div className="table-row table-head" role="row">
              <span>Type</span>
              <span>Amount</span>
              <span>Created</span>
              <span>State</span>
              <span />
            </div>
            {movementsQuery.data.content.map((movement) => (
              <Link className="table-row table-link" role="row" to={`/money-movements/${movement.id}`} key={movement.id}>
                <span>{humanizeEnum(movement.type)}</span>
                <span><MoneyAmount amount={movement.amount} currency={movement.currency} /></span>
                <span>{formatDateTime(movement.createdAt)}</span>
                <span><StatusPill value={movement.state} /></span>
                <span className="row-action"><ArrowRight aria-hidden="true" size={16} /></span>
              </Link>
            ))}
          </div>
        ) : null}
      </section>
    </div>
  );
}
