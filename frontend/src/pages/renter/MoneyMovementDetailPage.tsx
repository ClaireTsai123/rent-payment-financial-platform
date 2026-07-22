import { useQuery } from "@tanstack/react-query";
import { Link, useParams } from "react-router-dom";
import { ArrowLeft } from "lucide-react";
import { getMoneyMovement } from "../../api/renterPortal";
import { useAuth } from "../../auth/authContext";
import { ErrorNotice } from "../../components/feedback/ErrorNotice";
import { MoneyAmount } from "../../components/money/MoneyAmount";
import { StatusPill } from "../../components/status/StatusPill";
import { formatDateTime, humanizeEnum } from "../../utils/formatters";

export function MoneyMovementDetailPage() {
  const { moneyMovementId = "" } = useParams();
  const { token } = useAuth();

  const movementQuery = useQuery({
    queryKey: ["money-movement", moneyMovementId],
    queryFn: () => getMoneyMovement(token ?? "", moneyMovementId),
    enabled: Boolean(token && moneyMovementId)
  });

  const movement = movementQuery.data;

  return (
    <div className="page-stack">
      <Link className="back-link" to="/">
        <ArrowLeft aria-hidden="true" size={16} />
        Dashboard
      </Link>

      {movementQuery.error ? <ErrorNotice message={String(movementQuery.error)} /> : null}

      <header className="detail-header">
        <div>
          <span className="eyebrow">Money movement</span>
          <h1>{movement ? humanizeEnum(movement.type) : "Loading movement"}</h1>
        </div>
        {movement ? <StatusPill value={movement.state} /> : null}
      </header>

      {movement ? (
        <section className="detail-grid">
          <div className="detail-item">
            <span>Amount</span>
            <strong><MoneyAmount amount={movement.amount} currency={movement.currency} /></strong>
          </div>
          <div className="detail-item">
            <span>State</span>
            <strong>{humanizeEnum(movement.state)}</strong>
          </div>
          <div className="detail-item">
            <span>Currency</span>
            <strong>{movement.currency}</strong>
          </div>
          <div className="detail-item wide">
            <span>Operation key</span>
            <strong>{movement.operationKey}</strong>
          </div>
          <div className="detail-item wide">
            <span>Payment plan ID</span>
            <strong>{movement.paymentPlanId}</strong>
          </div>
          <div className="detail-item">
            <span>Created</span>
            <strong>{formatDateTime(movement.createdAt)}</strong>
          </div>
          <div className="detail-item">
            <span>Updated</span>
            <strong>{formatDateTime(movement.updatedAt)}</strong>
          </div>
        </section>
      ) : null}
    </div>
  );
}
