import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link, useParams } from "react-router-dom";
import { ArrowLeft, ArrowRight, PlayCircle } from "lucide-react";
import { createRenterCollection, getPaymentPlan, listMoneyMovements } from "../../api/renterPortal";
import { getErrorMessage } from "../../api/errorMessage";
import { useAuth } from "../../auth/authContext";
import { EmptyState } from "../../components/feedback/EmptyState";
import { ErrorNotice } from "../../components/feedback/ErrorNotice";
import { SuccessNotice } from "../../components/feedback/SuccessNotice";
import { MoneyAmount } from "../../components/money/MoneyAmount";
import { StatusPill } from "../../components/status/StatusPill";
import { formatDate, formatDateTime, humanizeEnum } from "../../utils/formatters";
import { isCollectionEligiblePaymentPlan } from "./paymentPlanEligibility";

export function PaymentPlanDetailPage() {
  const { paymentPlanId = "" } = useParams();
  const { token } = useAuth();
  const queryClient = useQueryClient();
  const [collectionResultId, setCollectionResultId] = useState<string | null>(null);

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

  const collectionMutation = useMutation({
    mutationFn: async () => {
      if (!plan) {
        throw new Error("Payment plan is still loading.");
      }
      const operationKey = `portal-collection-${plan.id}-${Date.now()}`;
      return createRenterCollection(token ?? "", operationKey, {
        paymentPlanId: plan.id,
        operationKey,
        currency: "USD"
      });
    },
    onSuccess: async (response) => {
      setCollectionResultId(response.moneyMovementId);
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["payment-plan", paymentPlanId], exact: true }),
        queryClient.invalidateQueries({ queryKey: ["money-movements", paymentPlanId], exact: true }),
        queryClient.invalidateQueries({ queryKey: ["money-movements"], exact: true })
      ]);
    }
  });

  const error = planQuery.error ?? movementsQuery.error ?? collectionMutation.error;

  return (
    <div className="page-stack">
      <Link className="back-link" to="/">
        <ArrowLeft aria-hidden="true" size={16} />
        Dashboard
      </Link>

      {error ? <ErrorNotice message={getErrorMessage(error)} /> : null}
      {collectionResultId ? (
        <SuccessNotice>
          Collection started. <Link className="inline-link" to={`/money-movements/${collectionResultId}`}>View movement</Link>
        </SuccessNotice>
      ) : null}

      <header className="detail-header">
        <div>
          <span className="eyebrow">Payment plan</span>
          <h1>{plan?.billingObligationId ?? "Loading plan"}</h1>
        </div>
        {plan ? (
          <div className="header-actions">
            <StatusPill value={plan.status} />
            <button
              type="button"
              className="primary-button"
              onClick={() => collectionMutation.mutate()}
              disabled={collectionMutation.isPending || !isCollectionEligiblePaymentPlan(plan)}
            >
              <PlayCircle aria-hidden="true" size={18} />
              {collectionMutation.isPending ? "Starting" : "Start collection"}
            </button>
          </div>
        ) : null}
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
