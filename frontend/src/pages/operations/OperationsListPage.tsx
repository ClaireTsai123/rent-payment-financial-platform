import { RefreshCw } from "lucide-react";
import { useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { listOperationsResource } from "../../api/operationsPortal";
import { getErrorMessage } from "../../api/errorMessage";
import { useAuth } from "../../auth/authContext";
import { EmptyState } from "../../components/feedback/EmptyState";
import { ErrorNotice } from "../../components/feedback/ErrorNotice";
import { PaginationControls } from "../../components/pagination/PaginationControls";
import { readValue, renderOperationsValue } from "./operationsFormat";
import { getOperationsResourceConfig } from "./operationsResources";
import { OperationsFilterBar } from "./OperationsFilterBar";

export function OperationsListPage() {
  const { resourceKey } = useParams();
  const config = getOperationsResourceConfig(resourceKey);
  const { token } = useAuth();
  const [page, setPage] = useState(0);
  const [draftFilters, setDraftFilters] = useState<Record<string, string>>({});
  const [appliedFilters, setAppliedFilters] = useState<Record<string, string>>({});

  const query = useQuery({
    queryKey: ["ops", config.key, page, appliedFilters],
    queryFn: () => listOperationsResource(token!, config.key, appliedFilters, page),
    enabled: Boolean(token)
  });

  const activeFilters = useMemo(() => Object.values(appliedFilters).some((value) => value.trim()), [appliedFilters]);

  return (
    <div className="page-stack">
      <header className="page-header">
        <div>
          <span className="eyebrow">Operations</span>
          <h1>{config.label}</h1>
        </div>
        <button className="secondary-button" type="button" onClick={() => void query.refetch()}>
          <RefreshCw aria-hidden="true" size={16} />
          Refresh
        </button>
      </header>

      <OperationsFilterBar
        filters={config.filters}
        values={draftFilters}
        onChange={setDraftFilters}
        onSubmit={() => {
          setPage(0);
          setAppliedFilters(draftFilters);
        }}
        onReset={() => {
          setPage(0);
          setDraftFilters({});
          setAppliedFilters({});
        }}
      />

      {query.error ? <ErrorNotice message={getErrorMessage(query.error)} /> : null}
      {query.isLoading ? <span className="loading-label">Loading {config.label.toLowerCase()}...</span> : null}
      {query.isFetching && !query.isLoading ? <span className="loading-label">Refreshing...</span> : null}

      {query.data && query.data.empty ? (
        <EmptyState title={activeFilters ? "No matching records" : `No ${config.label.toLowerCase()}`} />
      ) : null}

      {query.data && !query.data.empty ? (
        <section className="content-band">
          <div className="ops-table" role="table" aria-label={config.label}>
            <div className="ops-row ops-head" role="row">
              {config.columns.map((column) => <span key={column.key}>{column.label}</span>)}
              <span>Open</span>
            </div>
            {query.data.content.map((row) => (
              <Link className="ops-row table-link" role="row" key={String(readValue(row, "id"))} to={`/ops/${config.key}/${readValue(row, "id")}`}>
                {config.columns.map((column) => (
                  <span key={column.key}>
                    {renderOperationsValue(readValue(row, column.key), column.kind, String(readValue(row, "currency") ?? "USD"))}
                  </span>
                ))}
                <span className="row-action">View</span>
              </Link>
            ))}
          </div>
          <PaginationControls
            page={query.data}
            isFetching={query.isFetching}
            label={`${config.label} pagination`}
            onPrevious={() => setPage((current) => Math.max(0, current - 1))}
            onNext={() => setPage((current) => current + 1)}
          />
        </section>
      ) : null}
    </div>
  );
}
