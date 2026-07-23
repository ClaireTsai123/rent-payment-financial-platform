import { RefreshCw } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { Link, useLocation, useParams, useSearchParams } from "react-router-dom";
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
  const location = useLocation();
  const [searchParams, setSearchParams] = useSearchParams();
  const config = getOperationsResourceConfig(resourceKey);
  const { token } = useAuth();
  const page = parsePage(searchParams.get("page"));
  const appliedFilters = useMemo(
    () => readFiltersFromSearch(searchParams, config.filters.map((filter) => filter.name)),
    [config.filters, searchParams]
  );
  const [draftFilters, setDraftFilters] = useState<Record<string, string>>(appliedFilters);

  useEffect(() => {
    setDraftFilters(appliedFilters);
  }, [appliedFilters]);

  const query = useQuery({
    queryKey: ["ops", config.key, page, appliedFilters],
    queryFn: () => listOperationsResource(token!, config.key, appliedFilters, page),
    enabled: Boolean(token)
  });

  const activeFilterCount = useMemo(
    () => Object.values(appliedFilters).filter((value) => value.trim()).length,
    [appliedFilters]
  );
  const activeFilters = activeFilterCount > 0;

  function replaceSearch(nextFilters: Record<string, string>, nextPage = 0) {
    const nextParams = new URLSearchParams();
    if (nextPage > 0) {
      nextParams.set("page", String(nextPage));
    }
    config.filters.forEach((filter) => {
      const value = nextFilters[filter.name]?.trim();
      if (value) {
        nextParams.set(filter.name, value);
      }
    });
    setSearchParams(nextParams);
  }

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
          replaceSearch(draftFilters);
        }}
        onReset={() => {
          setDraftFilters({});
          setSearchParams(new URLSearchParams());
        }}
        activeFilterCount={activeFilterCount}
      />

      {query.error ? <ErrorNotice message={getErrorMessage(query.error)} /> : null}
      {query.isLoading ? <span className="loading-label">Loading {config.label.toLowerCase()}...</span> : null}
      {query.isFetching && !query.isLoading ? <span className="loading-label">Refreshing...</span> : null}

      {query.data && query.data.empty ? (
        <EmptyState
          title={activeFilters ? "No matching records" : `No ${config.label.toLowerCase()}`}
          detail={
            activeFilters
              ? "This shared view loaded correctly, but no records match the current exact-search or filter criteria."
              : undefined
          }
        />
      ) : null}

      {query.data && !query.data.empty ? (
        <section className="content-band">
          <div className="ops-table" role="table" aria-label={config.label}>
            <div className="ops-row ops-head" role="row">
              {config.columns.map((column) => <span key={column.key}>{column.label}</span>)}
              <span>Open</span>
            </div>
            {query.data.content.map((row) => (
              <Link
                className="ops-row table-link"
                role="row"
                key={String(readValue(row, "id"))}
                to={`/ops/${config.key}/${readValue(row, "id")}${location.search}`}
              >
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
            onPrevious={() => replaceSearch(appliedFilters, Math.max(0, page - 1))}
            onNext={() => replaceSearch(appliedFilters, page + 1)}
          />
        </section>
      ) : null}
    </div>
  );
}

function parsePage(value: string | null) {
  const page = Number(value);
  return Number.isInteger(page) && page > 0 ? page : 0;
}

function readFiltersFromSearch(searchParams: URLSearchParams, allowedNames: string[]) {
  return allowedNames.reduce<Record<string, string>>((filters, name) => {
    const value = searchParams.get(name);
    if (value) {
      filters[name] = value;
    }
    return filters;
  }, {});
}
