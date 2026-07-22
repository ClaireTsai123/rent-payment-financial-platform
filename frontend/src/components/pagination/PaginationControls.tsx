import { ChevronLeft, ChevronRight } from "lucide-react";
import type { PageResponse } from "../../api/types";

type PaginationControlsProps = {
  page: PageResponse<unknown>;
  isFetching?: boolean;
  label: string;
  onPrevious: () => void;
  onNext: () => void;
};

export function PaginationControls({ page, isFetching = false, label, onPrevious, onNext }: PaginationControlsProps) {
  const firstItem = page.totalElements === 0 ? 0 : page.number * page.size + 1;
  const lastItem = Math.min((page.number + 1) * page.size, page.totalElements);

  return (
    <div className="pagination-controls" aria-label={label}>
      <span aria-live="polite">
        {firstItem}-{lastItem} of {page.totalElements}
        {isFetching ? " refreshing" : ""}
      </span>
      <div className="pagination-buttons">
        <button type="button" className="icon-button" onClick={onPrevious} disabled={page.first || isFetching} aria-label={`${label} previous page`}>
          <ChevronLeft aria-hidden="true" size={18} />
        </button>
        <button type="button" className="icon-button" onClick={onNext} disabled={page.last || isFetching} aria-label={`${label} next page`}>
          <ChevronRight aria-hidden="true" size={18} />
        </button>
      </div>
    </div>
  );
}
