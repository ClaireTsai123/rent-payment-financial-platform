import type { FormEvent } from "react";
import type { FilterOption } from "./operationsResources";

type Props = {
  filters: FilterOption[];
  values: Record<string, string>;
  onChange: (values: Record<string, string>) => void;
  onSubmit: () => void;
  onReset: () => void;
  activeFilterCount?: number;
};

export function OperationsFilterBar({ filters, values, onChange, onSubmit, onReset, activeFilterCount = 0 }: Props) {
  function update(name: string, value: string) {
    onChange({ ...values, [name]: value });
  }

  function submit(event: FormEvent) {
    event.preventDefault();
    onSubmit();
  }

  return (
    <form className="filter-bar" onSubmit={submit} aria-label="Operations filters">
      {activeFilterCount > 0 ? (
        <div className="filter-summary">
          <strong>{activeFilterCount} active filter{activeFilterCount === 1 ? "" : "s"}</strong>
          <span>Filters and page are stored in the URL for refreshes and shared links.</span>
        </div>
      ) : null}
      {filters.map((filter) => (
        <label key={filter.name} className="filter-field" htmlFor={`ops-filter-${filter.name}`}>
          <span>
            {filter.label}
            {filter.exact ? <em aria-hidden="true">Exact</em> : null}
          </span>
          {filter.type === "select" ? (
            <select
              id={`ops-filter-${filter.name}`}
              aria-label={filter.label}
              value={values[filter.name] ?? ""}
              onChange={(event) => update(filter.name, event.target.value)}
            >
              <option value="">Any</option>
              {filter.options?.map((option) => (
                <option value={option} key={option}>{option}</option>
              ))}
            </select>
          ) : (
            <input
              id={`ops-filter-${filter.name}`}
              aria-label={filter.label}
              type={filter.type ?? "text"}
              placeholder={filter.exact ? "Exact value" : undefined}
              value={values[filter.name] ?? ""}
              onChange={(event) => update(filter.name, event.target.value)}
            />
          )}
        </label>
      ))}
      <div className="filter-actions">
        <button className="primary-button" type="submit">Apply</button>
        <button className="secondary-button" type="button" onClick={onReset}>Reset</button>
      </div>
    </form>
  );
}
