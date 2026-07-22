import type { FormEvent } from "react";
import type { FilterOption } from "./operationsResources";

type Props = {
  filters: FilterOption[];
  values: Record<string, string>;
  onChange: (values: Record<string, string>) => void;
  onSubmit: () => void;
  onReset: () => void;
};

export function OperationsFilterBar({ filters, values, onChange, onSubmit, onReset }: Props) {
  function update(name: string, value: string) {
    onChange({ ...values, [name]: value });
  }

  function submit(event: FormEvent) {
    event.preventDefault();
    onSubmit();
  }

  return (
    <form className="filter-bar" onSubmit={submit} aria-label="Operations filters">
      {filters.map((filter) => (
        <label key={filter.name} className="filter-field">
          <span>{filter.label}</span>
          {filter.type === "select" ? (
            <select value={values[filter.name] ?? ""} onChange={(event) => update(filter.name, event.target.value)}>
              <option value="">Any</option>
              {filter.options?.map((option) => (
                <option value={option} key={option}>{option}</option>
              ))}
            </select>
          ) : (
            <input
              type={filter.type ?? "text"}
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
