import { ArrowLeft, RefreshCw } from "lucide-react";
import { Link, useLocation, useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { getOperationsResource } from "../../api/operationsPortal";
import { getErrorMessage } from "../../api/errorMessage";
import { useAuth } from "../../auth/authContext";
import { ErrorNotice } from "../../components/feedback/ErrorNotice";
import { StatusPill } from "../../components/status/StatusPill";
import { readValue, renderOperationsValue } from "./operationsFormat";
import { getOperationsResourceConfig } from "./operationsResources";

export function OperationsDetailPage() {
  const { resourceKey, id } = useParams();
  const location = useLocation();
  const config = getOperationsResourceConfig(resourceKey);
  const { token } = useAuth();
  const query = useQuery({
    queryKey: ["ops", config.key, id],
    queryFn: () => getOperationsResource(token!, config.key, id!),
    enabled: Boolean(token && id)
  });
  const record = query.data;
  const statusValue = config.statusField && record ? readValue(record, config.statusField) : null;
  const relatedLinks = record ? buildRelatedLinks(config.relatedLinks ?? [], record as Record<string, unknown>) : [];

  return (
    <div className="page-stack">
      <Link className="back-link" to={`/ops/${config.key}${location.search}`}>
        <ArrowLeft aria-hidden="true" size={16} />
        Back
      </Link>
      <header className="detail-header">
        <div>
          <span className="eyebrow">Operations detail</span>
          <h1>{config.detailLabel}</h1>
        </div>
        <div className="header-actions">
          {statusValue ? <StatusPill value={String(statusValue)} /> : null}
          <button className="secondary-button" type="button" onClick={() => void query.refetch()}>
            <RefreshCw aria-hidden="true" size={16} />
            Refresh
          </button>
        </div>
      </header>

      {query.error ? <ErrorNotice message={getErrorMessage(query.error)} /> : null}
      {query.isLoading ? <span className="loading-label">Loading detail...</span> : null}
      {query.isFetching && !query.isLoading ? <span className="loading-label">Refreshing...</span> : null}

      {record ? (
        <>
          <section className="detail-grid ops-detail-grid">
            {config.detailFields.map((field) => (
              <div className={field.kind === "multiline" ? "detail-item wide" : "detail-item"} key={field.key}>
                <span>{field.label}</span>
                <strong>{renderOperationsValue(readValue(record, field.key), field.kind, String(readValue(record, "currency") ?? "USD"))}</strong>
              </div>
            ))}
          </section>

          {relatedLinks.length > 0 ? (
            <section className="content-band">
              <h2>Related records</h2>
              <div className="related-link-grid">
                {relatedLinks.map((link) => (
                  <Link className="related-link" to={link.to} key={`${link.label}-${link.to}`}>
                    <span>{link.label}</span>
                    <strong>{link.value}</strong>
                  </Link>
                ))}
              </div>
            </section>
          ) : null}

          {"stateHistory" in record && Array.isArray(record.stateHistory) ? (
            <section className="content-band">
              <h2>State history</h2>
              <div className="ops-table history-table" role="table" aria-label="Money movement state history">
                <div className="ops-row ops-head" role="row">
                  <span>From</span>
                  <span>To</span>
                  <span>Reason</span>
                  <span>Changed</span>
                </div>
                {record.stateHistory.map((item) => (
                  <div className="ops-row" role="row" key={item.id}>
                    <span>{item.fromState ? <StatusPill value={item.fromState} /> : "..."}</span>
                    <span><StatusPill value={item.toState} /></span>
                    <span>{item.reason}</span>
                    <span>{renderOperationsValue(item.changedAt, "date")}</span>
                  </div>
                ))}
              </div>
            </section>
          ) : null}
        </>
      ) : null}
    </div>
  );
}

function buildRelatedLinks(
  relatedLinks: NonNullable<ReturnType<typeof getOperationsResourceConfig>["relatedLinks"]>,
  record: Record<string, unknown>
) {
  return relatedLinks.flatMap((link) => {
    if (link.when && !link.when(record)) {
      return [];
    }
    const value = record[link.field];
    if (value === null || value === undefined || String(value).trim() === "") {
      return [];
    }
    const stringValue = String(value);
    const to = link.detail
      ? `/ops/${link.resourceKey}/${encodeURIComponent(stringValue)}`
      : `/ops/${link.resourceKey}?${new URLSearchParams({ [link.filter ?? link.field]: stringValue }).toString()}`;
    return [{ label: link.label, to, value: stringValue }];
  });
}
