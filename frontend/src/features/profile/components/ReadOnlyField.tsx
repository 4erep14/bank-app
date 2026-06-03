// Story: US-005

interface ReadOnlyFieldProps {
  /** Unique id for the field — used for aria-labelledby association */
  id: string;
  /** Visible field label */
  label: string;
  /** The display value to render */
  value: string;
  /**
   * When true renders the lock icon and additional "read-only" indicator.
   * Use for fields that are permanently non-editable (email, dateOfBirth).
   * Defaults to true.
   */
  locked?: boolean;
}

/**
 * ReadOnlyField — displays a labelled, non-interactive field value.
 *
 * AC1: renders firstName, lastName, email, phoneNumber, dateOfBirth in view mode.
 * AC3: email and dateOfBirth use locked=true to signal they cannot be edited.
 *
 * Accessibility:
 * - aria-readonly="true" on the value container
 * - aria-labelledby wires label to value
 * - Lock icon is aria-hidden (decorative)
 * - Keyboard users can focus the wrapper via tabIndex when locked
 */
export default function ReadOnlyField({
  id,
  label,
  value,
  locked = true,
}: ReadOnlyFieldProps) {
  const labelId = `${id}-label`;

  return (
    <div className="flex flex-col gap-1">
      {/* Label */}
      <span
        id={labelId}
        className="text-sm font-medium text-gray-700"
      >
        {label}
        {locked && (
          <span
            aria-hidden="true"
            className="ml-1.5 inline-flex items-center align-middle text-gray-400"
            title="This field cannot be edited"
          >
            {/* Lock icon SVG */}
            <svg
              className="h-3.5 w-3.5"
              viewBox="0 0 20 20"
              fill="currentColor"
              aria-hidden="true"
            >
              <path
                fillRule="evenodd"
                d="M10 1a4.5 4.5 0 0 0-4.5 4.5V9H5a2 2 0 0 0-2 2v6a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2v-6a2 2 0 0 0-2-2h-.5V5.5A4.5 4.5 0 0 0 10 1Zm3 8V5.5a3 3 0 1 0-6 0V9h6Z"
                clipRule="evenodd"
              />
            </svg>
          </span>
        )}
      </span>

      {/* Value container */}
      <div
        id={id}
        role="textbox"
        aria-labelledby={labelId}
        aria-readonly="true"
        aria-multiline="false"
        tabIndex={locked ? 0 : -1}
        className={[
          'rounded-md border px-3 py-2 text-sm text-gray-700',
          locked
            ? 'cursor-not-allowed border-gray-200 bg-gray-50 text-gray-500'
            : 'border-gray-200 bg-gray-50',
        ].join(' ')}
      >
        {value || <span className="italic text-gray-400">—</span>}
      </div>

      {locked && (
        <p className="text-xs text-gray-400">
          This field cannot be changed.
        </p>
      )}
    </div>
  );
}
