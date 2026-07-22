import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { ArrowRight, PlayCircle, RefreshCw } from "lucide-react";
import { createRenterCollection, listMoneyMovements, listPaymentPlans } from "../../api/renterPortal";
import { getErrorMessage } from "../../api/errorMessage";
import { useAuth } from "../../auth/authContext";
import { EmptyState } from "../../components/feedback/EmptyState";
import { ErrorNotice } from "../../components/feedback/ErrorNotice";
import { SuccessNotice } from "../../components/feedback/SuccessNotice";
import { MoneyAmount } from "../../components/money/MoneyAmount";
import { PaginationControls } from "../../components/pagination/PaginationControls";
import { StatusPill } from "../../components/status/StatusPill";
import { formatDate, formatDateTime, humanizeEnum } from "../../utils/formatters";
import { isCollectionEligiblePaymentPlan } from "./paymentPlanEligibility";

export function RenterDashboardPage() {
  const auth = useAuth();
  const token = auth.token ?? "";
  const queryClient = useQueryClient();
  const [collectionResultId, setCollectionResultId] = useState<string | null>(null);
  const [paymentPlansPage, setPaymentPlansPage] = useState(0);
  const [movementsPage, setMovementsPage] = useState(0);

  const paymentPlansQuery = useQuery({
    queryKey: ["payment-plans", paymentPlansPage],
    queryFn: () => listPaymentPlans(token, paymentPlansPage),
    enabled: Boolean(token),
    placeholderData: (previousData) => previousData
  });

  const movementsQuery = useQuery({
    queryKey: ["money-movements", movementsPage],
    queryFn: () => listMoneyMovements(token, undefined, movementsPage),
    enabled: Boolean(token),
    placeholderData: (previousData) => previousData
  });

  const openPlans = useMemo(
    () => paymentPlansQuery.data?.content.filter(isCollectionEligiblePaymentPlan) ?? [],
    [paymentPlansQuery.data]
  );
  const primaryPlan = useMemo(
    () =>
      paymentPlansQuery.data?.content.find(isCollectionEligiblePaymentPlan),
    [paymentPlansQuery.data]
  );
  const pendingMovements = useMemo(
    () =>
      movementsQuery.data?.content.filter((movement) =>
        movement.state === "CREATED" || movement.state === "SUBMITTED" || movement.state === "PROCESSING"
      ) ?? [],
    [movementsQuery.data]
  );

  const collectionMutation = useMutation({
    mutationFn: async () => {
      if (!primaryPlan) {
        throw new Error("No active payment plan is available.");
      }
      const operationKey = `portal-collection-${primaryPlan.id}-${Date.now()}`;
      return createRenterCollection(token, operationKey, {
        paymentPlanId: primaryPlan.id,
        operationKey,
        currency: "USD"
      });
    },
    onSuccess: async (response) => {
      setCollectionResultId(response.moneyMovementId);
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["payment-plans"] }),
        queryClient.invalidateQueries({ queryKey: ["money-movements"] })
      ]);
    }
  });

  const error = paymentPlansQuery.error ?? movementsQuery.error ?? collectionMutation.error;

  return (
    <div className="page-stack">
      <header className="page-header">
        <div>
          <span className="eyebrow">Renter dashboard</span>
          <h1>Payment plan overview</h1>
        </div>
        <button
          type="button"
          className="secondary-button"
          onClick={() => {
            setCollectionResultId(null);
            void paymentPlansQuery.refetch();
            void movementsQuery.refetch();
          }}
        >
          <RefreshCw aria-hidden="true" size={17} />
          Refresh
        </button>
      </header>

      {error ? <ErrorNotice message={getErrorMessage(error)} /> : null}
      {collectionResultId ? (
        <SuccessNotice>
          Collection started. <Link className="inline-link" to={`/money-movements/${collectionResultId}`}>View movement</Link>
        </SuccessNotice>
      ) : null}

      <section className="metrics-grid" aria-label="Payment summary">
        <div className="metric">
          <span>Open plans</span>
          <strong aria-label="Open plans count">{paymentPlansQuery.data ? openPlans.length : "..."}</strong>
        </div>
        <div className="metric">
          <span>In-flight movements</span>
          <strong>{pendingMovements.length}</strong>
        </div>
        <div className="metric">
          <span>Initial collection</span>
          <strong>{primaryPlan ? <MoneyAmount amount={primaryPlan.initialCollectionAmount} /> : "..."}</strong>
        </div>
      </section>

      <section className="content-band">
        <div className="section-heading">
          <h2>Payment plans</h2>
          {primaryPlan ? (
            <button
              type="button"
              className="primary-button"
              onClick={() => collectionMutation.mutate()}
              disabled={collectionMutation.isPending}
            >
              <PlayCircle aria-hidden="true" size={18} />
              {collectionMutation.isPending ? "Starting" : "Start collection"}
            </button>
          ) : null}
        </div>

        {paymentPlansQuery.isLoading ? <div className="table-skeleton" /> : null}
        {paymentPlansQuery.data?.empty ? (
          <EmptyState title="No payment plans found" detail="The backend has no renter-scoped plans for this dev token." />
        ) : null}
        {paymentPlansQuery.data?.content.length ? (
          <>
            <div className="data-table" role="table" aria-label="Payment plans">
              <div className="table-row table-head" role="row">
                <span>Obligation</span>
                <span>Rent</span>
                <span>Repayment due</span>
                <span>Status</span>
                <span />
              </div>
              {paymentPlansQuery.data.content.map((plan) => (
                <Link className="table-row table-link" role="row" to={`/payment-plans/${plan.id}`} key={plan.id}>
                  <span>{plan.billingObligationId}</span>
                  <span><MoneyAmount amount={plan.rentAmount} /></span>
                  <span>{formatDate(plan.repaymentDueDate)}</span>
                  <span><StatusPill value={plan.status} /></span>
                  <span className="row-action"><ArrowRight aria-hidden="true" size={16} /></span>
                </Link>
              ))}
            </div>
            <PaginationControls
              page={paymentPlansQuery.data}
              isFetching={paymentPlansQuery.isFetching && !paymentPlansQuery.isLoading}
              label="Payment plans"
              onPrevious={() => setPaymentPlansPage((page) => Math.max(0, page - 1))}
              onNext={() => setPaymentPlansPage((page) => page + 1)}
            />
          </>
        ) : null}
      </section>

      <section className="content-band" id="movements">
        <div className="section-heading">
          <h2>Money movement history</h2>
        </div>

        {movementsQuery.isLoading ? <div className="table-skeleton" /> : null}
        {movementsQuery.data?.empty ? <EmptyState title="No money movements yet" /> : null}
        {movementsQuery.data?.content.length ? (
          <>
            <div className="data-table" role="table" aria-label="Money movements">
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
            <PaginationControls
              page={movementsQuery.data}
              isFetching={movementsQuery.isFetching && !movementsQuery.isLoading}
              label="Money movements"
              onPrevious={() => setMovementsPage((page) => Math.max(0, page - 1))}
              onNext={() => setMovementsPage((page) => page + 1)}
            />
          </>
        ) : null}
      </section>
    </div>
  );
}
